package hu.blackbelt.matchreplay.analysis;

public record AggregatedMistake(
        String opponentMoveKey,
        String opponentMoveLabel,
        String fromPositionKey,
        String fromPositionLabel,
        String userMoveKey,
        String userMoveLabel,
        int count,
        double avgScoreGap,
        MistakeSeverity severity) {
}
