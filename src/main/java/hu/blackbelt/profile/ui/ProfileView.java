package hu.blackbelt.profile.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.ViewTitle;
import hu.blackbelt.belt.BeltPromotionService;
import hu.blackbelt.competition.CompetitionResult;
import hu.blackbelt.competition.CompetitionService;
import hu.blackbelt.profile.Gender;
import hu.blackbelt.profile.Lifestyle;
import hu.blackbelt.profile.UserProfile;
import hu.blackbelt.profile.UserProfileService;
import hu.blackbelt.training.TrainingSessionService;
import hu.blackbelt.training.TrainingType;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Route("profile")
@PageTitle("Profile & Stats")
@Menu(order = 4, icon = "vaadin:user", title = "Profile & Stats")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final UserProfileService profileService;
    private final TrainingSessionService trainingService;
    private final CompetitionService competitionService;
    private final BeltPromotionService beltService;

    private final VerticalLayout statsPanel = new VerticalLayout();
    private final VerticalLayout miscPanel = new VerticalLayout();

    private final TextField nameField = new TextField("Name");
    private final DatePicker dobPicker = new DatePicker("Date of Birth");
    private final ComboBox<Gender> genderCombo = new ComboBox<>("Gender");
    private final NumberField weightField = new NumberField("Weight");
    private final NumberField heightField = new NumberField("Height");
    private final ComboBox<Lifestyle> lifestyleCombo = new ComboBox<>("Lifestyle");
    private final NumberField trainingHoursField = new NumberField("Training Hours Per Day");

    public ProfileView(UserProfileService profileService,
                       TrainingSessionService trainingService,
                       CompetitionService competitionService,
                       BeltPromotionService beltService) {
        this.profileService = profileService;
        this.trainingService = trainingService;
        this.competitionService = competitionService;
        this.beltService = beltService;

        setPadding(true);
        setSpacing(true);
        add(new ViewTitle("Profile & Stats"));

        var tabSheet = new TabSheet();
        tabSheet.setWidthFull();
        tabSheet.add("Profile", buildProfileTab());
        tabSheet.add("Stats", statsPanel);
        tabSheet.add("Miscellaneous", miscPanel);
        add(tabSheet);

        loadProfile();
    }

    private void loadProfile() {
        profileService.getProfile().ifPresent(profile -> {
            nameField.setValue(profile.getName());
            dobPicker.setValue(profile.getDateOfBirth());
            genderCombo.setValue(profile.getGender());
            weightField.setValue(profile.getWeightKg());
            heightField.setValue(profile.getHeightCm());
            lifestyleCombo.setValue(profile.getLifestyle());
            trainingHoursField.setValue(profile.getTrainingHoursPerDay());
        });
        refreshPanels();
    }

    private VerticalLayout buildProfileTab() {
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setTooltipText("Your full name.");

        dobPicker.setRequired(true);
        dobPicker.setMax(LocalDate.now().minusYears(10));
        dobPicker.setMin(LocalDate.now().minusYears(120));
        dobPicker.setTooltipText("Used to calculate your age for the BMR formula.");

        genderCombo.setItems(Gender.values());
        genderCombo.setRequired(true);
        genderCombo.setTooltipText("Biological sex used in the Mifflin-St Jeor BMR formula.");

        weightField.setSuffixComponent(new Span("kg"));
        weightField.setMin(20);
        weightField.setMax(300);
        weightField.setRequired(true);
        weightField.setTooltipText("Your current body weight in kilograms.");

        heightField.setSuffixComponent(new Span("cm"));
        heightField.setMin(100);
        heightField.setMax(250);
        heightField.setRequired(true);
        heightField.setTooltipText("Your height in centimetres.");

        lifestyleCombo.setItems(Lifestyle.values());
        lifestyleCombo.setRequired(true);
        lifestyleCombo.setWidthFull();
        lifestyleCombo.setTooltipText("Your general daily activity level outside of dedicated BJJ training.");

        trainingHoursField.setSuffixComponent(new Span("h/day"));
        trainingHoursField.setMin(0);
        trainingHoursField.setMax(12);
        trainingHoursField.setStep(0.25);
        trainingHoursField.setRequired(true);
        trainingHoursField.setTooltipText("Average BJJ/grappling training per day, averaged across the week.");

        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        form.add(nameField, 2);
        form.add(dobPicker);
        form.add(genderCombo);
        form.add(weightField);
        form.add(heightField);
        form.add(lifestyleCombo, 2);
        form.add(trainingHoursField);

        var saveButton = new Button("Save Profile");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveProfile());

        var content = new VerticalLayout(form, saveButton);
        content.setPadding(false);
        return content;
    }

    private void saveProfile() {
        if (!validateForm()) return;

        var profile = profileService.getProfile().orElse(new UserProfile());
        profile.setName(nameField.getValue());
        profile.setDateOfBirth(dobPicker.getValue());
        profile.setGender(genderCombo.getValue());
        profile.setWeightKg(weightField.getValue());
        profile.setHeightCm(heightField.getValue());
        profile.setLifestyle(lifestyleCombo.getValue());
        profile.setTrainingHoursPerDay(trainingHoursField.getValue());

        profileService.save(profile);
        refreshPanels();

        var n = Notification.show("Profile saved");
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private boolean validateForm() {
        boolean valid = true;
        if (nameField.isEmpty()) { nameField.setInvalid(true); valid = false; }
        if (dobPicker.isEmpty()) { dobPicker.setInvalid(true); valid = false; }
        if (genderCombo.isEmpty()) { genderCombo.setInvalid(true); valid = false; }
        if (weightField.isEmpty()) { weightField.setInvalid(true); valid = false; }
        if (heightField.isEmpty()) { heightField.setInvalid(true); valid = false; }
        if (lifestyleCombo.isEmpty()) { lifestyleCombo.setInvalid(true); valid = false; }
        if (trainingHoursField.isEmpty()) { trainingHoursField.setInvalid(true); valid = false; }
        return valid;
    }

    private void refreshPanels() {
        statsPanel.removeAll();
        statsPanel.setPadding(false);
        miscPanel.removeAll();
        miscPanel.setPadding(false);

        var profileOpt = profileService.getProfile();
        if (profileOpt.isEmpty()) {
            var msg = new Span("Fill in your profile first to see calculated stats.");
            msg.getStyle().set("color", "var(--vaadin-text-color-secondary)");
            statsPanel.add(msg);
            miscPanel.add(msg);
            return;
        }

        buildStatsPanel(profileOpt.get());
        buildMiscPanel(profileOpt.get());
    }

    private void buildStatsPanel(UserProfile profile) {
        double bmr = profileService.calculateBMR(profile);
        double bmi = profileService.calculateBMI(profile);
        double tdee = profileService.calculateTDEE(profile);
        double activityFactor = profileService.getActivityFactor(profile);
        double protein = profileService.calculateProteinTarget(profile);
        double water = profileService.calculateWaterTarget(profile);
        int age = profileService.calculateAge(profile);

        statsPanel.add(new H3("Body Metrics"));
        statsPanel.add(statRow("Age",
                age + " years",
                "Calculated from your date of birth. Age directly affects BMR — metabolic rate typically " +
                "decreases by around 1–2% per decade after 30, so it is important to keep this up to date."));
        statsPanel.add(statRow("BMI",
                String.format("%.1f — %s", bmi, profileService.getBMICategory(bmi)),
                "Body Mass Index is derived from weight divided by height squared (kg/m²). " +
                "Normal range is 18.5–24.9. BMI does not distinguish muscle from fat, so " +
                "muscular grapplers often score in the overweight range while still being very healthy. " +
                "Use it as one signal, not the full picture."));

        statsPanel.add(new H3("Energy"));
        statsPanel.add(statRow("BMR",
                String.format("%.0f kcal/day", bmr),
                "Basal Metabolic Rate — the calories your body burns at complete rest to maintain vital " +
                "functions such as breathing, circulation, and cell repair. Calculated with the Mifflin-St Jeor " +
                "formula, which is the most accurate general-use equation for adults. " +
                "Sustained intake below this value is not advisable long-term."));
        statsPanel.add(statRow("Activity Factor (MET)",
                String.format("× %.3f  (%s)", activityFactor, profile.getLifestyle().getLabel()),
                "The Physical Activity Level multiplier, expressed here as a MET-based factor, scales your " +
                "BMR to reflect daily movement. Values range from 1.2 (sedentary) to 1.9 (extremely active). " +
                "Choosing the right level is the single biggest variable in TDEE accuracy — " +
                "most people underestimate their activity."));
        statsPanel.add(statRow("TDEE",
                String.format("%.0f kcal/day", tdee),
                "Total Daily Energy Expenditure — the total calories you burn in a day, combining your BMR " +
                "with your activity factor and dedicated BJJ training (estimated at MET 8 for grappling). " +
                "Eating at this level maintains your current weight. " +
                "Add ~300–500 kcal/day to gain lean mass; subtract the same to lose fat while preserving muscle."));

        statsPanel.add(new H3("Nutrition Targets"));
        statsPanel.add(statRow("Protein Target",
                String.format("%.0f g/day  (%.1f g/kg)", protein, 2.0),
                "Protein is the building block for muscle repair after hard training. BJJ practitioners benefit " +
                "from 1.6–2.2 g per kg of body weight. This target uses 2.0 g/kg — sufficient for frequent " +
                "sparring and drilling. Spreading intake evenly across 4–5 meals maximises muscle protein synthesis."));
        statsPanel.add(statRow("Water Target",
                String.format("%.0f ml/day  (%.1f L)", water, water / 1000.0),
                "Hydration is critical for grappling performance, joint lubrication, and recovery speed. " +
                "The baseline is 35 ml per kg of body weight, plus 500 ml for every hour of training. " +
                "Even mild dehydration (2% body weight) measurably reduces reaction time and grip strength. " +
                "Drink consistently throughout the day, not only around training sessions."));
    }

    private void buildMiscPanel(UserProfile profile) {
        var now = LocalDate.now();

        long totalMinutes = trainingService.getTotalMinutes();
        long giMinutes = trainingService.getMinutesByType(TrainingType.GI);
        long nogiMinutes = trainingService.getMinutesByType(TrainingType.NOGI);
        long grapplingMinutes = trainingService.getMinutesByType(TrainingType.GRAPPLING);
        long totalSessions = trainingService.countBetween(LocalDate.of(2000, 1, 1), now);
        long sessionsThisYear = trainingService.countBetween(now.withDayOfYear(1), now);
        long sessionsThisMonth = trainingService.countBetween(now.withDayOfMonth(1), now);

        long totalCompetitions = competitionService.countTotal();
        long matchesWon = competitionService.getTotalMatchesWon();
        long matchesLost = competitionService.getTotalMatchesLost();
        long gold = competitionService.countByResult(CompetitionResult.GOLD);
        long silver = competitionService.countByResult(CompetitionResult.SILVER);
        long bronze = competitionService.countByResult(CompetitionResult.BRONZE);

        double avgDuration = totalSessions > 0 ? (double) totalMinutes / totalSessions : 0;

        String dominantType = "–";
        long maxMin = Math.max(giMinutes, Math.max(nogiMinutes, grapplingMinutes));
        if (maxMin > 0) {
            if (maxMin == giMinutes) dominantType = "Gi";
            else if (maxMin == nogiMinutes) dominantType = "No-Gi";
            else dominantType = "Grappling";
        }

        double totalCaloriesBurned = profileService.trainingCaloriesPerSession(profile, totalMinutes);
        double monthlyCalories = sessionsThisMonth > 0
                ? profileService.trainingCaloriesPerSession(profile, avgDuration) * sessionsThisMonth
                : 0;

        long totalMatches = matchesWon + matchesLost;
        String winRate = totalMatches > 0
                ? String.format("%.0f%%", 100.0 * matchesWon / totalMatches)
                : "–";

        miscPanel.add(new H3("Training Summary"));
        miscPanel.add(stat("Total sessions", totalSessions + " sessions"));
        miscPanel.add(stat("This year / this month", sessionsThisYear + " / " + sessionsThisMonth));
        miscPanel.add(stat("Total mat time", formatHours(totalMinutes)));
        miscPanel.add(stat("Average session", String.format("%.0f min", avgDuration)));
        miscPanel.add(stat("Dominant style", dominantType));
        if (totalMinutes > 0) {
            miscPanel.add(stat("Gi / No-Gi / Grappling",
                    formatHours(giMinutes) + " / " + formatHours(nogiMinutes) + " / " + formatHours(grapplingMinutes)));
        }

        miscPanel.add(new H3("Calorie Estimates from Training"));
        miscPanel.add(stat("All-time burn", String.format("%.0f kcal", totalCaloriesBurned)));
        miscPanel.add(stat("This month's burn", String.format("%.0f kcal", monthlyCalories)));
        miscPanel.add(stat("Per session (avg)",
                String.format("%.0f kcal", avgDuration > 0
                        ? profileService.trainingCaloriesPerSession(profile, avgDuration) : 0)));

        miscPanel.add(new H3("Competition Record"));
        miscPanel.add(stat("Total tournaments", String.valueOf(totalCompetitions)));
        miscPanel.add(stat("Medals", gold + " gold  " + silver + " silver  " + bronze + " bronze"));
        miscPanel.add(stat("Match record", matchesWon + "W / " + matchesLost + "L"));
        miscPanel.add(stat("Win rate", winRate));

        miscPanel.add(new H3("Nutrition at a Glance"));
        miscPanel.add(stat("Daily calorie target",
                String.format("%.0f kcal", profileService.calculateTDEE(profile))));
        miscPanel.add(stat("Daily protein target",
                String.format("%.0f g", profileService.calculateProteinTarget(profile))));
        miscPanel.add(stat("Daily water target",
                String.format("%.0f ml  (%.1f L)",
                        profileService.calculateWaterTarget(profile),
                        profileService.calculateWaterTarget(profile) / 1000.0)));

        beltService.getLatestPromotion().ifPresent(promotion -> {
            miscPanel.add(new H3("Belt Journey"));
            String beltLabel = promotion.getBelt().name().charAt(0)
                    + promotion.getBelt().name().substring(1).toLowerCase();
            miscPanel.add(stat("Current rank",
                    beltLabel + " — " + promotion.getStripes() + " stripe(s)"));
            miscPanel.add(stat("Promoted on",
                    promotion.getDateAwarded().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
            if (promotion.getAwardedBy() != null) {
                miscPanel.add(stat("Awarded by", promotion.getAwardedBy()));
            }
            long sessionsSincePromotion = trainingService.countBetween(promotion.getDateAwarded(), now);
            long minutesSincePromotion = trainingService.getTotalMinutes();
            miscPanel.add(stat("Sessions since promotion", String.valueOf(sessionsSincePromotion)));
            miscPanel.add(stat("Mat time since promotion", "approx. " + formatHours(minutesSincePromotion)));
        });
    }

    private HorizontalLayout statRow(String label, String value, String tooltipText) {
        var labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("min-width", "14em")
                .set("color", "var(--vaadin-text-color-secondary)");

        var valueSpan = new Span(value);

        var infoIcon = VaadinIcon.INFO_CIRCLE_O.create();
        infoIcon.setSize("1em");
        infoIcon.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("cursor", "help")
                .set("flex-shrink", "0");
        Tooltip.forComponent(infoIcon).withText(tooltipText);

        var row = new HorizontalLayout(labelSpan, valueSpan, infoIcon);
        row.setPadding(false);
        row.setSpacing(false);
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("gap", "0.5em").set("padding", "0.15em 0");
        return row;
    }

    private HorizontalLayout stat(String label, String value) {
        var labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("min-width", "14em")
                .set("color", "var(--vaadin-text-color-secondary)");
        var valueSpan = new Span(value);
        var row = new HorizontalLayout(labelSpan, valueSpan);
        row.setPadding(false);
        row.setSpacing(false);
        row.getStyle().set("gap", "0.5em").set("padding", "0.15em 0");
        return row;
    }

    private String formatHours(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (hours == 0) return mins + " min";
        return hours + "h " + (mins > 0 ? mins + "m" : "");
    }
}
