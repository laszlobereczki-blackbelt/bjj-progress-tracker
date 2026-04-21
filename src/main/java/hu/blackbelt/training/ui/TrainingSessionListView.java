package hu.blackbelt.training.ui;

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
import hu.blackbelt.training.TrainingSession;
import hu.blackbelt.training.TrainingSessionService;
import hu.blackbelt.training.TrainingType;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("training")
@PageTitle("Training Sessions")
@Menu(order = 1, icon = "vaadin:calendar", title = "Training")
@PermitAll
public class TrainingSessionListView extends VerticalLayout {

    private final TrainingSessionService service;
    private final Grid<TrainingSession> grid;

    public TrainingSessionListView(TrainingSessionService service) {
        this.service = service;
        setSizeFull();

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        grid = new Grid<>(TrainingSession.class, false);
        grid.addColumn(s -> dateFormatter.format(s.getDate())).setHeader("Date").setWidth("8em").setFlexGrow(0);
        grid.addColumn(s -> s.getDurationMinutes() + " min").setHeader("Duration").setWidth("7em").setFlexGrow(0);
        grid.addColumn(TrainingSession::getType).setHeader("Type").setWidth("8em").setFlexGrow(0);
        grid.addColumn(TrainingSession::getLocation).setHeader("Location").setFlexGrow(1);
        grid.addColumn(TrainingSession::getNotes).setHeader("Notes").setFlexGrow(2);
        grid.addComponentColumn(session -> {
            var editBtn = new Button("Edit", e -> openDialog(session));
            var deleteBtn = new Button("Delete", e -> {
                service.delete(session.getId());
                grid.getDataProvider().refreshAll();
                Notification.show("Session deleted", 2000, Notification.Position.BOTTOM_END);
            });
            deleteBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);
            editBtn.addThemeVariants(ButtonVariant.SMALL);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("").setWidth("10em").setFlexGrow(0);

        grid.setItems(query -> service.list(toSpringPageRequest(query)).stream());
        grid.setEmptyStateText("No training sessions recorded yet");
        grid.setSizeFull();

        var newBtn = new Button("New Session", e -> openDialog(null));
        newBtn.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(new ViewTitle("Training Sessions"), newBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, toolbar.getComponentAt(0));

        add(toolbar, grid);
    }

    private void openDialog(TrainingSession existing) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Training Session" : "Edit Training Session");
        dialog.setWidth("400px");

        var datePicker = new DatePicker("Date");
        datePicker.setRequired(true);
        datePicker.setValue(existing != null ? existing.getDate() : LocalDate.now());

        var durationField = new IntegerField("Duration (minutes)");
        durationField.setMin(1);
        durationField.setMax(600);
        durationField.setRequired(true);
        durationField.setValue(existing != null ? existing.getDurationMinutes() : 60);

        var typeCombo = new ComboBox<TrainingType>("Type");
        typeCombo.setItems(TrainingType.values());
        typeCombo.setRequired(true);
        typeCombo.setValue(existing != null ? existing.getType() : TrainingType.GI);

        var locationField = new TextField("Location");
        locationField.setValue(existing != null && existing.getLocation() != null ? existing.getLocation() : "");

        var notesArea = new TextArea("Notes");
        notesArea.setValue(existing != null && existing.getNotes() != null ? existing.getNotes() : "");
        notesArea.setMinHeight("80px");

        var layout = new VerticalLayout(datePicker, durationField, typeCombo, locationField, notesArea);
        layout.setPadding(false);
        dialog.add(layout);

        var saveBtn = new Button("Save", e -> {
            if (datePicker.isEmpty() || durationField.isEmpty() || typeCombo.isEmpty()) {
                Notification.show("Please fill all required fields", 3000, Notification.Position.MIDDLE);
                return;
            }
            var session = existing != null ? existing : new TrainingSession(
                    datePicker.getValue(), durationField.getValue(), typeCombo.getValue());
            session.setDate(datePicker.getValue());
            session.setDurationMinutes(durationField.getValue());
            session.setType(typeCombo.getValue());
            session.setLocation(locationField.getValue().isBlank() ? null : locationField.getValue());
            session.setNotes(notesArea.getValue().isBlank() ? null : notesArea.getValue());
            service.save(session);
            grid.getDataProvider().refreshAll();
            dialog.close();
            Notification.show("Session saved", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}
