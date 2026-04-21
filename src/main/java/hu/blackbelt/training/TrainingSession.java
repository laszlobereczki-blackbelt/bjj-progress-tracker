package hu.blackbelt.training;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Entity
@Table(name = "training_session")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "session_id")
    private Long id;

    @Column(name = "session_date", nullable = false)
    private LocalDate date;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TrainingType type;

    @Column(name = "location")
    @Nullable
    private String location;

    @Column(name = "notes", length = 1000)
    @Nullable
    private String notes;

    protected TrainingSession() {
    }

    public TrainingSession(LocalDate date, int durationMinutes, TrainingType type) {
        this.date = date;
        this.durationMinutes = durationMinutes;
        this.type = type;
    }

    public @Nullable Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes < 1) {
            throw new IllegalArgumentException("Duration must be at least 1 minute");
        }
        this.durationMinutes = durationMinutes;
    }

    public TrainingType getType() {
        return type;
    }

    public void setType(TrainingType type) {
        this.type = type;
    }

    public @Nullable String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    public @Nullable String getNotes() {
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        TrainingSession other = (TrainingSession) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
