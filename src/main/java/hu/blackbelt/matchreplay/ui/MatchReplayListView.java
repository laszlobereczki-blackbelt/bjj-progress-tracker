package hu.blackbelt.matchreplay.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.ViewTitle;
import hu.blackbelt.matchreplay.Match;
import hu.blackbelt.matchreplay.MatchService;
import hu.blackbelt.matchreplay.ResultOutcome;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("match-lab")
@PageTitle("Match Lab")
@Menu(order = 6, icon = "vaadin:film", title = "Match Lab")
@PermitAll
public class MatchReplayListView extends VerticalLayout {

    private final MatchService service;
    private final Grid<Match> grid;

    public MatchReplayListView(MatchService service) {
        this.service = service;
        setSizeFull();

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        grid = new Grid<>(Match.class, false);
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
            var deleteBtn = new Button("Delete", e -> {
                service.delete(match.getId());
                grid.getDataProvider().refreshAll();
                Notification.show("Match deleted", 2000, Notification.Position.BOTTOM_END);
            });
            editBtn.addThemeVariants(ButtonVariant.SMALL);
            playBtn.addThemeVariants(ButtonVariant.SMALL);
            deleteBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);
            return new HorizontalLayout(editBtn, playBtn, deleteBtn);
        }).setHeader("").setWidth("14em").setFlexGrow(0);

        grid.setItems(query -> service.list(toSpringPageRequest(query)).stream());
        grid.setEmptyStateText("No matches recorded yet. Click 'New Match' to start.");
        grid.setSizeFull();

        var newBtn = new Button("New Match", e -> openNewMatchDialog());
        newBtn.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(new ViewTitle("Match Lab"), newBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, toolbar.getComponentAt(0));

        add(toolbar, grid);
    }

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

    private void openNewMatchDialog() {
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
