package hu.blackbelt.matchreplay.catalog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MoveCatalogLoader implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MoveCatalogLoader.class);

    private final AtomicReference<MoveCatalogSnapshot> current =
            new AtomicReference<>(MoveCatalogSnapshot.empty());

    private final Path catalogPath;
    private final ObjectMapper objectMapper;
    private Thread watchThread;

    public MoveCatalogLoader(
            @Value("${bjj.match-replay.catalog-path:config/moves.json}") String catalogPath) {
        this.catalogPath = Paths.get(catalogPath);
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void init() {
        ensureFileExists();
        loadFile();
        startWatcher();
    }

    public MoveCatalogSnapshot getSnapshot() {
        return current.get();
    }

    private void ensureFileExists() {
        if (Files.exists(catalogPath)) return;
        try {
            Path parent = catalogPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (InputStream in = getClass().getClassLoader()
                    .getResourceAsStream("moves-default.json")) {
                if (in != null) {
                    Files.copy(in, catalogPath);
                    log.info("Copied default move catalog to {}", catalogPath);
                }
            }
        } catch (IOException e) {
            log.warn("Could not copy default catalog to {}: {}", catalogPath, e.getMessage());
        }
    }

    private void loadFile() {
        if (!Files.exists(catalogPath)) {
            log.warn("Move catalog file not found at {}; using empty catalog", catalogPath);
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(catalogPath.toFile());
            int version = root.path("version").asInt(0);

            List<CatalogPosition> positions = new ArrayList<>();
            for (JsonNode n : root.path("positions")) {
                positions.add(new CatalogPosition(
                        n.path("key").asText(),
                        n.path("label").asText()));
            }

            List<CatalogMove> moves = new ArrayList<>();
            for (JsonNode n : root.path("moves")) {
                List<String> tags = new ArrayList<>();
                for (JsonNode t : n.path("tags")) tags.add(t.asText());
                moves.add(new CatalogMove(
                        n.path("key").asText(),
                        n.path("label").asText(),
                        n.path("category").asText(null),
                        List.copyOf(tags)));
            }

            List<CatalogTransition> transitions = new ArrayList<>();
            for (JsonNode n : root.path("transitions")) {
                transitions.add(new CatalogTransition(
                        n.path("fromPosition").asText(),
                        n.path("move").asText(),
                        n.path("toPosition").asText(),
                        n.path("difficulty").asInt(1)));
            }

            List<CatalogRecommendedResponse> recommended = new ArrayList<>();
            for (JsonNode n : root.path("recommendedResponses")) {
                List<CatalogResponseEntry> responses = new ArrayList<>();
                for (JsonNode r : n.path("responses")) {
                    responses.add(new CatalogResponseEntry(
                            r.path("move").asText(),
                            r.path("score").asInt(0),
                            r.path("reason").asText(null)));
                }
                recommended.add(new CatalogRecommendedResponse(
                        n.path("whenOpponentPlays").asText(),
                        n.path("fromPosition").asText(),
                        List.copyOf(responses)));
            }

            current.set(new MoveCatalogSnapshot(
                    version,
                    List.copyOf(positions),
                    List.copyOf(moves),
                    List.copyOf(transitions),
                    List.copyOf(recommended)));

            log.info("Loaded move catalog v{}: {} moves, {} transitions from {}",
                    version, moves.size(), transitions.size(), catalogPath);
        } catch (IOException e) {
            log.warn("Failed to load move catalog from {}: {}", catalogPath, e.getMessage());
        }
    }

    private void startWatcher() {
        Path dir = catalogPath.toAbsolutePath().getParent();
        if (dir == null || !Files.isDirectory(dir)) return;
        String fileName = catalogPath.getFileName().toString();

        watchThread = new Thread(() -> {
            try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                dir.register(watcher,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key;
                    try {
                        key = watcher.poll(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (key == null) continue;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context() instanceof Path changed
                                && fileName.equals(changed.toString())) {
                            loadFile();
                        }
                    }
                    if (!key.reset()) break;
                }
            } catch (IOException e) {
                log.warn("Cannot watch catalog directory {}: {}", dir, e.getMessage());
            }
        }, "move-catalog-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    @Override
    public void destroy() {
        if (watchThread != null) watchThread.interrupt();
    }
}
