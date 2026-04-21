package hu.blackbelt.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.blackbelt.belt.BeltPromotion;
import hu.blackbelt.belt.BeltPromotionService;
import hu.blackbelt.competition.Competition;
import hu.blackbelt.competition.CompetitionService;
import hu.blackbelt.training.TrainingSession;
import hu.blackbelt.training.TrainingSessionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class DummyDataService {

    private final TrainingSessionService trainingSessionService;
    private final CompetitionService competitionService;
    private final BeltPromotionService beltPromotionService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    DummyDataService(TrainingSessionService trainingSessionService,
                     CompetitionService competitionService,
                     BeltPromotionService beltPromotionService) {
        this.trainingSessionService = trainingSessionService;
        this.competitionService = competitionService;
        this.beltPromotionService = beltPromotionService;
    }

    @Transactional
    public void resetToDemo() {
        trainingSessionService.deleteAll();
        competitionService.deleteAll();
        beltPromotionService.deleteAll();

        try {
            var resource = new ClassPathResource("dummy-data.json");
            var data = objectMapper.readValue(resource.getInputStream(), DemoData.class);
            data.trainingSessions().forEach(trainingSessionService::save);
            data.competitions().forEach(competitionService::save);
            data.beltPromotions().forEach(beltPromotionService::save);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load demo data", e);
        }
    }

    record DemoData(
            List<TrainingSession> trainingSessions,
            List<Competition> competitions,
            List<BeltPromotion> beltPromotions
    ) {}
}
