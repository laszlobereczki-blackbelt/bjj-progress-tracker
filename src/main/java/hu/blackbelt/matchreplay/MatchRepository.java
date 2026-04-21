package hu.blackbelt.matchreplay;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface MatchRepository extends JpaRepository<Match, Long> {

    Slice<Match> findAllByOrderByMatchDateDesc(Pageable pageable);

    @Query("SELECT m FROM Match m LEFT JOIN FETCH m.events WHERE m.id = :id")
    Optional<Match> findByIdWithEvents(@Param("id") Long id);
}
