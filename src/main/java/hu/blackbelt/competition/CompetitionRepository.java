package hu.blackbelt.competition;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CompetitionRepository extends JpaRepository<Competition, Long> {

    Slice<Competition> findAllByOrderByDateDesc(Pageable pageable);

    long countByResult(CompetitionResult result);

    @Query("SELECT COALESCE(SUM(c.matchesWon), 0) FROM Competition c")
    long sumMatchesWon();

    @Query("SELECT COALESCE(SUM(c.matchesLost), 0) FROM Competition c")
    long sumMatchesLost();
}
