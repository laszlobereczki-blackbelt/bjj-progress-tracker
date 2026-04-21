package hu.blackbelt.profile;

public enum Lifestyle {
    SEDENTARY("Sedentary", 1.2, "Little or no exercise"),
    LIGHTLY_ACTIVE("Lightly Active", 1.375, "Light exercise 1–3 days/week"),
    MODERATELY_ACTIVE("Moderately Active", 1.55, "Moderate exercise 3–5 days/week"),
    VERY_ACTIVE("Very Active", 1.725, "Hard exercise 6–7 days/week"),
    EXTRA_ACTIVE("Extra Active", 1.9, "Very hard exercise or physical job");

    private final String label;
    private final double activityFactor;
    private final String description;

    Lifestyle(String label, double activityFactor, String description) {
        this.label = label;
        this.activityFactor = activityFactor;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public double getActivityFactor() {
        return activityFactor;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return label + " – " + description;
    }
}
