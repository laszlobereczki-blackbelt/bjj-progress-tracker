package hu.blackbelt.competition;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompetitionService {

    private final CompetitionRepository repository;

    CompetitionService(CompetitionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Competition> list(Pageable pageable) {
        return repository.findAllByOrderByDateDesc(pageable).toList();
    }

    @Transactional
    public Competition save(Competition competition) {
        return repository.save(competition);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countTotal() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public long countByResult(CompetitionResult result) {
        return repository.countByResult(result);
    }

    @Transactional(readOnly = true)
    public long getTotalMatchesWon() {
        return repository.sumMatchesWon();
    }

    @Transactional(readOnly = true)
    public long getTotalMatchesLost() {
        return repository.sumMatchesLost();
    }
}
