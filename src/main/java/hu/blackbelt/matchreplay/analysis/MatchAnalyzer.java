package hu.blackbelt.matchreplay.analysis;

import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchEvent;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchAnalyzer {

    public MatchAnalysis analyze(Match match) {
        List<MatchEvent> events = match.getEvents();
        return new MatchAnalysis(
                computeFrequencies(events, "A"),
                computeFrequencies(events, "B"),
                computePatterns(events),
                computeFatigueSeries(events),
                computePositionTimes(events));
    }

    private List<MoveFrequency> computeFrequencies(List<MatchEvent> events, String actor) {
        List<MatchEvent> actorEvents = events.stream()
                .filter(e -> actor.equals(e.getActor()))
                .toList();
        int total = actorEvents.size();
        if (total == 0) return List.of();
        return actorEvents.stream()
                .collect(Collectors.groupingBy(MatchEvent::getMoveKey, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new MoveFrequency(actor, e.getKey(), e.getValue().intValue(),
                        (double) e.getValue() / total))
                .sorted(Comparator.comparingInt(MoveFrequency::count).reversed())
                .toList();
    }

    private List<PatternOccurrence> computePatterns(List<MatchEvent> events) {
        List<PatternOccurrence> result = new ArrayList<>();
        for (String actor : List.of("A", "B")) {
            List<String> moves = events.stream()
                    .filter(e -> actor.equals(e.getActor()))
                    .map(MatchEvent::getMoveKey)
                    .toList();
            for (int n = 2; n <= 3; n++) {
                result.addAll(computeNgrams(actor, moves, n));
            }
        }
        result.sort(Comparator.comparingInt(PatternOccurrence::occurrences).reversed()
                .thenComparingInt(p -> -p.sequence().size()));
        return result;
    }

    private List<PatternOccurrence> computeNgrams(String actor, List<String> moves, int n) {
        if (moves.size() < n) return List.of();
        Map<List<String>, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i <= moves.size() - n; i++) {
            List<String> window = new ArrayList<>(moves.subList(i, i + n));
            counts.merge(window, 1, (a, b) -> a + b);
        }
        return counts.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .map(e -> new PatternOccurrence(actor, e.getKey(), e.getValue()))
                .toList();
    }

    private List<FatiguePoint> computeFatigueSeries(List<MatchEvent> events) {
        return events.stream()
                .filter(e -> e.getFatigueA() != null || e.getFatigueB() != null)
                .map(e -> new FatiguePoint(e.getStepIndex(), e.getTimestampSeconds(),
                        e.getFatigueA(), e.getFatigueB()))
                .toList();
    }

    private List<PositionTime> computePositionTimes(List<MatchEvent> events) {
        return events.stream()
                .filter(e -> e.getPositionAfter() != null && !e.getPositionAfter().isBlank())
                .collect(Collectors.groupingBy(MatchEvent::getPositionAfter, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PositionTime(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparingInt(PositionTime::steps).reversed())
                .toList();
    }
}
