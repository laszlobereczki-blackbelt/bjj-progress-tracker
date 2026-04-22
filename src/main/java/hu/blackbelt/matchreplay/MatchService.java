package hu.blackbelt.matchreplay;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final MatchRepository repository;

    MatchService(MatchRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Match> list(Pageable pageable) {
        return repository.findAllByOrderByMatchDateDesc(pageable).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Match> findByIdWithEvents(Long id) {
        return repository.findByIdWithEvents(id);
    }

    @Transactional
    public Match save(Match match) {
        return repository.save(match);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public Match appendEvent(Long matchId, String actor, String moveKey, int timestampSeconds,
                             String positionAfter, Integer fatigueA, Integer fatigueB, String note) {
        Match match = repository.findByIdWithEvents(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
        int nextStep = match.getEvents().size();
        MatchEvent event = new MatchEvent(nextStep, timestampSeconds, actor, moveKey);
        event.setPositionAfter(positionAfter == null || positionAfter.isBlank() ? null : positionAfter);
        event.setFatigueA(fatigueA);
        event.setFatigueB(fatigueB);
        event.setNote(note == null || note.isBlank() ? null : note);
        match.addEvent(event);
        return repository.save(match);
    }

    @Transactional(readOnly = true)
    public List<Match> listAllWithEvents() {
        return repository.findAllWithEvents();
    }

    @Transactional
    public Match removeLastEvent(Long matchId) {
        Match match = repository.findByIdWithEvents(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
        match.removeLastEvent();
        return repository.save(match);
    }
}
