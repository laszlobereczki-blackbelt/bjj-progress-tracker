package hu.blackbelt.matchreplay.analysis;

public record OpponentMoveStats(
        String opponentName,
        String userMoveKey,
        String userMoveLabel,
        int count) {
}
