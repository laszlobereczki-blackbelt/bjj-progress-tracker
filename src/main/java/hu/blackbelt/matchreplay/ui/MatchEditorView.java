package hu.blackbelt.matchreplay.ui;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchEvent;
import hu.blackbelt.matchreplay.MatchExportService;
import hu.blackbelt.matchreplay.MatchService;
import hu.blackbelt.matchreplay.catalog.MoveCatalogLoader;
import hu.blackbelt.matchreplay.catalog.MoveCatalogSnapshot;
import jakarta.annotation.security.PermitAll;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

@Route("match-lab/editor")
@PageTitle("Match Editor")
@PermitAll
public class MatchEditorView extends VerticalLayout implements HasUrlParameter<Long> {

    private final MatchService service;
    private final MoveCatalogLoader catalogLoader;
    private final MatchExportService exportService;
    private Match match;

    private final Span matchHeader = new Span();
    private final VerticalLayout timeline = new VerticalLayout();
    private final RadioButtonGroup<String> actorGroup = new RadioButtonGroup<>();
    private final ComboBox<String> moveCombo = new ComboBox<>();
    private final Span catalogWarning = new Span();
    private final HorizontalLayout quickTransitionRow = new HorizontalLayout();
    private final HorizontalLayout recentMovesRow = new HorizontalLayout();
    private final TextField positionAfterField = new TextField();
    private final TextField noteField = new TextField();
    private final IntegerField timestampField = new IntegerField();
    private final IntegerField fatigueAField = new IntegerField();
    private final IntegerField fatigueBField = new IntegerField();

    // Keyboard shortcut support
    private final List<String> currentTransitionKeys = new ArrayList<>();
    private final List<ShortcutRegistration> shortcutRegistrations = new ArrayList<>();

    public MatchEditorView(MatchService service, MoveCatalogLoader catalogLoader, MatchExportService exportService) {
        this.service = service;
        this.catalogLoader = catalogLoader;
        this.exportService = exportService;
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
        buildUi();
    }

    private void buildUi() {
        // Clear any previously registered shortcuts before rebuilding
        shortcutRegistrations.forEach(ShortcutRegistration::remove);
        shortcutRegistrations.clear();

        removeAll();

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        String headerText = dateFormatter.format(match.getMatchDate())
                + (match.getOpponentName() != null ? " vs " + match.getOpponentName() : "");
        if (match.getSelfRole() != null) {
            headerText += " (you: " + match.getSelfRole() + ")";
        }
        matchHeader.setText(headerText);
        matchHeader.getStyle().set("font-size", "var(--lumo-font-size-l)").set("font-weight", "bold");

        var backBtn = new Button("← Back to list", e -> UI.getCurrent().navigate(MatchReplayListView.class));
        backBtn.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);

        var playBtn = new Button("▶ Playback", e -> UI.getCurrent().navigate(MatchPlaybackView.class, match.getId()));
        playBtn.addThemeVariants(ButtonVariant.SMALL);

        var analyzeBtn = new Button("Analyze", e -> UI.getCurrent().navigate(MatchAnalysisView.class, match.getId()));
        analyzeBtn.addThemeVariants(ButtonVariant.SMALL);

        // Export button — Anchor wraps the button so the browser triggers a file download
        String exportFilename = "match-" + (match.getId() != null ? match.getId() : "export") + ".json";
        var exportAnchor = new Anchor((DownloadEvent event) -> {
            event.setFileName(exportFilename);
            String json = exportService.export(match);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (var out = event.getOutputStream()) {
                out.write(bytes);
            }
        }, "");
        exportAnchor.setDownload(true);
        exportAnchor.getElement().getStyle().set("text-decoration", "none");
        var exportBtn = new Button("Export JSON");
        exportBtn.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        exportAnchor.add(exportBtn);

        var headerRow = new HorizontalLayout(backBtn, matchHeader, playBtn, analyzeBtn, exportAnchor);
        headerRow.setWidthFull();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setFlexGrow(1, matchHeader);

        timeline.setPadding(false);
        timeline.setSpacing(false);
        timeline.getStyle().set("gap", "4px");

