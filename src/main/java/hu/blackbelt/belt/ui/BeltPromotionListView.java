package hu.blackbelt.belt.ui;

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
import hu.blackbelt.belt.BeltPromotion;
import hu.blackbelt.belt.BeltPromotionService;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

@Route("belt-promotions")
@PageTitle("Belt Promotions")
@Menu(order = 2, icon = "vaadin:trophy", title = "Belt Promotions")
@PermitAll
public class BeltPromotionListView extends VerticalLayout {

    private final BeltPromotionService service;
    private final Grid<BeltPromotion> grid;

    public BeltPromotionListView(BeltPromotionService service) {
        this.service = service;
        setSizeFull();

        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

        grid = new Grid<>(BeltPromotion.class, false);
        grid.addColumn(BeltPromotion::getBelt).setHeader("Belt").setWidth("8em").setFlexGrow(0);
        grid.addColumn(p -> p.getStripes() + " stripe" + (p.getStripes() == 1 ? "" : "s"))
                .setHeader("Stripes").setWidth("8em").setFlexGrow(0);
        grid.addColumn(p -> dateFormatter.format(p.getDateAwarded())).setHeader("Date Awarded").setWidth("9em").setFlexGrow(0);
        grid.addColumn(BeltPromotion::getAwardedBy).setHeader("Awarded By").setFlexGrow(1);
        grid.addColumn(BeltPromotion::getAcademy).setHeader("Academy").setFlexGrow(1);
        grid.addColumn(BeltPromotion::getNotes).setHeader("Notes").setFlexGrow(2);
        grid.addComponentColumn(promotion -> {
            var editBtn = new Button("Edit", e -> openDialog(promotion));
            var deleteBtn = new Button("Delete", e -> {
                service.delete(promotion.getId());
                grid.getDataProvider().refreshAll();
                Notification.show("Promotion deleted", 2000, Notification.Position.BOTTOM_END);
            });
            deleteBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL);
            editBtn.addThemeVariants(ButtonVariant.SMALL);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("").setWidth("10em").setFlexGrow(0);

        grid.setItems(query -> service.list(toSpringPageRequest(query)).stream());
        grid.setEmptyStateText("No belt promotions recorded yet");
        grid.setSizeFull();

        var newBtn = new Button("New Promotion", e -> openDialog(null));
        newBtn.addThemeVariants(ButtonVariant.PRIMARY);

        var toolbar = new HorizontalLayout(new ViewTitle("Belt Promotions"), newBtn);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setFlexGrow(1, toolbar.getComponentAt(0));

        add(toolbar, grid);
    }

    private void openDialog(BeltPromotion existing) {
        var dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New Belt Promotion" : "Edit Belt Promotion");
        dialog.setWidth("400px");

        var beltCombo = new ComboBox<Belt>("Belt");
        beltCombo.setItems(Belt.values());
        beltCombo.setRequired(true);
        beltCombo.setValue(existing != null ? existing.getBelt() : Belt.WHITE);

        var stripesField = new IntegerField("Stripes");
        stripesField.setMin(0);
        stripesField.setMax(4);
        stripesField.setRequired(true);
        stripesField.setValue(existing != null ? existing.getStripes() : 0);

        var datePicker = new DatePicker("Date Awarded");
        datePicker.setRequired(true);
        datePicker.setValue(existing != null ? existing.getDateAwarded() : LocalDate.now());

        var awardedByField = new TextField("Awarded By");
        awardedByField.setValue(existing != null && existing.getAwardedBy() != null ? existing.getAwardedBy() : "");

        var academyField = new TextField("Academy");
        academyField.setValue(existing != null && existing.getAcademy() != null ? existing.getAcademy() : "");

        var notesArea = new TextArea("Notes");
        notesArea.setValue(existing != null && existing.getNotes() != null ? existing.getNotes() : "");
        notesArea.setMinHeight("80px");

        var layout = new VerticalLayout(beltCombo, stripesField, datePicker, awardedByField, academyField, notesArea);
        layout.setPadding(false);
        dialog.add(layout);

        var saveBtn = new Button("Save", e -> {
            if (beltCombo.isEmpty() || stripesField.isEmpty() || datePicker.isEmpty()) {
                Notification.show("Please fill all required fields", 3000, Notification.Position.MIDDLE);
                return;
            }
            var promotion = existing != null ? existing : new BeltPromotion(
                    beltCombo.getValue(), stripesField.getValue(), datePicker.getValue());
            promotion.setBelt(beltCombo.getValue());
            promotion.setStripes(stripesField.getValue());
            promotion.setDateAwarded(datePicker.getValue());
            promotion.setAwardedBy(awardedByField.getValue().isBlank() ? null : awardedByField.getValue());
            promotion.setAcademy(academyField.getValue().isBlank() ? null : academyField.getValue());
            promotion.setNotes(notesArea.getValue().isBlank() ? null : notesArea.getValue());
            service.save(promotion);
            grid.getDataProvider().refreshAll();
            dialog.close();
            Notification.show("Promotion saved", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.PRIMARY);

        dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), saveBtn);
        dialog.open();
    }
}
