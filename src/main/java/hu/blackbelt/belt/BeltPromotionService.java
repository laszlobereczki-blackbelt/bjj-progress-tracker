package hu.blackbelt.belt;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BeltPromotionService {

    private final BeltPromotionRepository repository;

    BeltPromotionService(BeltPromotionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BeltPromotion> list(Pageable pageable) {
        return repository.findAllByOrderByDateAwardedDesc(pageable).toList();
    }

    @Transactional
    public BeltPromotion save(BeltPromotion promotion) {
        return repository.save(promotion);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<BeltPromotion> getLatestPromotion() {
        return repository.findTopByOrderByDateAwardedDesc();
    }
}
