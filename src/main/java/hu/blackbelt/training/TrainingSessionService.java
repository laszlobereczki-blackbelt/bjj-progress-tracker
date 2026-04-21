package hu.blackbelt.training;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainingSessionService {

    private final TrainingSessionRepository repository;

    TrainingSessionService(TrainingSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TrainingSession> list(Pageable pageable) {
        return repository.findAllByOrderByDateDesc(pageable).toList();
    }

    @Transactional
    public TrainingSession save(TrainingSession session) {
        return repository.save(session);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getTotalMinutes() {
        return repository.sumDurationMinutes();
    }

    @Transactional(readOnly = true)
    public long getMinutesByType(TrainingType type) {
        return repository.sumDurationMinutesByType(type);
    }

    @Transactional(readOnly = true)
    public long countBetween(LocalDate start, LocalDate end) {
        return repository.countByDateBetween(start, end);
    }
}
