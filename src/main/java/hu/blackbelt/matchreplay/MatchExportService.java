package hu.blackbelt.matchreplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MatchExportService {

    private final ObjectMapper objectMapper;

    public MatchExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String export(Match match) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("exportVersion", 1);
            root.put("exportedAt", Instant.now().toString());
            root.put("matchDate", match.getMatchDate().toString());
            if (match.getOpponentName() != null) root.put("opponentName", match.getOpponentName());
            if (match.getSelfRole() != null) root.put("selfRole", match.getSelfRole());
            if (match.getDurationSeconds() != null) root.put("durationSeconds", match.getDurationSeconds());
            if (match.getResultOutcome() != null) root.put("resultOutcome", match.getResultOutcome().name());

            ArrayNode events = root.putArray("events");
            for (MatchEvent evt : match.getEvents()) {
                ObjectNode evtNode = events.addObject();
                evtNode.put("stepIndex", evt.getStepIndex());
                evtNode.put("timestampSeconds", evt.getTimestampSeconds());
                evtNode.put("actor", evt.getActor());
                evtNode.put("moveKey", evt.getMoveKey());
                if (evt.getPositionAfter() != null) evtNode.put("positionAfter", evt.getPositionAfter());
                if (evt.getFatigueA() != null) evtNode.put("fatigueA", evt.getFatigueA());
                if (evt.getFatigueB() != null) evtNode.put("fatigueB", evt.getFatigueB());
                if (evt.getNote() != null) evtNode.put("note", evt.getNote());
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export match to JSON", e);
        }
    }
}