        // Move combo — searchable, allows custom (out-of-catalog) values
        moveCombo.setLabel("Move");
        moveCombo.setPlaceholder("Search or type a move...");
        moveCombo.setWidth("280px");
        moveCombo.setAllowCustomValue(true);
        moveCombo.setItems(
                query -> catalogLoader.getSnapshot()
                        .search(query.getFilter().orElse("")).stream()
                        .map(m -> m.key())
                        .skip(query.getOffset())
                        .limit(query.getLimit()),
                query -> catalogLoader.getSnapshot()
                        .search(query.getFilter().orElse("")).size());
        moveCombo.setItemLabelGenerator(key -> catalogLoader.getSnapshot()
                .findMove(key).map(m -> m.label()).orElse(key));
        moveCombo.addCustomValueSetListener(e -> {
            moveCombo.setValue(e.getDetail());
            updateCatalogWarning();
            autoFillPositionAfter();
        });
        moveCombo.addValueChangeListener(e -> {
            updateCatalogWarning();
            autoFillPositionAfter();
        });

        // Advisory warning — shown when the move is not in the catalog
        catalogWarning.getStyle()
                .set("color", "var(--lumo-warning-text-color)")
                .set("font-size", "var(--lumo-font-size-xs)");
        catalogWarning.setVisible(false);

        // Quick transition chips (refreshed after each event)
        quickTransitionRow.setPadding(false);
        quickTransitionRow.setSpacing(false);
        quickTransitionRow.getStyle().set("gap", "var(--lumo-space-xs)").set("flex-wrap", "wrap");

        // Recent moves chips (refreshed after each event)
        recentMovesRow.setPadding(false);
        recentMovesRow.setSpacing(false);
        recentMovesRow.getStyle().set("gap", "var(--lumo-space-xs)").set("flex-wrap", "wrap");

        actorGroup.setLabel("Actor");
        actorGroup.setItems("A", "B");
        actorGroup.setValue(match.getSelfRole() != null ? match.getSelfRole() : "A");

        positionAfterField.setLabel("Position after");
        positionAfterField.setPlaceholder("e.g. side_control_top_A");
        positionAfterField.setWidth("220px");

        timestampField.setLabel("Time (s)");
        timestampField.setMin(0);
        timestampField.setValue(0);
        timestampField.setWidth("80px");

        fatigueAField.setLabel("Fatigue A (0-10)");
        fatigueAField.setMin(0);
        fatigueAField.setMax(10);
        fatigueAField.setValue(0);
        fatigueAField.setWidth("110px");
        fatigueAField.setStepButtonsVisible(true);

        fatigueBField.setLabel("Fatigue B (0-10)");
        fatigueBField.setMin(0);
        fatigueBField.setMax(10);
        fatigueBField.setValue(0);
        fatigueBField.setWidth("110px");
        fatigueBField.setStepButtonsVisible(true);

        noteField.setLabel("Note");
        noteField.setPlaceholder("optional");
        noteField.setWidth("200px");

