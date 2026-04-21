package hu.blackbelt.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfile() {
        return repository.findFirstByOrderByIdAsc();
    }

    @Transactional
    public UserProfile save(UserProfile profile) {
        return repository.save(profile);
    }

    public int calculateAge(UserProfile profile) {
        return Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
    }

    /** Mifflin-St Jeor formula */
    public double calculateBMR(UserProfile profile) {
        int age = calculateAge(profile);
        double w = profile.getWeightKg();
        double h = profile.getHeightCm();
        if (profile.getGender() == Gender.MALE) {
            return (10 * w) + (6.25 * h) - (5 * age) + 5;
        } else {
            return (10 * w) + (6.25 * h) - (5 * age) - 161;
        }
    }

    public double calculateBMI(UserProfile profile) {
        double heightM = profile.getHeightCm() / 100.0;
        return profile.getWeightKg() / (heightM * heightM);
    }

    public String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal weight";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }

    /** BMR × lifestyle activity factor */
    public double calculateTDEE(UserProfile profile) {
        double bmr = calculateBMR(profile);
        double factor = profile.getLifestyle().getActivityFactor();
        // Extra calories from explicit BJJ training (MET ≈ 8 for grappling)
        double trainingCalories = 8.0 * profile.getWeightKg() * profile.getTrainingHoursPerDay();
        return (bmr * factor) + trainingCalories;
    }

    /** Lifestyle activity factor (PAL / MET multiplier) */
    public double getActivityFactor(UserProfile profile) {
        return profile.getLifestyle().getActivityFactor();
    }

    /** 2.0 g per kg for active martial artists */
    public double calculateProteinTarget(UserProfile profile) {
        return 2.0 * profile.getWeightKg();
    }

    /** 35 ml/kg baseline + 500 ml per training hour */
    public double calculateWaterTarget(UserProfile profile) {
        return (35.0 * profile.getWeightKg()) + (500.0 * profile.getTrainingHoursPerDay());
    }

    /** Calories burned in a single BJJ session (MET 8) */
    public double trainingCaloriesPerSession(UserProfile profile, double durationMinutes) {
        return 8.0 * profile.getWeightKg() * (durationMinutes / 60.0);
    }
}
