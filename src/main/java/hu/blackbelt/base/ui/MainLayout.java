package hu.blackbelt.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import hu.blackbelt.base.DummyDataService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.ServletException;

@Layout
@PermitAll
public final class MainLayout extends AppLayout {

    private final DummyDataService dummyDataService;

    MainLayout(DummyDataService dummyDataService) {
        this.dummyDataService = dummyDataService;
        setPrimarySection(Section.DRAWER);
        addToDrawer(createApplicationHeader(), createApplicationDrawer(), createApplicationFooter());
    }

    private Component createApplicationHeader() {
        var appLogo = new Avatar("BJJ Tracker");
        appLogo.addClassName("app-logo");
        appLogo.addThemeVariants(AvatarVariant.AURA_FILLED, AvatarVariant.XSMALL);

        var appName = new Span("BJJ Tracker");
        appName.addClassName("app-name");

        var header = new HorizontalLayout(appLogo, appName);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        return header;
    }

    private Component createApplicationDrawer() {
        var scroller = new Scroller(createSideNav());
        scroller.addThemeVariants(ScrollerVariant.OVERFLOW_INDICATORS);
        return scroller;
    }

    private Component createApplicationFooter() {
        var loadDemoBtn = new Button("Load demo data", e -> confirmLoadDemo());
        loadDemoBtn.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);

        var signOutBtn = new Button("Sign out", e -> signOut());
        signOutBtn.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);

        var footer = new VerticalLayout(loadDemoBtn, signOutBtn);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.addClassName("app-footer");
        return footer;
    }

    private void confirmLoadDemo() {
        var dialog = new ConfirmDialog();
        dialog.setHeader("Load demo data");
        dialog.setText("This will delete all current data and replace it with demo data. This cannot be undone.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Load demo data");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> loadDemo());
        dialog.open();
    }

    private void loadDemo() {
        dummyDataService.resetToDemo();
        var notification = Notification.show("Demo data loaded successfully");
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setDuration(3000);
        UI.getCurrent().navigate("");
    }

    private void signOut() {
        try {
            VaadinServletRequest.getCurrent().getHttpServletRequest().logout();
        } catch (ServletException e) {
            // ignore — session is already invalid
        }
        UI.getCurrent().getSession().close();
        UI.getCurrent().getPage().setLocation("/login");
    }

    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.setMinWidth(200, Unit.PIXELS);
        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(createSideNavItem(entry)));
        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        if (menuEntry.icon() != null) {
            Component icon;
            if (menuEntry.icon().contains(".svg")) {
                icon = new SvgIcon(menuEntry.icon());
            } else {
                icon = new Icon(menuEntry.icon());
            }
            return new SideNavItem(menuEntry.title(), menuEntry.path(), icon);
        } else {
            return new SideNavItem(menuEntry.title(), menuEntry.path());
        }
    }
}
