package hu.blackbelt.matchreplay.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchEvent;
import hu.blackbelt.matchreplay.MatchService;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Route("match-lab/playback")
@PageTitle("Match Playback")
@PermitAll
public class MatchPlaybackView extends VerticalLayout implements HasUrlParameter<Long> {

    private final MatchService service;
    private Match match;
    private int currentStep = 0;

    private final Span stepIndicator = new Span();
    private final Div eventCard = new Div();
    private final Button prevBtn = new Button("← Prev");
    private final Button nextBtn = new Button("Next →");
    private final VerticalLayout allEventsList = new VerticalLayout();

    public MatchPlaybackView(MatchService service) {
        this.service = service;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long matchId) {
        if (matchId == null) {
            UI.getCurrent().navigate(MatchReplayListView.class);
            return;
        }
        match = service.findByIdWithEvents(matchId).orElse(null);
        if (match == null) {
            UI.getCurrent().navigate(MatchReplayListView.class);
            return;
        }
        currentStep = 0;
        buildUi();
    }

    private void buildUi() {
        removeAll();

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        String headerText = dateFormatter.format(match.getMatchDate())
                + (match.getOpponentName() != null ? " vs " + match.getOpponentName() : "");
        if (match.getSelfRole() != null) {
            headerText += " (you: " + match.getSelfRole() + ")";
        }

        var backBtn = new Button("← Back to list", e -> UI.getCurrent().navigate(MatchReplayListView.class));
        backBtn.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);

        var editBtn = new Button("Edit", e -> UI.getCurrent().navigate(MatchEditorView.class, match.getId()));
        editBtn.addThemeVariants(ButtonVariant.SMALL);

        var titleSpan = new Span(headerText);
        titleSpan.getStyle().set("font-size", "var(--lumo-font-size-l)").set("font-weight", "bold");

        var headerRow = new HorizontalLayout(backBtn, titleSpan, editBtn);
        headerRow.setWidthFull();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setFlexGrow(1, titleSpan);

        List<MatchEvent> events = match.getEvents();

        if (events.isEmpty()) {
            add(headerRow, new Span("No events recorded for this match yet."));
            return;
        }

        stepIndicator.getStyle().set("font-size", "var(--lumo-font-size-m)")
                .set("color", "var(--lumo-secondary-text-color)");

        eventCard.getStyle()
                .set("padding", "var(--lumo-space-l)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("min-height", "140px")
                .set("width", "100%")
                .set("max-width", "520px");

        prevBtn.addThemeVariants(ButtonVariant.TERTIARY);
        nextBtn.addThemeVariants(ButtonVariant.PRIMARY);
        prevBtn.addClickListener(e -> {
            if (currentStep > 0) {
                currentStep--;
                refreshStep();
            }
        });
        nextBtn.addClickListener(e -> {
            if (currentStep < events.size() - 1) {
                currentStep++;
                refreshStep();
            }
        });

        var navRow = new HorizontalLayout(prevBtn, stepIndicator, nextBtn);
        navRow.setAlignItems(FlexComponent.Alignment.CENTER);
        navRow.getStyle().set("gap", "var(--lumo-space-m)");

        allEventsList.setPadding(false);
        allEventsList.setSpacing(false);
        allEventsList.getStyle().set("gap", "2px").set("overflow-y", "auto");
        allEventsList.setWidthFull();

        refreshStep();

        var playerSection = new VerticalLayout(navRow, eventCard);
        playerSection.setAlignItems(FlexComponent.Alignment.CENTER);
        playerSection.setPadding(false);

        var divider = new H3("All Events");
        divider.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin", "var(--lumo-space-m) 0 var(--lumo-space-xs) 0");

        add(headerRow, playerSection, divider, allEventsList);
        setFlexGrow(1, allEventsList);
    }

    private void refreshStep() {
        List<MatchEvent> events = match.getEvents();
        stepIndicator.setText("Step " + (currentStep + 1) + " / " + events.size());
        prevBtn.setEnabled(currentStep > 0);
        nextBtn.setEnabled(currentStep < events.size() - 1);

        MatchEvent evt = events.get(currentStep);
        eventCard.removeAll();

        var actorLabel = new Span("Player " + evt.getActor());
        actorLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "bold")
                .set("color", "A".equals(evt.getActor()) ? "var(--lumo-primary-color)" : "var(--lumo-error-color)");

        var moveLabel = new Span(evt.getMoveKey());
        moveLabel.getStyle().set("font-size", "var(--lumo-font-size-l)");

        var tsLabel = new Span("@ " + evt.getTimestampSeconds() + "s");
        tsLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        var content = new VerticalLayout(actorLabel, moveLabel, tsLabel);
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-xs)");

        if (evt.getPositionAfter() != null) {
            var posLabel = new Span("Position after: " + evt.getPositionAfter());
            posLabel.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            content.add(posLabel);
        }

        if (evt.getFatigueA() != null || evt.getFatigueB() != null) {
            var fatigueRow = new HorizontalLayout();
            fatigueRow.setSpacing(true);
            fatigueRow.getStyle().set("gap", "var(--lumo-space-m)");

            var fatigueLabel = new Span("Fatigue — ");
            fatigueLabel.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            fatigueRow.add(fatigueLabel);

            var fatigueA = new Span("A: " + (evt.getFatigueA() != null ? evt.getFatigueA() + "/10" : "—"));
            fatigueA.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-primary-color)");
            var fatigueB = new Span("B: " + (evt.getFatigueB() != null ? evt.getFatigueB() + "/10" : "—"));
            fatigueB.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-error-color)");
            fatigueRow.add(fatigueA, fatigueB);
            content.add(fatigueRow);
        }

        if (evt.getNote() != null) {
            var noteLabel = new Span("Note: " + evt.getNote());
            noteLabel.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            content.add(noteLabel);
        }

        eventCard.add(content);

        allEventsList.removeAll();
        for (int i = 0; i < events.size(); i++) {
            allEventsList.add(buildEventRow(events.get(i), i == currentStep));
        }
    }

    private Div buildEventRow(MatchEvent evt, boolean active) {
        var stepSpan = new Span("#" + (evt.getStepIndex() + 1));
        stepSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "2.5em");

        var actorSpan = new Span(evt.getActor());
        actorSpan.getStyle().set("font-weight", "bold")
                .set("color", "A".equals(evt.getActor()) ? "var(--lumo-primary-color)" : "var(--lumo-error-color)")
                .set("min-width", "1.5em");

        var moveSpan = new Span(evt.getMoveKey());
        moveSpan.getStyle().set("flex-grow", "1");

        var tsSpan = new Span(evt.getTimestampSeconds() + "s");
        tsSpan.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

        var fatigueSpan = new Span(
                "A:" + (evt.getFatigueA() != null ? evt.getFatigueA() : "—")
                + " B:" + (evt.getFatigueB() != null ? evt.getFatigueB() : "—"));
        fatigueSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        var row = new HorizontalLayout(stepSpan, actorSpan, moveSpan, tsSpan, fatigueSpan);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setWidthFull();
        row.getStyle().set("gap", "var(--lumo-space-s)");

        var card = new Div(row);
        card.setWidthFull();
        card.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("cursor", "pointer");
        if (active) {
            card.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("border", "1px solid var(--lumo-primary-color-50pct)");
        } else {
            card.getStyle()
                    .set("background", "var(--lumo-base-color)")
                    .set("border", "1px solid var(--lumo-contrast-10pct)");
        }
        int step = evt.getStepIndex();
        card.addClickListener(e -> {
            currentStep = step;
            refreshStep();
        });
        return card;
    }
}
