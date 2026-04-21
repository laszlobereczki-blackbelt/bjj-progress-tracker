package hu.blackbelt.matchreplay;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "match_replay")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "match_id")
    private Long id;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "opponent_name")
    @Nullable
    private String opponentName;

    @Column(name = "self_role", length = 1)
    @Nullable
    private String selfRole;

    @Column(name = "duration_seconds")
    @Nullable
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_outcome", length = 20)
    @Nullable
    private ResultOutcome resultOutcome;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepIndex ASC")
    private List<MatchEvent> events = new ArrayList<>();

    protected Match() {
    }

    public Match(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public @Nullable Long getId() {
        return id;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public @Nullable String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(@Nullable String opponentName) {
        this.opponentName = opponentName;
    }

    public @Nullable String getSelfRole() {
        return selfRole;
    }

    public void setSelfRole(@Nullable String selfRole) {
        this.selfRole = selfRole;
    }

    public @Nullable Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(@Nullable Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public @Nullable ResultOutcome getResultOutcome() {
        return resultOutcome;
    }

    public void setResultOutcome(@Nullable ResultOutcome resultOutcome) {
        this.resultOutcome = resultOutcome;
    }

    public List<MatchEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    void addEvent(MatchEvent event) {
        event.setMatch(this);
        events.add(event);
    }

    void removeLastEvent() {
        if (!events.isEmpty()) {
            events.remove(events.size() - 1);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) return false;
        if (obj == this) return true;
        Match other = (Match) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
