package hu.blackbelt.belt;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface BeltPromotionRepository extends JpaRepository<BeltPromotion, Long> {

    Slice<BeltPromotion> findAllByOrderByDateAwardedDesc(Pageable pageable);

    Optional<BeltPromotion> findTopByOrderByDateAwardedDesc();
}
