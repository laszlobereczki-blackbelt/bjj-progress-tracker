package hu.blackbelt.matchreplay.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.ViewTitle;
import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchService;
import hu.blackbelt.matchreplay.ResultOutcome;
import hu.blackbelt.matchreplay.analysis.*;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("match-lab")
@PageTitle("Match Lab")
@Menu(order = 6, icon = "vaadin:film", title = "Match Lab")
@PermitAll
public class MatchReplayListView extends VerticalLayout {

    private final MatchService service;
    private final CrossMatchAnalyzer crossMatchAnalyzer;

    public MatchReplayListView(MatchService service, CrossMatchAnalyzer crossMatchAnalyzer) {
        this.service = service;
        this.crossMatchAnalyzer = crossMatchAnalyzer;
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var titleRow = new HorizontalLayout(new ViewTitle("Match Lab"));
        titleRow.setWidthFull();
        titleRow.setPadding(true);
        titleRow.getStyle().set("padding-bottom", "0");

        var tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        tabSheet.add("Matches", buildMatchesPanel());
        tabSheet.add("Insights", buildInsightsPanel());

        add(titleRow, tabSheet);
        setFlexGrow(1, tabSheet);
    }

    // ---- Matches panel --------------------------------------------------

    private Component buildMatchesPanel() {
        var layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        var grid = new Grid<>(Match.class, false);
        grid.addColumn(m -> dateFormatter.format(m.getMatchDate())).setHeader("Date").setWidth("9em").setFlexGrow(0);
        grid.addColumn(m -> m.getOpponentName() != null ? m.getOpponentName() : "—")
                .setHeader("Opponent").setFlexGrow(1);
        grid.addColumn(m -> m.getSelfRole() != null ? "Player " + m.getSelfRole() : "—")
                .setHeader("You are").setWidth("7em").setFlexGrow(0);
        grid.addColumn(m -> m.getResultOutcome() != null ? formatOutcome(m.getResultOutcome()) : "—")
                .setHeader("Result").setWidth("10em").setFlexGrow(0);
        grid.addColumn(m -> m.getDurationSeconds() != null ? m.getDurationSeconds() + "s" : "—")
                .setHeader("Duration").setWidth("7em").setFlexGrow(0);
        grid.addComponentColumn(match -> {
            var editBtn = new Button("Edit", e ->
                    getUI().ifPresent(ui -> ui.navigate(MatchEditorView.class, match.getId())));
            var playBtn = new Button("Play", e ->
                    getUI().ifPresent(ui -> ui.navigate(MatchPlaybackView.class, match.getId())));
            var analyzeBtn = new Button("Analyze", e ->
                    getUI().ifPresent(ui -> ui.navigate(MatchAnalysisView.class, match.getId())));
            var deleteBtn = new Button("Delete", e -> {
                service.delete(match.getId());
                grid.getDataProvider().refreshAll();
                Notification.show("Match deleted", 2000, Notification.Position.BOTTOM_END);
            });
            editBtn.addThemeVariants(ButtonVariant.SMALL);
            playBtn.addThemeVariants(ButtonVariant.SMALL);
            analyzeBtn.addThemeVariants(ButtonVariant.SMALL);
            deleteBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);
            return new HorizontalLayout(editBtn, playBtn, analyzeBtn, deleteBtn);
        }).setHeader("").setWidth("20em").setFlexGrow(0);

        grid.setItems(query -> service.list(toSpringPageRequest(query)).stream());
        grid.setEmptyStateText("No matches recorded yet. Click 'New Match' to start.");
        grid.setSizeFull();

        var newBtn = new Button("New Match", e -> openNewMatchDialog(grid));
        newBtn.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(newBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        layout.add(toolbar, grid);
        layout.setFlexGrow(1, grid);
        return layout;
    }

    // ---- Insights panel -------------------------------------------------

    private Component buildInsightsPanel() {
        var layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.getStyle().set("overflow-y", "auto");

        CrossMatchAnalysis analysis = crossMatchAnalyzer.analyze();

        var summary = new Span("Aggregated across " + analysis.totalMatches() + " match"
                + (analysis.totalMatches() == 1 ? "" : "es") + ".");
        summary.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        layout.add(summary);

        layout.add(buildTopMistakesSection(analysis.topMistakes()));
        layout.add(buildMovePreferencesSection(analysis.movePreferencesByOpponent()));
        layout.add(buildAvgFatigueSection(analysis.avgFatigueCurve()));

        return layout;
    }

    private Component buildTopMistakesSection(List<AggregatedMistake> mistakes) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.getStyle().set("gap", "var(--lumo-space-s)");
        section.add(new H3("Top Recurring Mistakes"));

