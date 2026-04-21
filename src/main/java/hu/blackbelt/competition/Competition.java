package hu.blackbelt.competition;

import hu.blackbelt.belt.Belt;
import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Entity
@Table(name = "competition")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "competition_id")
    private Long id;

    @Column(name = "competition_date", nullable = false)
    private LocalDate date;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location")
    @Nullable
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "division")
    @Nullable
    private Belt division;

    @Column(name = "weight_class")
    @Nullable
    private String weightClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    @Nullable
    private CompetitionResult result;

    @Column(name = "matches_won")
    private int matchesWon;

    @Column(name = "matches_lost")
    private int matchesLost;

    @Column(name = "notes", length = 1000)
    @Nullable
    private String notes;

    protected Competition() {
    }

    public Competition(LocalDate date, String name) {
        this.date = date;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    public @Nullable Belt getDivision() {
        return division;
    }

    public void setDivision(@Nullable Belt division) {
        this.division = division;
    }

    public @Nullable String getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(@Nullable String weightClass) {
        this.weightClass = weightClass;
    }

    public @Nullable CompetitionResult getResult() {
        return result;
    }

    public void setResult(@Nullable CompetitionResult result) {
        this.result = result;
    }

    public int getMatchesWon() {
        return matchesWon;
    }

    public void setMatchesWon(int matchesWon) {
        this.matchesWon = matchesWon;
    }

    public int getMatchesLost() {
        return matchesLost;
    }

    public void setMatchesLost(int matchesLost) {
        this.matchesLost = matchesLost;
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
        Competition other = (Competition) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
