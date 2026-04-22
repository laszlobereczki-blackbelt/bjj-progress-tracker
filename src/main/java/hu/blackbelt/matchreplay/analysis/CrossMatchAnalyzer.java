package hu.blackbelt.matchreplay.analysis;

import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchEvent;
import hu.blackbelt.matchreplay.MatchService;
import hu.blackbelt.matchreplay.catalog.MoveCatalogLoader;
import hu.blackbelt.matchreplay.catalog.MoveCatalogSnapshot;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CrossMatchAnalyzer {

    private final MatchService matchService;
    private final RecommendationEngine recommendationEngine;
    private final MoveCatalogLoader catalogLoader;

    public CrossMatchAnalyzer(MatchService matchService, RecommendationEngine recommendationEngine,
                               MoveCatalogLoader catalogLoader) {
        this.matchService = matchService;
        this.recommendationEngine = recommendationEngine;
        this.catalogLoader = catalogLoader;
    }

    public CrossMatchAnalysis analyze() {
        List<Match> matches = matchService.listAllWithEvents();
        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();

        List<AggregatedMistake> topMistakes = aggregateMistakes(matches);
        List<OpponentMoveStats> movePreferences = aggregateMovePreferences(matches, catalog);
        List<AveragedFatiguePoint> avgFatigue = averageFatigueCurve(matches);

        return new CrossMatchAnalysis(matches.size(), topMistakes, movePreferences, avgFatigue);
    }

    private List<AggregatedMistake> aggregateMistakes(List<Match> matches) {
        // key = "opponentMoveKey|fromPositionKey"
        Map<String, int[]> countMap = new LinkedHashMap<>();
        Map<String, double[]> gapSumMap = new LinkedHashMap<>();
        Map<String, MistakeSeverity[]> severityMap = new LinkedHashMap<>();
        Map<String, MistakeFinding> sampleMap = new LinkedHashMap<>();

        for (Match match : matches) {
            if (match.getSelfRole() == null) continue;
            for (MistakeFinding f : recommendationEngine.analyze(match)) {
                String key = f.opponentMoveKey() + "|" + (f.fromPositionKey() != null ? f.fromPositionKey() : "");
                sampleMap.putIfAbsent(key, f);
                countMap.computeIfAbsent(key, k -> new int[]{0})[0]++;
                gapSumMap.computeIfAbsent(key, k -> new double[]{0.0})[0] += f.scoreGap();
                MistakeSeverity[] sev = severityMap.computeIfAbsent(key, k -> new MistakeSeverity[]{MistakeSeverity.LOW});
                if (f.severity().ordinal() < sev[0].ordinal()) sev[0] = f.severity();
            }
        }

        return sampleMap.entrySet().stream()
                .map(e -> {
                    String key = e.getKey();
                    MistakeFinding sample = e.getValue();
                    int count = countMap.get(key)[0];
                    double avgGap = gapSumMap.get(key)[0] / count;
                    MistakeSeverity severity = severityMap.get(key)[0];
                    return new AggregatedMistake(
                            sample.opponentMoveKey(), sample.opponentMoveLabel(),
                            sample.fromPositionKey(), sample.fromPositionLabel(),
                            sample.userMoveKey(), sample.userMoveLabel(),
                            count, avgGap, severity);
                })
                .sorted(Comparator.comparingInt(AggregatedMistake::count).reversed())
                .limit(10)
                .toList();
    }

    private List<OpponentMoveStats> aggregateMovePreferences(List<Match> matches, MoveCatalogSnapshot catalog) {
        Map<String, Map<String, Integer>> opponentMoveCounts = new LinkedHashMap<>();
        for (Match match : matches) {
            String opponent = match.getOpponentName();
            String selfRole = match.getSelfRole();
            if (opponent == null || selfRole == null) continue;
            for (MatchEvent evt : match.getEvents()) {
                if (!selfRole.equals(evt.getActor())) continue;
                opponentMoveCounts
                        .computeIfAbsent(opponent, k -> new LinkedHashMap<>())
                        .merge(evt.getMoveKey(), 1, Integer::sum);
            }
        }

        List<OpponentMoveStats> result = new ArrayList<>();
        for (var entry : opponentMoveCounts.entrySet()) {
            String opponent = entry.getKey();
            entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> {
                        String moveLabel = catalog.findMove(e.getKey()).map(m -> m.label()).orElse(e.getKey());
                        result.add(new OpponentMoveStats(opponent, e.getKey(), moveLabel, e.getValue()));
                    });
        }
        return result;
    }

    private List<AveragedFatiguePoint> averageFatigueCurve(List<Match> matches) {
        int numBuckets = 5;
        double[] sumA = new double[numBuckets];
        double[] sumB = new double[numBuckets];
        int[] countA = new int[numBuckets];
        int[] countB = new int[numBuckets];

        for (Match match : matches) {
            List<MatchEvent> events = match.getEvents();
            int total = events.size();
            if (total == 0) continue;
            for (MatchEvent evt : events) {
                if (evt.getFatigueA() == null && evt.getFatigueB() == null) continue;
                int bucket = Math.min((int) ((double) evt.getStepIndex() / total * numBuckets), numBuckets - 1);
                if (evt.getFatigueA() != null) {
                    sumA[bucket] += evt.getFatigueA();
                    countA[bucket]++;
                }
                if (evt.getFatigueB() != null) {
                    sumB[bucket] += evt.getFatigueB();
                    countB[bucket]++;
                }
            }
        }

        int[] bucketPcts = {10, 30, 50, 70, 90};
        List<AveragedFatiguePoint> result = new ArrayList<>();
        for (int i = 0; i < numBuckets; i++) {
            int samples = Math.max(countA[i], countB[i]);
            if (samples == 0) continue;
            double avgA = countA[i] > 0 ? sumA[i] / countA[i] : -1;
            double avgB = countB[i] > 0 ? sumB[i] / countB[i] : -1;
            result.add(new AveragedFatiguePoint(bucketPcts[i], avgA, avgB, samples));
        }
        return result;
    }
}
