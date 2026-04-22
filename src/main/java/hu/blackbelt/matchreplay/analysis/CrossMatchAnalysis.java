package hu.blackbelt.matchreplay.analysis;

import java.util.List;

public record CrossMatchAnalysis(
        int totalMatches,
        List<AggregatedMistake> topMistakes,
        List<OpponentMoveStats> movePreferencesByOpponent,
        List<AveragedFatiguePoint> avgFatigueCurve) {
}
