package hu.blackbelt.matchreplay;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "match_event")
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(name = "timestamp_seconds", nullable = false)
    private int timestampSeconds;

    @Column(name = "actor", nullable = false, length = 1)
    private String actor;

    @Column(name = "move_key", nullable = false)
    private String moveKey;

    @Column(name = "position_after")
    @Nullable
    private String positionAfter;

    @Column(name = "fatigue_a")
    @Nullable
    private Integer fatigueA;

    @Column(name = "fatigue_b")
    @Nullable
    private Integer fatigueB;

    @Column(name = "note", length = 500)
    @Nullable
    private String note;

    protected MatchEvent() {
    }

    public MatchEvent(int stepIndex, int timestampSeconds, String actor, String moveKey) {
        this.stepIndex = stepIndex;
        this.timestampSeconds = timestampSeconds;
        this.actor = actor;
        this.moveKey = moveKey;
    }

    public @Nullable Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    void setMatch(Match match) {
        this.match = match;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public int getTimestampSeconds() {
        return timestampSeconds;
    }

    public void setTimestampSeconds(int timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getMoveKey() {
        return moveKey;
    }

    public void setMoveKey(String moveKey) {
        this.moveKey = moveKey;
    }

    public @Nullable String getPositionAfter() {
        return positionAfter;
    }

    public void setPositionAfter(@Nullable String positionAfter) {
        this.positionAfter = positionAfter;
    }

    public @Nullable Integer getFatigueA() {
        return fatigueA;
    }

    public void setFatigueA(@Nullable Integer fatigueA) {
        this.fatigueA = fatigueA;
    }

    public @Nullable Integer getFatigueB() {
        return fatigueB;
    }

    public void setFatigueB(@Nullable Integer fatigueB) {
        this.fatigueB = fatigueB;
    }

    public @Nullable String getNote() {
        return note;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) return false;
        if (obj == this) return true;
        MatchEvent other = (MatchEvent) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
