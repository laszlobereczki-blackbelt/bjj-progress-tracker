package hu.blackbelt.matchreplay.analysis;

import java.util.List;

public record MatchAnalysis(
        List<MoveFrequency> frequenciesA,
        List<MoveFrequency> frequenciesB,
        List<PatternOccurrence> patterns,
        List<FatiguePoint> fatigueSeries,
        List<PositionTime> positionTimes) {
}
