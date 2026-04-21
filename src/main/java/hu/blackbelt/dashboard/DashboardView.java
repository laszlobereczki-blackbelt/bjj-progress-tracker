package hu.blackbelt.dashboard;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.ViewTitle;
import hu.blackbelt.belt.BeltPromotionService;
import hu.blackbelt.competition.CompetitionResult;
import hu.blackbelt.competition.CompetitionService;
import hu.blackbelt.training.TrainingSessionService;
import hu.blackbelt.training.TrainingType;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Route("")
@PageTitle("Dashboard")
@Menu(order = 0, icon = "vaadin:dashboard", title = "Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(TrainingSessionService trainingService,
                         BeltPromotionService beltService,
                         CompetitionService competitionService) {
        setPadding(true);
        setSpacing(true);

        add(new ViewTitle("Dashboard"));

        add(buildMatTimeSection(trainingService));
        add(buildSessionCountSection(trainingService));
        add(buildBeltSection(beltService));
        add(buildCompetitionSection(competitionService));
    }

    private VerticalLayout buildMatTimeSection(TrainingSessionService service) {
        long totalMinutes = service.getTotalMinutes();
        long giMinutes = service.getMinutesByType(TrainingType.GI);
        long nogiMinutes = service.getMinutesByType(TrainingType.NOGI);
        long grapplingMinutes = service.getMinutesByType(TrainingType.GRAPPLING);

        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.addClassNames("dashboard-section");

        section.add(new H3("Mat Time"));
        section.add(stat("Total", formatHours(totalMinutes)));

        if (totalMinutes > 0) {
            section.add(stat("Gi", formatHours(giMinutes) + pct(giMinutes, totalMinutes)));
            section.add(stat("No-Gi", formatHours(nogiMinutes) + pct(nogiMinutes, totalMinutes)));
            section.add(stat("Grappling", formatHours(grapplingMinutes) + pct(grapplingMinutes, totalMinutes)));
        }

        return section;
    }

    private VerticalLayout buildSessionCountSection(TrainingSessionService service) {
        var now = LocalDate.now();
        long thisWeek = service.countBetween(now.minusWeeks(1), now);
        long thisMonth = service.countBetween(now.withDayOfMonth(1), now);
        long thisYear = service.countBetween(now.withDayOfYear(1), now);

        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        section.add(new H3("Sessions"));
        section.add(stat("This week", String.valueOf(thisWeek)));
        section.add(stat("This month", String.valueOf(thisMonth)));
        section.add(stat("This year", String.valueOf(thisYear)));

        return section;
    }

    private VerticalLayout buildBeltSection(BeltPromotionService service) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        section.add(new H3("Belt"));

        service.getLatestPromotion().ifPresentOrElse(promotion -> {
            var beltLabel = promotion.getBelt().name().charAt(0)
                    + promotion.getBelt().name().substring(1).toLowerCase();
            var stripeLabel = promotion.getStripes() + " stripe" + (promotion.getStripes() == 1 ? "" : "s");
            section.add(stat("Current", beltLabel + " — " + stripeLabel));

            long days = ChronoUnit.DAYS.between(promotion.getDateAwarded(), LocalDate.now());
            section.add(stat("Time at belt", formatDays(days)));
            section.add(stat("Since", promotion.getDateAwarded().toString()));
        }, () -> section.add(new Span("No promotions recorded yet")));

        return section;
    }

    private VerticalLayout buildCompetitionSection(CompetitionService service) {
        long total = service.countTotal();
        long gold = service.countByResult(CompetitionResult.GOLD);
        long silver = service.countByResult(CompetitionResult.SILVER);
        long bronze = service.countByResult(CompetitionResult.BRONZE);
        long won = service.getTotalMatchesWon();
        long lost = service.getTotalMatchesLost();

        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);

        section.add(new H3("Competitions"));
        section.add(stat("Total", String.valueOf(total)));
        section.add(stat("Medals", gold + " gold  " + silver + " silver  " + bronze + " bronze"));
        section.add(stat("Match record", won + "W / " + lost + "L"));

        return section;
    }

    private HorizontalLayout stat(String label, String value) {
        var labelSpan = new Span(label + ":");
        labelSpan.getStyle().set("font-weight", "600").set("min-width", "10em").set("color", "var(--vaadin-text-color-secondary)");
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
        if (hours == 0) {
            return mins + " min";
        }
        return hours + "h " + (mins > 0 ? mins + "m" : "");
    }

    private String pct(long part, long total) {
        if (total == 0) return "";
        long pct = Math.round(100.0 * part / total);
        return "  (" + pct + "%)";
    }

    private String formatDays(long days) {
        if (days < 30) return days + " days";
        if (days < 365) return (days / 30) + " months";
        long years = days / 365;
        long months = (days % 365) / 30;
        return years + "y " + (months > 0 ? months + "m" : "");
    }
}
