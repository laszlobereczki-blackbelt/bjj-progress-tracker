package hu.blackbelt.matchreplay.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.ViewTitle;
import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchService;
import hu.blackbelt.matchreplay.analysis.*;
import hu.blackbelt.matchreplay.catalog.MoveCatalogLoader;
import hu.blackbelt.matchreplay.catalog.MoveCatalogSnapshot;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.stream.Collectors;

@Route("match-lab/analysis")
@PageTitle("Match Analysis")
@PermitAll
public class MatchAnalysisView extends VerticalLayout implements HasUrlParameter<Long> {

    private final MatchService matchService;
    private final MatchAnalyzer analyzer;
    private final RecommendationEngine recommendationEngine;
    private final MoveCatalogLoader catalogLoader;

    public MatchAnalysisView(MatchService matchService, MatchAnalyzer analyzer,
                             RecommendationEngine recommendationEngine, MoveCatalogLoader catalogLoader) {
        this.matchService = matchService;
        this.analyzer = analyzer;
        this.recommendationEngine = recommendationEngine;
        this.catalogLoader = catalogLoader;
        setSizeFull();
        setPadding(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long matchId) {
        if (matchId == null) {
            UI.getCurrent().navigate(MatchReplayListView.class);
            return;
        }
        Match match = matchService.findByIdWithEvents(matchId).orElse(null);
        if (match == null) {
            UI.getCurrent().navigate(MatchReplayListView.class);
            return;
        }
        buildUi(match);
    }

    private void buildUi(Match match) {
        removeAll();

        var backBtn = new Button("← Back to list", e -> UI.getCurrent().navigate(MatchReplayListView.class));
        backBtn.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);

        var editBtn = new Button("Edit", e -> UI.getCurrent().navigate(MatchEditorView.class, match.getId()));
        editBtn.addThemeVariants(ButtonVariant.SMALL);

        var toolbar = new HorizontalLayout(new ViewTitle("Analysis"), backBtn, editBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, toolbar.getComponentAt(0));

        if (match.getEvents().isEmpty()) {
            var empty = new Span("No events recorded yet. Add some moves in the editor first.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)").set("padding", "var(--lumo-space-m)");
            add(toolbar, empty);
            return;
        }

        MatchAnalysis analysis = analyzer.analyze(match);
        List<MistakeFinding> mistakes = recommendationEngine.analyze(match);
        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();

        var tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.add("Frequencies", buildFrequenciesPanel(analysis, catalog));
        tabSheet.add("Patterns", buildPatternsPanel(analysis, catalog));
        tabSheet.add("Fatigue", buildFatiguePanel(analysis));
        tabSheet.add("Feedback", buildFeedbackPanel(mistakes, match));

        add(toolbar, tabSheet);
        setFlexGrow(1, tabSheet);
    }

    // ---- Frequencies panel -----------------------------------------------

    private Component buildFrequenciesPanel(MatchAnalysis analysis, MoveCatalogSnapshot catalog) {
        var layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);

        if (analysis.frequenciesA().isEmpty() && analysis.frequenciesB().isEmpty()
                && analysis.positionTimes().isEmpty()) {
            layout.add(new Span("Not enough data."));
            return layout;
        }

        var cols = new HorizontalLayout();
        cols.setWidthFull();
        cols.setAlignItems(FlexComponent.Alignment.START);
        cols.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

        if (!analysis.frequenciesA().isEmpty()) {
            cols.add(buildFrequencyBlock("Player A moves", analysis.frequenciesA(), catalog, "#4fa3e0"));
        }
        if (!analysis.frequenciesB().isEmpty()) {
            cols.add(buildFrequencyBlock("Player B moves", analysis.frequenciesB(), catalog, "#e07b4f"));
        }

        layout.add(cols);

        if (!analysis.positionTimes().isEmpty()) {
            layout.add(buildPositionBlock(analysis.positionTimes(), catalog));
        }

