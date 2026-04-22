package hu.blackbelt.matchreplay.analysis;

public record MistakeFinding(
        int stepIndex,
        String userMoveKey,
        String userMoveLabel,
        String opponentMoveKey,
        String opponentMoveLabel,
        String fromPositionKey,
        String fromPositionLabel,
        int chosenScore,
        String topMoveKey,
        String topMoveLabel,
        int topScore,
        String topReason,
        int scoreGap,
        MistakeSeverity severity) {
}
