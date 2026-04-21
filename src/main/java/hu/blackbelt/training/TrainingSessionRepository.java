package hu.blackbelt.training;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    Slice<TrainingSession> findAllByOrderByDateDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TrainingSession t")
    long sumDurationMinutes();

    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TrainingSession t WHERE t.type = :type")
    long sumDurationMinutesByType(@Param("type") TrainingType type);

    long countByDateBetween(LocalDate start, LocalDate end);
}