        return layout;
    }

    private Component buildFrequencyBlock(String title, List<MoveFrequency> freqs,
                                           MoveCatalogSnapshot catalog, String color) {
        var block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle().set("gap", "var(--lumo-space-xs)").set("min-width", "280px").set("flex", "1");

        block.add(new H3(title));

        int maxCount = freqs.stream().mapToInt(MoveFrequency::count).max().orElse(1);
        for (MoveFrequency f : freqs) {
            String label = catalog.findMove(f.moveKey()).map(m -> m.label()).orElse(f.moveKey());
            block.add(makeBar(label, f.count(), maxCount, color,
                    Math.round(f.fraction() * 100) + "%"));
        }
        return block;
    }

    private Component buildPositionBlock(List<PositionTime> positionTimes, MoveCatalogSnapshot catalog) {
        var block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle().set("gap", "var(--lumo-space-xs)").set("margin-top", "var(--lumo-space-m)");
        block.add(new H3("Time in position (steps)"));

        int maxSteps = positionTimes.stream().mapToInt(PositionTime::steps).max().orElse(1);
        for (PositionTime pt : positionTimes) {
            String label = catalog.positions().stream()
                    .filter(p -> p.key().equals(pt.positionKey()))
                    .map(p -> p.label())
                    .findFirst()
                    .orElse(pt.positionKey());
            block.add(makeBar(label, pt.steps(), maxSteps, "#6dbf67", pt.steps() + " steps"));
        }
        return block;
    }

    // ---- Patterns panel --------------------------------------------------

    private Component buildPatternsPanel(MatchAnalysis analysis, MoveCatalogSnapshot catalog) {
        var layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);

        if (analysis.patterns().isEmpty()) {
            var note = new Span("No repeated sequences found. Patterns appear when the same 2- or 3-move sequence occurs at least twice.");
            note.getStyle().set("color", "var(--lumo-secondary-text-color)");
            layout.add(note);
            return layout;
        }

        List<PatternOccurrence> patternsA = analysis.patterns().stream()
                .filter(p -> "A".equals(p.actor())).toList();
        List<PatternOccurrence> patternsB = analysis.patterns().stream()
                .filter(p -> "B".equals(p.actor())).toList();

        var cols = new HorizontalLayout();
        cols.setWidthFull();
        cols.setAlignItems(FlexComponent.Alignment.START);
        cols.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

        if (!patternsA.isEmpty()) {
            cols.add(buildPatternBlock("Player A patterns", patternsA, catalog));
        }
        if (!patternsB.isEmpty()) {
            cols.add(buildPatternBlock("Player B patterns", patternsB, catalog));
        }
        layout.add(cols);
        return layout;
    }

    private Component buildPatternBlock(String title, List<PatternOccurrence> patterns,
                                         MoveCatalogSnapshot catalog) {
        var block = new VerticalLayout();
        block.setPadding(false);
        block.setSpacing(false);
        block.getStyle().set("gap", "var(--lumo-space-s)").set("min-width", "280px").set("flex", "1");
        block.add(new H3(title));

        for (PatternOccurrence p : patterns) {
            var card = new Div();
            card.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "var(--lumo-space-s) var(--lumo-space-m)");

            var moves = p.sequence().stream()
                    .map(key -> catalog.findMove(key).map(m -> m.label()).orElse(key))
                    .toList();
            var sequence = new Span(String.join(" → ", moves));
            sequence.getStyle().set("font-size", "var(--lumo-font-size-s)");

            var badge = new Span("×" + p.occurrences());
            badge.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("font-weight", "bold")
                    .set("margin-left", "var(--lumo-space-s)");

            var row = new HorizontalLayout(sequence, badge);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            card.add(row);
            block.add(card);
        }
        return block;
    }

    // ---- Fatigue panel ---------------------------------------------------

    private Component buildFatiguePanel(MatchAnalysis analysis) {
        var layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);

        if (analysis.fatigueSeries().isEmpty()) {
            var note = new Span("No fatigue data recorded. Use the fatigue fields in the editor to track player fatigue per event.");
            note.getStyle().set("color", "var(--lumo-secondary-text-color)");
            layout.add(note);
            return layout;
        }

        var header = new HorizontalLayout();
        header.getStyle().set("gap", "var(--lumo-space-m)").set("padding-bottom", "var(--lumo-space-xs)")
                .set("border-bottom", "1px solid var(--lumo-contrast-20pct)").set("width", "100%");
        header.add(styledSpan("Step", "4em", true));
        header.add(styledSpan("Time (s)", "5em", true));
        header.add(styledSpan("Fatigue A", "120px", true));
        header.add(styledSpan("Fatigue B", "120px", true));
        layout.add(header);

        for (FatiguePoint pt : analysis.fatigueSeries()) {
            var row = new HorizontalLayout();
            row.getStyle().set("gap", "var(--lumo-space-m)").set("align-items", "center").set("width", "100%");
            row.add(styledSpan(String.valueOf(pt.stepIndex() + 1), "4em", false));
            row.add(styledSpan(pt.timestampSeconds() + "s", "5em", false));
            row.add(makeFatigueBar(pt.fatigueA(), "#4fa3e0"));
            row.add(makeFatigueBar(pt.fatigueB(), "#e07b4f"));
            layout.add(row);
        }

        return layout;
    }

    private Span styledSpan(String text, String width, boolean bold) {
        var s = new Span(text);
        s.getStyle().set("min-width", width).set("font-size", "var(--lumo-font-size-s)");
        if (bold) s.getStyle().set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
        return s;
    }

    private Component makeFatigueBar(Integer value, String color) {
        var wrapper = new HorizontalLayout();
        wrapper.getStyle().set("width", "120px").set("align-items", "center").set("gap", "4px");

        if (value == null) {
            var na = new Span("—");
            na.getStyle().set("color", "var(--lumo-disabled-text-color)").set("font-size", "var(--lumo-font-size-s)");
            wrapper.add(na);
            return wrapper;
        }

        var track = new Div();
        track.getStyle().set("flex", "1").set("height", "8px").set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "4px").set("overflow", "hidden");
        var fill = new Div();
        fill.getStyle().set("width", (value * 10) + "%").set("height", "100%")
                .set("background", color).set("border-radius", "4px");
        track.add(fill);

        var label = new Span(String.valueOf(value));
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("min-width", "1.5em")
                .set("text-align", "right");

        wrapper.add(track, label);
        return wrapper;
    }

    // ---- Feedback panel --------------------------------------------------

    private Component buildFeedbackPanel(List<MistakeFinding> mistakes, Match match) {
        var layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(false);
        layout.getStyle().set("gap", "var(--lumo-space-s)");

        String selfRole = match.getSelfRole();
        if (selfRole == null) {
            var note = new Span("Set your role (A or B) on the match to enable recommendation feedback.");
            note.getStyle().set("color", "var(--lumo-secondary-text-color)");
            layout.add(note);
            return layout;
        }

        List<MistakeFinding> highAndMedium = mistakes.stream()
                .filter(m -> m.severity() != MistakeSeverity.LOW)
                .collect(Collectors.toList());
        List<MistakeFinding> low = mistakes.stream()
                .filter(m -> m.severity() == MistakeSeverity.LOW)
                .collect(Collectors.toList());

        if (highAndMedium.isEmpty() && low.isEmpty()) {
            var ok = new Span("No catalog feedback available. Either no catalog recommendations match the moves in this match, or all your choices were aligned with the top recommendations.");
            ok.getStyle().set("color", "var(--lumo-secondary-text-color)");
            layout.add(ok);
            return layout;
        }

        if (!highAndMedium.isEmpty()) {
            layout.add(new H3("Mistakes"));
            for (MistakeFinding f : highAndMedium) {
                layout.add(buildMistakeCard(f));
            }
        }

        if (!low.isEmpty()) {
            var toggle = new Button("Show minor deviations (" + low.size() + ")");
            toggle.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            var lowSection = new VerticalLayout();
            lowSection.setPadding(false);
            lowSection.setSpacing(false);
            lowSection.getStyle().set("gap", "var(--lumo-space-s)");
            lowSection.setVisible(false);
            for (MistakeFinding f : low) {
                lowSection.add(buildMistakeCard(f));
            }
            toggle.addClickListener(e -> {
                boolean visible = !lowSection.isVisible();
                lowSection.setVisible(visible);
                toggle.setText(visible
                        ? "Hide minor deviations (" + low.size() + ")"
                        : "Show minor deviations (" + low.size() + ")");
            });
            layout.add(toggle, lowSection);
        }

        return layout;
    }

    private Component buildMistakeCard(MistakeFinding f) {
        var card = new Div();
        String borderColor = f.severity() == MistakeSeverity.HIGH
                ? "var(--lumo-error-color)"
                : f.severity() == MistakeSeverity.MEDIUM
                ? "var(--lumo-warning-color, #f0b429)"
                : "var(--lumo-contrast-30pct)";
        card.getStyle()
                .set("border-left", "4px solid " + borderColor)
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "0 var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-xs)");

        // Step line
        var stepBadge = new Span("Step " + (f.stepIndex() + 1));
        stepBadge.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "bold")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.05em");

        var severityBadge = new Span(f.severity().name());
        severityBadge.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "bold")
                .set("color", borderColor)
                .set("margin-left", "var(--lumo-space-s)");

        var headerRow = new HorizontalLayout(stepBadge, severityBadge);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setPadding(false);
        headerRow.setSpacing(false);
        headerRow.getStyle().set("gap", "0");

        // Situation description
        String positionText = (f.fromPositionKey() != null && !f.fromPositionKey().isBlank())
                ? " from " + f.fromPositionLabel()
                : "";
        var situation = new Span("Opponent played " + f.opponentMoveLabel() + positionText
                + ". You chose " + f.userMoveLabel()
                + " (score " + f.chosenScore() + ").");
        situation.getStyle().set("font-size", "var(--lumo-font-size-s)").set("display", "block")
                .set("margin-top", "2px");

        // Recommendation line
        var recommendation = new Span("Top recommendation: " + f.topMoveLabel()
                + " (score " + f.topScore() + ")");
        recommendation.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("display", "block")
                .set("color", "var(--lumo-primary-text-color)")
                .set("margin-top", "2px");

        card.add(headerRow, situation, recommendation);

        // Reason from catalog
        if (f.topReason() != null && !f.topReason().isBlank()) {
            var reason = new Span(f.topReason());
            reason.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("display", "block")
                    .set("margin-top", "2px");
            card.add(reason);
        }

        return card;
    }

    // ---- Shared bar builder ----------------------------------------------

    private Component makeBar(String label, int value, int max, String color, String suffix) {
        double pct = max > 0 ? (double) value / max * 100.0 : 0;

        var nameLabel = new Span(label);
        nameLabel.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("min-width", "12em").set("overflow", "hidden")
                .set("text-overflow", "ellipsis").set("white-space", "nowrap");

        var fill = new Div();
        fill.getStyle().set("width", pct + "%").set("height", "16px")
                .set("background", color).set("border-radius", "3px").set("min-width", "2px");

        var track = new Div(fill);
        track.getStyle().set("width", "100%").set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "3px").set("overflow", "hidden");

        var countLabel = new Span(suffix);
        countLabel.getStyle().set("min-width", "4em").set("text-align", "right")
                .set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

        var row = new HorizontalLayout(nameLabel, track, countLabel);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, track);
        return row;
    }
}