        var addBtn = new Button("Add Event", e -> addEvent());
        addBtn.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);

        var undoBtn = new Button("Undo Last", e -> undoLast());
        undoBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);

        var btnRow = new HorizontalLayout(addBtn, undoBtn);
        btnRow.setAlignItems(FlexComponent.Alignment.BASELINE);

        var shortcutHint = new Span("Shortcuts: 1-9 = quick transitions  ·  U = undo");
        shortcutHint.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        var moveBlock = new VerticalLayout(moveCombo, catalogWarning, quickTransitionRow, recentMovesRow);
        moveBlock.setPadding(false);
        moveBlock.setSpacing(false);
        moveBlock.getStyle().set("gap", "var(--lumo-space-xs)");

        var inputBar = new VerticalLayout();
        inputBar.setPadding(true);
        inputBar.setSpacing(false);
        inputBar.getStyle()
                .set("gap", "var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        var row1 = new HorizontalLayout(actorGroup, moveBlock, positionAfterField, timestampField);
        row1.setAlignItems(FlexComponent.Alignment.START);
        row1.getStyle().set("flex-wrap", "wrap");

        var row2 = new HorizontalLayout(fatigueAField, fatigueBField, noteField, btnRow);
        row2.setAlignItems(FlexComponent.Alignment.BASELINE);
        row2.getStyle().set("flex-wrap", "wrap");

        inputBar.add(row1, row2, shortcutHint);

        refreshTimeline();

        add(headerRow, timeline, inputBar);
        setFlexGrow(1, timeline);

        // Register keyboard shortcuts (only fire when no input field is focused)
        Key[] digitKeys = {
            Key.DIGIT_1, Key.DIGIT_2, Key.DIGIT_3, Key.DIGIT_4, Key.DIGIT_5,
            Key.DIGIT_6, Key.DIGIT_7, Key.DIGIT_8, Key.DIGIT_9
        };
        for (int i = 0; i < digitKeys.length; i++) {
            final int idx = i;
            shortcutRegistrations.add(
                Shortcuts.addShortcutListener(this, () -> selectQuickTransition(idx), digitKeys[i])
            );
        }
        shortcutRegistrations.add(
            Shortcuts.addShortcutListener(this, this::undoLast, Key.KEY_U)
        );
    }

    private void refreshTimeline() {
        timeline.removeAll();
        List<MatchEvent> events = match.getEvents();
        if (events.isEmpty()) {
            var empty = new Span("No events yet. Add the first move below.");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)").set("padding", "var(--lumo-space-m)");
            timeline.add(empty);
        } else {
            for (MatchEvent evt : events) {
                timeline.add(buildEventCard(evt));
            }
            // Sticky fatigue: seed sliders from last event
            MatchEvent last = events.get(events.size() - 1);
            if (last.getFatigueA() != null) fatigueAField.setValue(last.getFatigueA());
            if (last.getFatigueB() != null) fatigueBField.setValue(last.getFatigueB());
        }
        refreshQuickTransitions();
        refreshRecentMoves();
    }

    private void refreshQuickTransitions() {
        currentTransitionKeys.clear();
        quickTransitionRow.removeAll();
        String currentPos = getCurrentPosition();
        if (currentPos == null || currentPos.isBlank()) return;

        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();
        List<?> transitions = catalog.transitionsFrom(currentPos);
        if (transitions.isEmpty()) return;

        var label = new Span("Quick:");
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("align-self", "center");
        quickTransitionRow.add(label);

        int chipNum = 0;
        for (var t : catalog.transitionsFrom(currentPos)) {
            String moveKey = t.move();
            currentTransitionKeys.add(moveKey);
            chipNum++;
            String moveLabel = catalog.findMove(moveKey).map(m -> m.label()).orElse(moveKey);
            String chipLabel = chipNum <= 9 ? chipNum + ". " + moveLabel : moveLabel;
            var chip = new Button(chipLabel, e -> {
                moveCombo.setValue(moveKey);
                if (positionAfterField.isEmpty()) positionAfterField.setValue(t.toPosition());
                updateCatalogWarning();
            });
            chip.addThemeVariants(ButtonVariant.SMALL);
            chip.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            quickTransitionRow.add(chip);
        }
    }

    private void refreshRecentMoves() {
        recentMovesRow.removeAll();
        List<MatchEvent> events = match.getEvents();
        if (events.isEmpty()) return;

        // Last 5 distinct move keys, most-recent first
        List<String> recent = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && recent.size() < 5; i--) {
            String key = events.get(i).getMoveKey();
            if (!recent.contains(key)) recent.add(key);
        }
        if (recent.isEmpty()) return;

        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();
        var label = new Span("Recent:");
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("align-self", "center");
        recentMovesRow.add(label);

        for (String moveKey : recent) {
            String moveLabel = catalog.findMove(moveKey).map(m -> m.label()).orElse(moveKey);
            var chip = new Button(moveLabel, e -> {
                moveCombo.setValue(moveKey);
                updateCatalogWarning();
            });
            chip.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            chip.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            recentMovesRow.add(chip);
        }
    }

    private void selectQuickTransition(int idx) {
        if (idx >= currentTransitionKeys.size()) return;
        String moveKey = currentTransitionKeys.get(idx);
        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();
        moveCombo.setValue(moveKey);
        updateCatalogWarning();
        String currentPos = getCurrentPosition();
        if (currentPos != null && positionAfterField.isEmpty()) {
            catalog.transitionsFrom(currentPos).stream()
                    .filter(t -> t.move().equals(moveKey))
                    .findFirst()
                    .ifPresent(t -> positionAfterField.setValue(t.toPosition()));
        }
    }

    private void updateCatalogWarning() {
        String key = moveCombo.getValue();
        if (key == null || key.isBlank()) {
            catalogWarning.setVisible(false);
            return;
        }
        boolean known = catalogLoader.getSnapshot().isMoveKnown(key);
        catalogWarning.setText("⚠ Not in catalog — will be saved as-is");
        catalogWarning.setVisible(!known);
    }

    /** If the chosen move has exactly one transition from the current position, auto-fill positionAfter. */
    private void autoFillPositionAfter() {
        if (!positionAfterField.isEmpty()) return;
        String moveKey = moveCombo.getValue();
        String currentPos = getCurrentPosition();
        if (moveKey == null || currentPos == null) return;

        var matching = catalogLoader.getSnapshot().transitionsFrom(currentPos).stream()
                .filter(t -> t.move().equals(moveKey))
                .toList();
        if (matching.size() == 1) {
            positionAfterField.setValue(matching.get(0).toPosition());
        }
    }

    /** The position we are currently in = positionAfter of the last event. */
    private String getCurrentPosition() {
        List<MatchEvent> events = match.getEvents();
        if (events.isEmpty()) return null;
        return events.get(events.size() - 1).getPositionAfter();
    }

    private Div buildEventCard(MatchEvent evt) {
        var stepBadge = new Span("#" + (evt.getStepIndex() + 1));
        stepBadge.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("min-width", "2.5em");

        var actorBadge = new Span(evt.getActor());
        actorBadge.getStyle()
                .set("font-weight", "bold")
                .set("color", "A".equals(evt.getActor()) ? "var(--lumo-primary-color)" : "var(--lumo-error-color)")
                .set("min-width", "1.5em");

        MoveCatalogSnapshot catalog = catalogLoader.getSnapshot();
        String moveLabel = catalog.findMove(evt.getMoveKey())
                .map(m -> m.label()).orElse(evt.getMoveKey());
        var moveSpan = new Span(moveLabel);
        moveSpan.getStyle().set("flex-grow", "1");

        var tsLabel = new Span(evt.getTimestampSeconds() + "s");
        tsLabel.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xs)");

        var fatigueBadge = new Span(
                "A:" + (evt.getFatigueA() != null ? evt.getFatigueA() : "—")
                + " B:" + (evt.getFatigueB() != null ? evt.getFatigueB() : "—"));
        fatigueBadge.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        var row = new HorizontalLayout(stepBadge, actorBadge, moveSpan, tsLabel, fatigueBadge);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setWidthFull();
        row.getStyle().set("gap", "var(--lumo-space-s)");

        if (evt.getPositionAfter() != null) {
            String posLabel = catalog.positions().stream()
                    .filter(p -> p.key().equals(evt.getPositionAfter()))
                    .map(p -> p.label())
                    .findFirst()
                    .orElse(evt.getPositionAfter());
            var posSpan = new Span("→ " + posLabel);
            posSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-style", "italic");
            row.add(posSpan);
        }

        var card = new Div(row);
        card.setWidthFull();
        card.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)");
        return card;
    }

    private void addEvent() {
        String moveKey = moveCombo.getValue();
        if (moveKey == null || moveKey.isBlank()) {
            Notification.show("Enter or select a move", 2000, Notification.Position.MIDDLE);
            return;
        }
        match = service.appendEvent(
                match.getId(),
                actorGroup.getValue(),
                moveKey.trim(),
                timestampField.getValue() != null ? timestampField.getValue() : 0,
                positionAfterField.getValue(),
                fatigueAField.getValue(),
                fatigueBField.getValue(),
                noteField.getValue());
        moveCombo.clear();
        positionAfterField.clear();
        noteField.clear();
        catalogWarning.setVisible(false);
        // Alternate actor for convenience
        actorGroup.setValue("A".equals(actorGroup.getValue()) ? "B" : "A");
        refreshTimeline();
        Notification.show("Event added", 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.SUCCESS);
    }

    private void undoLast() {
        if (match.getEvents().isEmpty()) {
            Notification.show("Nothing to undo", 1500, Notification.Position.BOTTOM_END);
            return;
        }
        match = service.removeLastEvent(match.getId());
        refreshTimeline();
        Notification.show("Last event removed", 1500, Notification.Position.BOTTOM_END);
    }
}
