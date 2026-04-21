package hu.blackbelt.profile;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "profile_id")
    private @Nullable Long id;

    @Column(name = "name", nullable = false)
    private String name = "";

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth = LocalDate.now().minusYears(25);

    @Column(name = "weight_kg", nullable = false)
    private double weightKg = 75.0;

    @Column(name = "height_cm", nullable = false)
    private double heightCm = 175.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender = Gender.MALE;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifestyle", nullable = false)
    private Lifestyle lifestyle = Lifestyle.MODERATELY_ACTIVE;

    @Column(name = "training_hours_per_day", nullable = false)
    private double trainingHoursPerDay = 1.0;

    public UserProfile() {
    }

    public UserProfile(String name, LocalDate dateOfBirth, double weightKg, double heightCm,
                       Gender gender, Lifestyle lifestyle, double trainingHoursPerDay) {
        setName(name);
        setDateOfBirth(dateOfBirth);
        setWeightKg(weightKg);
        setHeightCm(heightCm);
        setGender(gender);
        setLifestyle(lifestyle);
        setTrainingHoursPerDay(trainingHoursPerDay);
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) throw new IllegalArgumentException("Date of birth is required");
        this.dateOfBirth = dateOfBirth;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg < 20 || weightKg > 300) throw new IllegalArgumentException("Weight must be between 20 and 300 kg");
        this.weightKg = weightKg;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(double heightCm) {
        if (heightCm < 100 || heightCm > 250) throw new IllegalArgumentException("Height must be between 100 and 250 cm");
        this.heightCm = heightCm;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        if (gender == null) throw new IllegalArgumentException("Gender is required");
        this.gender = gender;
    }

    public Lifestyle getLifestyle() {
        return lifestyle;
    }

    public void setLifestyle(Lifestyle lifestyle) {
        if (lifestyle == null) throw new IllegalArgumentException("Lifestyle is required");
        this.lifestyle = lifestyle;
    }

    public double getTrainingHoursPerDay() {
        return trainingHoursPerDay;
    }

    public void setTrainingHoursPerDay(double trainingHoursPerDay) {
        if (trainingHoursPerDay < 0 || trainingHoursPerDay > 12)
            throw new IllegalArgumentException("Training hours must be between 0 and 12");
        this.trainingHoursPerDay = trainingHoursPerDay;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) return false;
        if (obj == this) return true;
        UserProfile other = (UserProfile) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
