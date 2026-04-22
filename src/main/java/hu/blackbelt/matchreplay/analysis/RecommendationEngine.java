package hu.blackbelt.matchreplay.analysis;

import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchEvent;
import hu.blackbelt.matchreplay.catalog.CatalogRecommendedResponse;
import hu.blackbelt.matchreplay.catalog.CatalogResponseEntry;
import hu.blackbelt.matchreplay.catalog.MoveCatalogLoader;
import hu.blackbelt.matchreplay.catalog.MoveCatalogSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class RecommendationEngine {

    private final MoveCatalogLoader catalogLoader;

    public RecommendationEngine(MoveCatalogLoader catalogLoader) {
        this.catalogLoader = catalogLoader;
    }

    /**
     * Evaluates each user move against catalog recommendations and returns findings
     * for all moves where a catalog opinion exists and a score gap >= 1 is detected.
     * Severity: HIGH (gap >= 5), MEDIUM (gap 3-4), LOW (gap 1-2).
     */
    public List<MistakeFinding> analyze(Match match) {
        String selfRole = match.getSelfRole();
        if (selfRole == null) return List.of();

        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();
        List<MatchEvent> events = match.getEvents();
        List<MistakeFinding> findings = new ArrayList<>();

        for (int i = 1; i < events.size(); i++) {
            MatchEvent current = events.get(i);
            MatchEvent prev = events.get(i - 1);

            if (!selfRole.equals(current.getActor())) continue;
            if (selfRole.equals(prev.getActor())) continue; // previous must be opponent

            CatalogRecommendedResponse rec = findRecommendation(catalog, prev.getMoveKey(), prev.getPositionAfter());
            if (rec == null) continue;

            List<CatalogResponseEntry> responses = rec.responses();
            if (responses.isEmpty()) continue;

            CatalogResponseEntry top = responses.stream()
                    .max(Comparator.comparingInt(CatalogResponseEntry::score))
                    .orElseThrow();

            int chosenScore = responses.stream()
                    .filter(r -> Objects.equals(r.move(), current.getMoveKey()))
                    .mapToInt(CatalogResponseEntry::score)
                    .findFirst()
                    .orElse(0);

            int gap = top.score() - chosenScore;
            if (gap <= 0) continue;

            MistakeSeverity severity = gap >= 5 ? MistakeSeverity.HIGH
                    : gap >= 3 ? MistakeSeverity.MEDIUM
                    : MistakeSeverity.LOW;

            String userMoveLabel = catalog.findMove(current.getMoveKey())
                    .map(m -> m.label()).orElse(current.getMoveKey());
            String opponentMoveLabel = catalog.findMove(prev.getMoveKey())
                    .map(m -> m.label()).orElse(prev.getMoveKey());
            String topMoveLabel = catalog.findMove(top.move())
                    .map(m -> m.label()).orElse(top.move());
            String fromPositionLabel = catalog.positions().stream()
                    .filter(p -> p.key().equals(prev.getPositionAfter()))
                    .map(p -> p.label())
                    .findFirst()
                    .orElse(prev.getPositionAfter() != null ? prev.getPositionAfter() : "");

            findings.add(new MistakeFinding(
                    current.getStepIndex(),
                    current.getMoveKey(),
                    userMoveLabel,
                    prev.getMoveKey(),
                    opponentMoveLabel,
                    prev.getPositionAfter(),
                    fromPositionLabel,
                    chosenScore,
                    top.move(),
                    topMoveLabel,
                    top.score(),
                    top.reason(),
                    gap,
                    severity));
        }

        return findings;
    }

    private CatalogRecommendedResponse findRecommendation(MoveCatalogSnapshot catalog,
                                                          String opponentMoveKey,
                                                          String fromPosition) {
        if (opponentMoveKey == null) return null;
        return catalog.recommendedResponses().stream()
                .filter(r -> r.whenOpponentPlays().equals(opponentMoveKey)
                        && Objects.equals(r.fromPosition(), fromPosition))
                .findFirst()
                .orElse(null);
    }
}
