package hu.blackbelt.help;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import hu.blackbelt.base.ui.MainLayout;
import hu.blackbelt.base.ui.ViewTitle;
import jakarta.annotation.security.PermitAll;

@Route(value = "help", layout = MainLayout.class)
@PageTitle("Help")
@Menu(order = 5, icon = "vaadin:question-circle-o", title = "Help")
@PermitAll
public class HelpView extends VerticalLayout {

    public HelpView() {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        add(new ViewTitle("Help & User Guide"));
        add(buildIntro());
        add(buildSection("Dashboard",
                "vaadin:dashboard",
                "Your personal at-a-glance summary. No data entry happens here — it is a read-only overview that updates automatically as you log sessions, promotions, and competitions.",
                new String[]{
                        "Mat Time — Total hours on the mat broken down by style (Gi, No-Gi, Grappling) with percentages.",
                        "Sessions — How many sessions you have logged this week, this month, and this year.",
                        "Belt — Your current rank, stripe count, and how long you have held the belt.",
                        "Competitions — Total tournaments entered, medals won, and your all-time match record."
                }));

        add(buildSection("Training",
                "vaadin:calendar",
                "Log every session you train. The grid shows all entries newest-first and supports pagination for large histories.",
                new String[]{
                        "Add session — Click the Add button (top right) to open the form. Fill in date, duration in minutes, training type, and optional location and notes.",
                        "Edit session — Click the pencil icon on any row to reopen the form pre-filled with that session's data.",
                        "Delete session — Click the trash icon and confirm the deletion.",
                        "Training types — Gi means training with the traditional uniform, No-Gi means shorts and rash guard, Grappling covers wrestling and submission grappling outside a BJJ context.",
                        "Duration — Enter the total mat time in minutes, not including warm-up or changing time unless you want to count it."
                }));

        add(buildSection("Belt Promotions",
                "vaadin:trophy",
                "Keep a permanent record of every belt and stripe promotion. The grid is sorted with the most recent promotion first.",
                new String[]{
                        "Add promotion — Click Add and enter the belt, stripe count (0–4), date awarded, and optionally who promoted you and at which academy.",
                        "Stripes — Stripes run 0 to 4. A value of 0 means a fresh belt with no stripes. After four stripes you would add a new entry with the next belt.",
                        "Notes — Use notes to record the occasion, how long the grading took, or anything else you want to remember.",
                        "Dashboard sync — The Dashboard Belt section always reflects your most recently added promotion automatically."
                }));

        add(buildSection("Competitions",
                "vaadin:records",
                "Track every tournament you enter, win or lose. Results feed into the dashboard medal and match-record counts.",
                new String[]{
                        "Add competition — Fill in the date, tournament name, and optionally location, belt division, weight class, result, and individual match tallies.",
                        "Result — Choose Gold, Silver, Bronze, or Participated. Participated is for tournaments where you entered but did not medal.",
                        "Match record — Matches Won and Matches Lost let you track individual bouts across the tournament, not just the final result.",
                        "Division — Selecting a belt here records which division you competed at, which may differ from your current training belt."
                }));

        add(buildSection("Profile & Stats",
                "vaadin:user",
                "Store your biometric information and let the app calculate training-related health metrics for you. The view has three tabs.",
                new String[]{
                        "Profile tab — Enter your name, date of birth, weight (kg), height (cm), gender, and lifestyle activity level. Save with the Save button at the bottom.",
                        "Stats tab — Once the profile is saved, this tab shows calculated metrics: age, BMI with category, BMR (calories your body burns at rest), TDEE (total daily energy expenditure including BJJ), daily protein target (2 g/kg body weight), and hydration target. Hover any value to read a tooltip explaining the formula.",
                        "Miscellaneous tab — A narrative summary combining training totals, estimated calorie burn, competition record, and your belt journey from first promotion to today.",
                        "Lifestyle — Sedentary means little or no exercise, lightly active means 1–3 days per week, moderately active 3–5 days, very active 6–7 days, extra active means physical job plus training.",
                        "Training hours per day — Used to adjust the hydration target (+500 ml per training hour) and the calorie burn estimate."
                }));

        add(buildDemoDataNote());
    }

    private Paragraph buildIntro() {
        var p = new Paragraph(
                "BJJ Progression Tracker helps you record your training journey: "
                        + "mat time, belt rank, competitions, and body metrics — all in one place. "
                        + "Use the sidebar to navigate between sections. This page explains what each section does and how to use it."
        );
        p.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return p;
    }

    private Details buildSection(String title, String icon, String summary, String[] bullets) {
        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "0.4em");

        var summaryPara = new Paragraph(summary);
        summaryPara.getStyle().set("margin-bottom", "0.5em");
        content.add(summaryPara);

        var list = new UnorderedList();
        for (var text : bullets) {
            var item = new ListItem();
            int dash = text.indexOf(" — ");
            if (dash > 0) {
                var label = new Span(text.substring(0, dash));
                label.getStyle().set("font-weight", "600");
                var rest = new Span(text.substring(dash));
                item.add(label, rest);
            } else {
                item.add(new Span(text));
            }
            list.add(item);
        }
        list.getStyle().set("padding-left", "1.2em").set("margin", "0");
        content.add(list);

        var details = new Details(title, content);
        details.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0 1em");
        details.setWidthFull();
        return details;
    }

    private Div buildDemoDataNote() {
        var box = new Div();
        box.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "1em 1.25em")
                .set("margin-top", "0.5em");

        var heading = new Span("Demo data");
        heading.getStyle().set("font-weight", "600");
        var text = new Paragraph(
                "The \"Load demo data\" button at the bottom of the sidebar populates the app with sample "
                        + "training sessions, a belt promotion, and competitions so you can explore the app before entering your own records. "
                        + "Loading demo data replaces everything currently in the database, so do this before you start entering real data."
        );
        text.getStyle().set("margin", "0.4em 0 0");
        box.add(heading, text);
        return box;
    }
}
