package hu.blackbelt.matchreplay.analysis;

public enum MistakeSeverity {
    HIGH,   // score gap >= 5
    MEDIUM, // score gap 3-4
    LOW     // score gap 1-2, hidden by default
}
