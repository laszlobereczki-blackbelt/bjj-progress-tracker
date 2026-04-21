package hu.blackbelt.belt;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Entity
@Table(name = "belt_promotion")
public class BeltPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "promotion_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "belt", nullable = false)
    private Belt belt;

    @Column(name = "stripes", nullable = false)
    private int stripes;

    @Column(name = "date_awarded", nullable = false)
    private LocalDate dateAwarded;

    @Column(name = "awarded_by")
    @Nullable
    private String awardedBy;

    @Column(name = "academy")
    @Nullable
    private String academy;

    @Column(name = "notes", length = 1000)
    @Nullable
    private String notes;

    protected BeltPromotion() {
    }

    public BeltPromotion(Belt belt, int stripes, LocalDate dateAwarded) {
        this.belt = belt;
        setStripes(stripes);
        this.dateAwarded = dateAwarded;
    }

    public @Nullable Long getId() {
        return id;
    }

    public Belt getBelt() {
        return belt;
    }

    public void setBelt(Belt belt) {
        this.belt = belt;
    }

    public int getStripes() {
        return stripes;
    }

    public void setStripes(int stripes) {
        if (stripes < 0 || stripes > 4) {
            throw new IllegalArgumentException("Stripes must be between 0 and 4");
        }
        this.stripes = stripes;
    }

    public LocalDate getDateAwarded() {
        return dateAwarded;
    }

    public void setDateAwarded(LocalDate dateAwarded) {
        this.dateAwarded = dateAwarded;
    }

    public @Nullable String getAwardedBy() {
        return awardedBy;
    }

    public void setAwardedBy(@Nullable String awardedBy) {
        this.awardedBy = awardedBy;
    }

    public @Nullable String getAcademy() {
        return academy;
    }

    public void setAcademy(@Nullable String academy) {
        this.academy = academy;
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
        BeltPromotion other = (BeltPromotion) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