        if (mistakes.isEmpty()) {
            var note = new Span("No catalog-matched mistakes found yet. Add more matches with roles and catalog moves to see patterns.");
            note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
            section.add(note);
            return section;
        }

        for (AggregatedMistake m : mistakes) {
            var card = new Div();
            String borderColor = m.severity() == MistakeSeverity.HIGH
                    ? "var(--lumo-error-color)"
                    : m.severity() == MistakeSeverity.MEDIUM
                    ? "var(--lumo-warning-color, #f0b429)"
                    : "var(--lumo-contrast-30pct)";
            card.getStyle()
                    .set("border-left", "4px solid " + borderColor)
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "0 var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0")
                    .set("padding", "var(--lumo-space-s) var(--lumo-space-m)");

            var countBadge = new Span("×" + m.count());
            countBadge.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)")
                    .set("color", borderColor).set("margin-right", "var(--lumo-space-s)");

            String posText = (m.fromPositionKey() != null && !m.fromPositionKey().isBlank())
                    ? " from " + m.fromPositionLabel() : "";
            var text = new Span("Opponent plays " + m.opponentMoveLabel() + posText
                    + " → you chose " + m.userMoveLabel()
                    + " (avg gap: " + String.format("%.1f", m.avgScoreGap()) + ")");
            text.getStyle().set("font-size", "var(--lumo-font-size-s)");

            var row = new HorizontalLayout(countBadge, text);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setPadding(false);
            card.add(row);
            section.add(card);
        }
        return section;
    }

    private Component buildMovePreferencesSection(List<OpponentMoveStats> stats) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.getStyle().set("gap", "var(--lumo-space-s)").set("margin-top", "var(--lumo-space-m)");
        section.add(new H3("Your Move Preferences by Opponent"));

        if (stats.isEmpty()) {
            var note = new Span("No opponent data yet. Set your role and opponent name on matches to see preferences.");
            note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
            section.add(note);
            return section;
        }

        Map<String, List<OpponentMoveStats>> byOpponent = stats.stream()
                .collect(Collectors.groupingBy(OpponentMoveStats::opponentName));

        int maxCount = stats.stream().mapToInt(OpponentMoveStats::count).max().orElse(1);

        var cols = new HorizontalLayout();
        cols.setWidthFull();
        cols.setAlignItems(FlexComponent.Alignment.START);
        cols.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-l)");

        for (var entry : byOpponent.entrySet()) {
            var block = new VerticalLayout();
            block.setPadding(false);
            block.setSpacing(false);
            block.getStyle().set("gap", "var(--lumo-space-xs)").set("min-width", "240px").set("flex", "1");
            var opponentHeader = new Span("vs " + entry.getKey());
            opponentHeader.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");
            block.add(opponentHeader);
            for (OpponentMoveStats s : entry.getValue()) {
                block.add(makeBar(s.userMoveLabel(), s.count(), maxCount, "#4fa3e0", String.valueOf(s.count())));
            }
            cols.add(block);
        }
        section.add(cols);
        return section;
    }

    private Component buildAvgFatigueSection(List<AveragedFatiguePoint> points) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.getStyle().set("gap", "var(--lumo-space-xs)").set("margin-top", "var(--lumo-space-m)");
        section.add(new H3("Average Fatigue Curve"));

        if (points.isEmpty()) {
            var note = new Span("No fatigue data yet. Record fatigue values in the editor to see averages here.");
            note.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");
            section.add(note);
            return section;
        }

        var header = new HorizontalLayout();
        header.getStyle().set("gap", "var(--lumo-space-m)").set("padding-bottom", "var(--lumo-space-xs)")
                .set("border-bottom", "1px solid var(--lumo-contrast-20pct)").set("width", "100%");
        header.add(styledSpan("Match %", "6em", true));
        header.add(styledSpan("Avg Fatigue A", "140px", true));
        header.add(styledSpan("Avg Fatigue B", "140px", true));
        header.add(styledSpan("Samples", "5em", true));
        section.add(header);

        for (AveragedFatiguePoint pt : points) {
            var row = new HorizontalLayout();
            row.getStyle().set("gap", "var(--lumo-space-m)").set("align-items", "center").set("width", "100%");
            row.add(styledSpan(pt.bucketPct() + "%", "6em", false));
            row.add(makeFatigueBar(pt.avgFatigueA(), "#4fa3e0"));
            row.add(makeFatigueBar(pt.avgFatigueB(), "#e07b4f"));
            row.add(styledSpan(String.valueOf(pt.sampleCount()), "5em", false));
            section.add(row);
        }
        return section;
    }

    // ---- Shared helpers --------------------------------------------------

    private String formatOutcome(ResultOutcome outcome) {
        return switch (outcome) {
            case WIN_SUBMISSION -> "Win (sub)";
            case WIN_POINTS -> "Win (pts)";
            case WIN_DQ -> "Win (DQ)";
            case DRAW -> "Draw";
            case LOSS_SUBMISSION -> "Loss (sub)";
            case LOSS_POINTS -> "Loss (pts)";
            case LOSS_DQ -> "Loss (DQ)";
        };
    }

    private Span styledSpan(String text, String width, boolean bold) {
        var s = new Span(text);
        s.getStyle().set("min-width", width).set("font-size", "var(--lumo-font-size-s)");
        if (bold) s.getStyle().set("font-weight", "bold").set("color", "var(--lumo-secondary-text-color)");
        return s;
    }

    private Component makeFatigueBar(double rawValue, String color) {
        var wrapper = new HorizontalLayout();
        wrapper.getStyle().set("width", "140px").set("align-items", "center").set("gap", "4px");

        if (rawValue < 0) {
            var na = new Span("—");
            na.getStyle().set("color", "var(--lumo-disabled-text-color)").set("font-size", "var(--lumo-font-size-s)");
            wrapper.add(na);
            return wrapper;
        }

        int pct = (int) Math.round(rawValue * 10); // 0–100 for 0–10 scale
        var track = new Div();
        track.getStyle().set("flex", "1").set("height", "8px").set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "4px").set("overflow", "hidden");
        var fill = new Div();
        fill.getStyle().set("width", pct + "%").set("height", "100%")
                .set("background", color).set("border-radius", "4px");
        track.add(fill);

        var label = new Span(String.format("%.1f", rawValue));
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("min-width", "2.5em")
                .set("text-align", "right");

        wrapper.add(track, label);
        return wrapper;
    }

    private Component makeBar(String label, int value, int max, String color, String suffix) {
        double pct = max > 0 ? (double) value / max * 100.0 : 0;

        var nameLabel = new Span(label);
        nameLabel.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("min-width", "10em").set("overflow", "hidden")
                .set("text-overflow", "ellipsis").set("white-space", "nowrap");

        var fill = new Div();
        fill.getStyle().set("width", pct + "%").set("height", "14px")
                .set("background", color).set("border-radius", "3px").set("min-width", "2px");

        var track = new Div(fill);
        track.getStyle().set("width", "100%").set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "3px").set("overflow", "hidden");

        var countLabel = new Span(suffix);
        countLabel.getStyle().set("min-width", "3em").set("text-align", "right")
                .set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

        var row = new HorizontalLayout(nameLabel, track, countLabel);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, track);
        return row;
    }

    private void openNewMatchDialog(Grid<Match> grid) {
        var dialog = new Dialog();
        dialog.setHeaderTitle("New Match");
        dialog.setWidth("400px");

        var datePicker = new DatePicker("Date");
        datePicker.setRequired(true);
        datePicker.setValue(LocalDate.now());

        var opponentField = new TextField("Opponent");
        opponentField.setPlaceholder("optional");

        var selfRoleGroup = new RadioButtonGroup<String>();
        selfRoleGroup.setLabel("You are playing as");
        selfRoleGroup.setItems("A", "B");
        selfRoleGroup.setValue("A");

        var outcomeCombo = new ComboBox<ResultOutcome>("Result");
        outcomeCombo.setItems(ResultOutcome.values());
        outcomeCombo.setItemLabelGenerator(this::formatOutcome);
        outcomeCombo.setPlaceholder("optional");
        outcomeCombo.setWidthFull();

        var durationField = new IntegerField("Duration (seconds)");
        durationField.setPlaceholder("optional");
        durationField.setMin(0);
        durationField.setWidthFull();

        var layout = new VerticalLayout(datePicker, opponentField, selfRoleGroup, outcomeCombo, durationField);
        layout.setPadding(false);
        dialog.add(layout);

        var createBtn = new Button("Create & Edit", e -> {
            if (datePicker.isEmpty()) {
                Notification.show("Please select a date", 2000, Notification.Position.MIDDLE);
                return;
            }
            var match = new Match(datePicker.getValue());
            match.setOpponentName(opponentField.getValue().isBlank() ? null : opponentField.getValue());
            match.setSelfRole(selfRoleGroup.getValue());
            match.setResultOutcome(outcomeCombo.getValue());
            match.setDurationSeconds(durationField.getValue());
            var saved = service.save(match);
            dialog.close();
            Notification.show("Match created", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.SUCCESS);
            getUI().ifPresent(ui -> ui.navigate(MatchEditorView.class, saved.getId()));
        });
        createBtn.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), createBtn);
        dialog.open();
    }
}
