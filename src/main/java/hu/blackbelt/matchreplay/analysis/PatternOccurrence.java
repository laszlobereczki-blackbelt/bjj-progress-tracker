package hu.blackbelt.matchreplay.analysis;

import java.util.List;

public record PatternOccurrence(String actor, List<String> sequence, int occurrences) {
}
