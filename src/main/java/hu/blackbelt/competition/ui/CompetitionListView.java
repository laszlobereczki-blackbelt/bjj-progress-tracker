package hu.blackbelt.competition.ui;

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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.ViewTitle;
import hu.blackbelt.belt.Belt;
import hu.blackbelt.competition.Competition;
import hu.blackbelt.competition.CompetitionResult;
import hu.blackbelt.competition.CompetitionService;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("competitions")
@PageTitle("Competitions")
@Menu(order = 3, icon = "vaadin:records", title = "Competitions")
@PermitAll
public class CompetitionListView extends VerticalLayout {

    private final CompetitionService service;
    private final Grid<Competition> grid;

    public CompetitionListView(CompetitionService service) {
        this.service = service;
        setSizeFull();

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        grid = new Grid<>(Competition.class, false);
        grid.addColumn(c -> dateFormatter.format(c.getDate())).setHeader("Date").setWidth("8em").setFlexGrow(0);
        grid.addColumn(Competition::getName).setHeader("Name").setFlexGrow(2);
        grid.addColumn(Competition::getLocation).setHeader("Location").setFlexGrow(1);
        grid.addColumn(c -> Optional.ofNullable(c.getDivision()).map(Belt::name).orElse(""))
                .setHeader("Division").setWidth("7em").setFlexGrow(0);
        grid.addColumn(Competition::getWeightClass).setHeader("Weight").setWidth("7em").setFlexGrow(0);
        grid.addColumn(c -> Optional.ofNullable(c.getResult()).map(CompetitionResult::name).orElse(""))
                .setHeader("Result").setWidth("8em").setFlexGrow(0);
        grid.addColumn(c -> c.getMatchesWon() + "W / " + c.getMatchesLost() + "L")
                .setHeader("W/L").setWidth("6em").setFlexGrow(0);
        grid.addComponentColumn(competition -> {
            var editBtn = new Button("Edit", e -> openDialog(competition));
            var deleteBtn = new Button("Delete", e -> {
                service.delete(competition.getId());
                grid.getDataProvider().refreshAll();
                Notification.show("Competition deleted", 2000, Notification.Position.BOTTOM_END);
            });
            deleteBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);
            editBtn.addThemeVariants(ButtonVariant.SMALL);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("").setWidth("10em").setFlexGrow(0);

        grid.setItems(query -> service.list(toSpringPageRequest(query)).stream());
        grid.setEmptyStateText("No competitions recorded yet");
        grid.setSizeFull();

        var newBtn = new Button("New Competition", e -> openDialog(null));
        newBtn.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(new ViewTitle("Competitions"), newBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, toolbar.getComponentAt(0));

        add(toolbar, grid);
    }

    private void openDialog(Competition existing) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Competition" : "Edit Competition");
        dialog.setWidth("420px");

        var nameFIeld = new TextField("Name");
        nameFIeld.setRequired(true);
        nameFIeld.setValue(existing != null ? existing.getName() : "");

        var datePicker = new DatePicker("Date");
        datePicker.setRequired(true);
        datePicker.setValue(existing != null ? existing.getDate() : LocalDate.now());

        var locationField = new TextField("Location");
        locationField.setValue(existing != null && existing.getLocation() != null ? existing.getLocation() : "");

        var divisionCombo = new ComboBox<Belt>("Division (belt)");
        divisionCombo.setItems(Belt.values());
        divisionCombo.setValue(existing != null ? existing.getDivision() : null);

        var weightField = new TextField("Weight Class");
        weightField.setValue(existing != null && existing.getWeightClass() != null ? existing.getWeightClass() : "");

        var resultCombo = new ComboBox<CompetitionResult>("Result");
        resultCombo.setItems(CompetitionResult.values());
        resultCombo.setValue(existing != null ? existing.getResult() : null);

        var wonField = new IntegerField("Matches Won");
        wonField.setMin(0);
        wonField.setValue(existing != null ? existing.getMatchesWon() : 0);

        var lostField = new IntegerField("Matches Lost");
        lostField.setMin(0);
        lostField.setValue(existing != null ? existing.getMatchesLost() : 0);

        var notesArea = new TextArea("Notes");
        notesArea.setValue(existing != null && existing.getNotes() != null ? existing.getNotes() : "");
        notesArea.setMinHeight("80px");

        var row1 = new HorizontalLayout(nameFIeld, datePicker);
        row1.setWidthFull();
        row1.setFlexGrow(2, nameFIeld);
        row1.setFlexGrow(1, datePicker);

        var row2 = new HorizontalLayout(divisionCombo, weightField);
        row2.setWidthFull();

        var row3 = new HorizontalLayout(wonField, lostField);
        row3.setWidthFull();

        var layout = new VerticalLayout(row1, locationField, row2, resultCombo, row3, notesArea);
        layout.setPadding(false);
        dialog.add(layout);

        var saveBtn = new Button("Save", e -> {
            if (nameFIeld.isEmpty() || datePicker.isEmpty()) {
                Notification.show("Name and date are required", 3000, Notification.Position.MIDDLE);
                return;
            }
            var competition = existing != null ? existing : new Competition(datePicker.getValue(), nameFIeld.getValue());
            competition.setName(nameFIeld.getValue());
            competition.setDate(datePicker.getValue());
            competition.setLocation(locationField.getValue().isBlank() ? null : locationField.getValue());
            competition.setDivision(divisionCombo.getValue());
            competition.setWeightClass(weightField.getValue().isBlank() ? null : weightField.getValue());
            competition.setResult(resultCombo.getValue());
            competition.setMatchesWon(wonField.getValue() != null ? wonField.getValue() : 0);
            competition.setMatchesLost(lostField.getValue() != null ? lostField.getValue() : 0);
            competition.setNotes(notesArea.getValue().isBlank() ? null : notesArea.getValue());
            service.save(competition);
            grid.getDataProvider().refreshAll();
            dialog.close();
            Notification.show("Competition saved", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}
