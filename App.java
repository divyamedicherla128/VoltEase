package com.voltease;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * VoltEase - Smart Electricity Consumption and Billing System.
 *
 * Tariff rates retained from the user's original App.java:
 * First 100 units: Rs. 1.50/unit
 * Next 100 units: Rs. 2.50/unit
 * Next 100 units: Rs. 4.00/unit
 * Above 300 units: Rs. 6.00/unit
 * Fixed charge: Rs. 50.00
 * Other charge: Rs. 20.00
 *
 * Confirm these rates against the hackathon's official test-case sheet,
 * because the supplied problem statement does not specify tariff values.
 */
public class App extends Application {

    private static final Color NAVY = Color.web("#10243A");
    private static final Color BLUE = Color.web("#2563EB");
    private static final Color TEAL = Color.web("#0F9D91");
    private static final Color BG = Color.web("#F3F6FB");
    private static final Color MUTED = Color.web("#64748B");
    private static final Color WHITE = Color.WHITE;

    private static final double RATE_FIRST_100 = 1.50;
    private static final double RATE_NEXT_100 = 2.50;
    private static final double RATE_NEXT_100_AFTER_200 = 4.00;
    private static final double RATE_ABOVE_300 = 6.00;
    private static final double FIXED_CHARGE = 50.00;
    private static final double OTHER_CHARGE = 20.00;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    private final ObservableList<Consumer> consumers =
            FXCollections.observableArrayList();
    private final ObservableList<BillRecord> bills =
            FXCollections.observableArrayList();

    private final Path dataDirectory = Path.of(
            System.getProperty("user.home"), ".voltease");
    private final Path consumersFile = dataDirectory.resolve("consumers.dat");
    private final Path billsFile = dataDirectory.resolve("bills.dat");

    private BorderPane root;
    private Stage primaryStage;
    private int nextBillNumber = 1;
    private String dashboardConsumerId;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        loadData();

        root = new BorderPane();
        root.setStyle("-fx-background-color: #F2F6FC;");

        // Modern top welcome bar
        HBox welcomeBar = new HBox(18);
        welcomeBar.setAlignment(Pos.CENTER_LEFT);
        welcomeBar.setPadding(new Insets(18, 28, 18, 30));
        welcomeBar.setStyle("-fx-background-color: white; "
                + "-fx-border-color: #E5ECF5; -fx-border-width: 0 0 1 0;");

        VBox welcomeText = new VBox(4);
        Label welcome = new Label("Welcome to VoltEase!");
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 25));
        welcome.setTextFill(NAVY);
        Label welcomeSubtitle = new Label("Smart electricity consumption & billing at a glance");
        welcomeSubtitle.setFont(Font.font("Arial", 13));
        welcomeSubtitle.setTextFill(MUTED);
        welcomeText.getChildren().addAll(welcome, welcomeSubtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label dateLabel = new Label("▦   Today: " + LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy")));
        dateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        dateLabel.setTextFill(NAVY);
        dateLabel.setPadding(new Insets(13, 17, 13, 17));
        dateLabel.setStyle("-fx-background-color: #EEF3FF; -fx-background-radius: 18;");

        welcomeBar.getChildren().addAll(welcomeText, headerSpacer, dateLabel);
        root.setTop(welcomeBar);

        // Dark-blue navigation sidebar
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(24, 14, 22, 14));
        sidebar.setPrefWidth(232);
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, #092B59, #071D3D);");

        VBox brand = new VBox(5);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(8, 10, 25, 10));
        Label brandIcon = new Label("⚡");
        brandIcon.setFont(Font.font("Arial", FontWeight.BOLD, 38));
        brandIcon.setTextFill(Color.web("#FACC15"));
        Label brandName = new Label("VoltEase");
        brandName.setFont(Font.font("Arial", FontWeight.BOLD, 27));
        brandName.setTextFill(WHITE);
        Label brandTag = new Label("SMART POWER. SMART LIFE.");
        brandTag.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        brandTag.setTextFill(Color.web("#BBD2F4"));
        brand.getChildren().addAll(brandIcon, brandName, brandTag);

        Label menuTitle = new Label("MAIN MENU");
        menuTitle.setTextFill(Color.web("#9DB5D5"));
        menuTitle.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        menuTitle.setPadding(new Insets(8, 0, 7, 10));

        sidebar.getChildren().addAll(
                brand, menuTitle,
                navButton("⌂   Dashboard", this::showDashboard),
                navButton("♙   Consumers", this::showConsumers),
                navButton("⚡   Calculate bill", this::showBillCalculator),
                navButton("▤   Billing history", this::showHistory),
                navButton("▥   Reports & summaries", this::showReports)
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox sidebarFooter = new VBox(5);
        sidebarFooter.setPadding(new Insets(18, 8, 8, 8));
        Label footerLine = new Label("🌱  Save Electricity");
        footerLine.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        footerLine.setTextFill(Color.web("#D8F99A"));
        Label footerSub = new Label("     Save Money");
        footerSub.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        footerSub.setTextFill(WHITE);
        sidebarFooter.getChildren().addAll(footerLine, footerSub);
        sidebar.getChildren().addAll(spacer, sidebarFooter);
        root.setLeft(sidebar);

        showDashboard();

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("VoltEase | Smart Electricity Billing");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    private Button navButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(14, 14, 14, 14));
        button.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        button.setTextFill(Color.web("#EAF2FF"));
        button.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;");
        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: #17477E; -fx-background-radius: 10; -fx-cursor: hand;"));
        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; -fx-cursor: hand;"));
        button.setOnAction(e -> action.run());
        return button;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.setPadding(new Insets(11, 18, 11, 18));
        button.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        button.setTextFill(WHITE);
        button.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 10; -fx-cursor: hand;");
        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: #1D4ED8; -fx-background-radius: 10; -fx-cursor: hand;"));
        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 10; -fx-cursor: hand;"));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setPadding(new Insets(10, 16, 10, 16));
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        button.setTextFill(NAVY);
        button.setStyle("-fx-background-color: #E8EFF8; -fx-background-radius: 10; -fx-cursor: hand;");
        return button;
    }

    private Label pageTitle(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        label.setTextFill(NAVY);
        return label;
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 13));
        label.setTextFill(MUTED);
        return label;
    }

    private VBox card(String title, String value, String note, Color accent) {
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(MUTED);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        Label valueLabel = new Label(value);
        valueLabel.setTextFill(accent);
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 23));

        Label noteLabel = new Label(note);
        noteLabel.setTextFill(MUTED);
        noteLabel.setFont(Font.font("Arial", 10));
        noteLabel.setWrapText(true);

        VBox box = new VBox(9, titleLabel, valueLabel, noteLabel);
        box.setPadding(new Insets(18));
        box.setMinWidth(155);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
                + "-fx-border-color: #DCE6F3; -fx-border-radius: 16; "
                + "-fx-effect: dropshadow(gaussian, rgba(25,55,95,0.08), 12, 0, 0, 3);");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox contentPage(String title, String subtitle) {
        VBox page = new VBox(12);
        page.setPadding(new Insets(28));
        page.setStyle("-fx-background-color: #F2F6FC;");
        page.getChildren().addAll(pageTitle(title), mutedLabel(subtitle));
        return page;
    }

    private void showDashboard() {
        // Dashboard-only redesign. All billing, consumer and storage logic remains unchanged.
        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 24, 24));
        page.setStyle("-fx-background-color: #F2F6FC;");

        // Welcome strip
        HBox welcomeCard = new HBox(14);
        welcomeCard.setAlignment(Pos.CENTER_LEFT);
        welcomeCard.setPadding(new Insets(18, 22, 18, 22));
        welcomeCard.setStyle("-fx-background-color: linear-gradient(to right, #FFFFFF, #EAF2FF); "
                + "-fx-background-radius: 18; -fx-border-color: #DCE7F7; -fx-border-radius: 18; "
                + "-fx-effect: dropshadow(gaussian, rgba(25,55,95,0.08), 12, 0, 0, 3);");
        Label welcomeIcon = new Label("⚡");
        welcomeIcon.setFont(Font.font("Arial", FontWeight.BOLD, 34));
        welcomeIcon.setTextFill(Color.web("#F4B400"));
        VBox welcomeText = new VBox(4);
        Label welcomeTitle = new Label("Welcome to VoltEase!");
        welcomeTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        welcomeTitle.setTextFill(NAVY);
        Label welcomeSub = new Label("Let's use electricity wisely and save more every month.");
        welcomeSub.setFont(Font.font("Arial", 12));
        welcomeSub.setTextFill(MUTED);
        welcomeText.getChildren().addAll(welcomeTitle, welcomeSub);
        Region welcomeSpacer = new Region();
        HBox.setHgrow(welcomeSpacer, Priority.ALWAYS);
        Label today = new Label("▦  Today: " + LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd MMM yyyy")));
        today.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        today.setTextFill(NAVY);
        today.setPadding(new Insets(12, 15, 12, 15));
        today.setStyle("-fx-background-color: #E5ECFF; -fx-background-radius: 14;");
        welcomeCard.getChildren().addAll(welcomeIcon, welcomeText, welcomeSpacer, today);

        String currentMonth = YearMonth.now().toString();
        double monthlyRevenue = bills.stream()
                .filter(b -> b.month.equals(currentMonth))
                .mapToDouble(b -> b.total).sum();
        long monthlyUnits = bills.stream()
                .filter(b -> b.month.equals(currentMonth))
                .mapToLong(b -> b.units).sum();
        long monthlyBillCount = bills.stream()
                .filter(b -> b.month.equals(currentMonth)).count();

        HBox summaryCards = new HBox(12,
                card("REGISTERED CONSUMERS", String.valueOf(consumers.size()),
                        "Total consumer accounts", BLUE),
                card("BILLS THIS MONTH", String.valueOf(monthlyBillCount),
                        "Bills generated this month", Color.web("#7C3AED")),
                card("MONTHLY UNITS", String.valueOf(monthlyUnits),
                        "Units billed this month", TEAL),
                card("MONTHLY BILL TOTAL", money(monthlyRevenue),
                        "Total amount generated", Color.web("#D97706")));
        summaryCards.setAlignment(Pos.CENTER);

        // Appliance cards are built from the appliances selected by each consumer.
        Label applianceTitle = new Label("⚡  Your Household Appliances");
        applianceTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        applianceTitle.setTextFill(WHITE);
        HBox applianceHeading = new HBox(applianceTitle);
        applianceHeading.setAlignment(Pos.CENTER_LEFT);
        applianceHeading.setPadding(new Insets(13, 16, 13, 16));
        applianceHeading.setStyle("-fx-background-color: #092B59; -fx-background-radius: 13 13 0 0;");

        ComboBox<Consumer> householdBox = new ComboBox<>(consumers);
        householdBox.setPromptText("Choose a registered household");
        householdBox.setMaxWidth(Double.MAX_VALUE);
        householdBox.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Consumer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name + " (" + item.id + ")");
            }
        });
        householdBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Consumer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name + " (" + item.id + ")");
            }
        });
        Consumer dashboardConsumer = consumers.stream()
                .filter(c -> c.id.equals(dashboardConsumerId)).findFirst()
                .orElse(consumers.isEmpty() ? null : consumers.get(consumers.size() - 1));
        if (dashboardConsumer != null) {
            dashboardConsumerId = dashboardConsumer.id;
            householdBox.setValue(dashboardConsumer);
        }
        householdBox.setOnAction(e -> {
            Consumer chosen = householdBox.getValue();
            dashboardConsumerId = chosen == null ? null : chosen.id;
            showDashboard();
        });
        HBox householdSelector = new HBox(10, mutedLabel("Household:"), householdBox);
        householdSelector.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(householdBox, Priority.NEVER);

        GridPane applianceGrid = new GridPane();
        applianceGrid.setHgap(12);
        applianceGrid.setVgap(12);
        applianceGrid.setPadding(new Insets(12));
        Consumer selectedHousehold = dashboardConsumer;
        Map<String, Integer> configured = selectedHousehold == null
                ? new LinkedHashMap<>() : selectedHousehold.getApplianceLimits();
        String[][] applianceInfo = {
                {"📺", "Television", "3"}, {"🌀", "Fan", "7"},
                {"🧊", "Refrigerator", "8"}, {"💡", "Lights", "5"},
                {"❄️", "Air Conditioner", "4"}, {"🔋", "Inverter", "3"},
                {"🧺", "Washing Machine", "1"}, {"🚰", "Water Pump", "1"}
        };
        int applianceIndex = 0;
        for (String[] a : applianceInfo) {
            if (!configured.containsKey(a[1])) continue;
            int limit = configured.get(a[1]);
            int used = Integer.parseInt(a[2]);
            boolean exceeded = used > limit;
            String accent = exceeded ? "#DC2626" : "#16A34A";
            VBox applianceCard = new VBox(7);
            applianceCard.setAlignment(Pos.CENTER);
            applianceCard.setPadding(new Insets(13, 8, 12, 8));
            applianceCard.setMinHeight(166);
            applianceCard.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                    + "-fx-border-color: #DCE6F3; -fx-border-radius: 14; "
                    + "-fx-effect: dropshadow(gaussian, rgba(25,55,95,0.07), 8, 0, 0, 2);");
            Label picture = new Label(a[0]);
            picture.setFont(Font.font("Arial", 34));
            Label name = new Label(a[1]);
            name.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            name.setTextFill(NAVY);
            Label hours = new Label(used + " / " + limit + " hours");
            hours.setFont(Font.font("Arial", FontWeight.BOLD, 15));
            hours.setTextFill(Color.web(accent));
            ProgressBar usageBar = new ProgressBar(Math.min(1.0, (double) used / Math.max(1, limit)));
            usageBar.setPrefWidth(145);
            usageBar.setMaxWidth(Double.MAX_VALUE);
            usageBar.setStyle("-fx-accent: " + accent + ";");
            Label status = new Label((exceeded ? "⚠  Limit exceeded" : "✓  Within limit"));
            status.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            status.setTextFill(Color.web(accent));
            applianceCard.getChildren().addAll(picture, name, hours, usageBar, status);
            applianceGrid.add(applianceCard, applianceIndex % 3, applianceIndex / 3);
            applianceIndex++;
            GridPane.setHgrow(applianceCard, Priority.ALWAYS);
            GridPane.setFillWidth(applianceCard, true);
        }
        if (selectedHousehold == null || configured.isEmpty()) {
            Label setupPrompt = new Label(selectedHousehold == null
                    ? "Register a consumer and select their household appliances to personalize this dashboard."
                    : "No appliances selected yet. Open Consumers and choose Configure appliances.");
            setupPrompt.setWrapText(true);
            setupPrompt.setPadding(new Insets(24));
            setupPrompt.setTextFill(MUTED);
            applianceGrid.add(setupPrompt, 0, 0, 3, 1);
        }
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.333);
            cc.setHgrow(Priority.ALWAYS);
            applianceGrid.getColumnConstraints().add(cc);
        }
        VBox appliancePanel = new VBox(0, applianceHeading, householdSelector, applianceGrid);
        appliancePanel.setStyle("-fx-background-color: #EAF1FC; -fx-background-radius: 13; "
                + "-fx-border-color: #DCE6F3; -fx-border-radius: 13;");

        // Right-side dashboard information panels
        VBox alertPanel = new VBox(8);
        alertPanel.setPadding(new Insets(16));
        alertPanel.setStyle("-fx-background-color: #FDE8E8; -fx-background-radius: 14; "
                + "-fx-border-color: #F5B8B8; -fx-border-radius: 14;");
        Label alertTitle = new Label("🔔  Usage Alert");
        alertTitle.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        alertTitle.setTextFill(Color.web("#B91C1C"));
        String alertMessage = "Register a household and select appliances to receive personalized usage alerts.";
        if (!configured.isEmpty()) {
            alertMessage = "All selected appliances are within their sample daily limits. Keep saving energy!";
            for (String[] appliance : applianceInfo) {
                if (configured.containsKey(appliance[1])
                        && Integer.parseInt(appliance[2]) > configured.get(appliance[1])) {
                    alertMessage = appliance[1] + " usage is above its daily limit. Please reduce usage.";
                    break;
                }
            }
        }
        Label alertText = new Label(alertMessage);
        alertText.setWrapText(true);
        alertText.setFont(Font.font("Arial", 12));
        alertText.setTextFill(Color.web("#7F1D1D"));
        alertPanel.getChildren().addAll(alertTitle, alertText);

        VBox tipsPanel = new VBox(9);
        tipsPanel.setPadding(new Insets(16));
        tipsPanel.setStyle("-fx-background-color: #FFF6D9; -fx-background-radius: 14; "
                + "-fx-border-color: #F1D98D; -fx-border-radius: 14;");
        Label tipsTitle = new Label("💡  Energy Saving Tips");
        tipsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        tipsTitle.setTextFill(NAVY);
        tipsPanel.getChildren().addAll(tipsTitle,
                mutedLabel("✓  Turn off lights when not in use."),
                mutedLabel("✓  Use fans instead of AC when comfortable."),
                mutedLabel("✓  Unplug devices when not needed."),
                mutedLabel("✓  Keep AC at a comfortable 24°C."));

        VBox billPanel = new VBox(7);
        billPanel.setPadding(new Insets(16));
        billPanel.setStyle("-fx-background-color: #F0E8FF; -fx-background-radius: 14; "
                + "-fx-border-color: #D8C4FA; -fx-border-radius: 14;");
        Label billTitle = new Label("▤  Monthly Bill");
        billTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        billTitle.setTextFill(NAVY);
        Label billAmount = new Label(money(monthlyRevenue));
        billAmount.setFont(Font.font("Arial", FontWeight.BOLD, 23));
        billAmount.setTextFill(Color.web("#6D28D9"));
        Button viewBills = secondaryButton("View billing history  →");
        viewBills.setOnAction(e -> showHistory());
        billPanel.getChildren().addAll(billTitle, billAmount, viewBills);

        VBox budgetPanel = new VBox(7);
        budgetPanel.setPadding(new Insets(16));
        budgetPanel.setStyle("-fx-background-color: #E5F8EA; -fx-background-radius: 14; "
                + "-fx-border-color: #B6E5C2; -fx-border-radius: 14;");
        Label budgetTitle = new Label("🎯  Monthly Summary");
        budgetTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        budgetTitle.setTextFill(NAVY);
        Label budgetText = new Label(monthlyUnits + " units  •  " + monthlyBillCount + " bill(s)");
        budgetText.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        budgetText.setTextFill(Color.web("#166534"));
        budgetPanel.getChildren().addAll(budgetTitle, budgetText,
                mutedLabel("Review your monthly usage and billing reports."));

        VBox rightColumn = new VBox(12, alertPanel, tipsPanel, billPanel, budgetPanel);
        rightColumn.setPrefWidth(265);
        rightColumn.setMinWidth(230);

        HBox mainContent = new HBox(14, appliancePanel, rightColumn);
        HBox.setHgrow(appliancePanel, Priority.ALWAYS);
        appliancePanel.setMaxWidth(Double.MAX_VALUE);

        Label recentTitle = new Label("Recent Billing Activity");
        recentTitle.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        recentTitle.setTextFill(NAVY);
        TableView<BillRecord> recentTable = createBillTable();
        recentTable.setItems(FXCollections.observableArrayList(
                bills.stream()
                        .sorted(Comparator.comparing((BillRecord b) -> b.date).reversed())
                        .limit(5)
                        .collect(Collectors.toList())));
        recentTable.setPrefHeight(190);
        VBox recentPanel = panel(recentTitle, recentTable);

        Label demoNote = new Label("Note: Daily limits are saved per household. Hours used are demonstration values until live appliance monitoring is connected.");
        demoNote.setWrapText(true);
        demoNote.setFont(Font.font("Arial", 10));
        demoNote.setTextFill(MUTED);

        page.getChildren().addAll(welcomeCard, summaryCards, mainContent, recentPanel, demoNote);
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F2F6FC; -fx-background-color: #F2F6FC;");
        root.setCenter(scroll);
    }

    private void showConsumers() {
        VBox page = contentPage("Consumer management", "Register consumers and quickly find their account details.");

        TextField search = new TextField();
        search.setPromptText("Search by consumer ID, name, address or phone");
        search.setMaxWidth(Double.MAX_VALUE);

        TextField idField = new TextField();
        idField.setPromptText("e.g. CUST001");
        TextField nameField = new TextField();
        nameField.setPromptText("Consumer full name");
        TextField addressField = new TextField();
        addressField.setPromptText("Service address");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number (optional)");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(9);
        addFormField(formGrid, 0, 0, "Consumer ID", idField);
        addFormField(formGrid, 1, 0, "Consumer name", nameField);
        addFormField(formGrid, 0, 1, "Address", addressField);
        addFormField(formGrid, 1, 1, "Phone number", phoneField);

        Button addButton = primaryButton("Save consumer");
        Button clearButton = secondaryButton("Clear fields");
        HBox formButtons = new HBox(10, addButton, clearButton);

        VBox formCard = panel(new Label("Register a new consumer"), formGrid, formButtons);

        FilteredList<Consumer> filtered = new FilteredList<>(consumers, c -> true);
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            String q = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(c -> q.isEmpty()
                    || c.id.toLowerCase(Locale.ROOT).contains(q)
                    || c.name.toLowerCase(Locale.ROOT).contains(q)
                    || c.address.toLowerCase(Locale.ROOT).contains(q)
                    || c.phone.toLowerCase(Locale.ROOT).contains(q));
        });

        TableView<Consumer> table = new TableView<>(filtered);
        table.getColumns().add(textColumn("Consumer ID", c -> c.id, 130));
        table.getColumns().add(textColumn("Name", c -> c.name, 180));
        table.getColumns().add(textColumn("Address", c -> c.address, 260));
        table.getColumns().add(textColumn("Phone", c -> c.phone.isBlank() ? "—" : c.phone, 130));
        styleTable(table);
        table.setPrefHeight(290);

        Button configureAppliancesButton = secondaryButton("Configure appliances for selected consumer");
        configureAppliancesButton.setOnAction(e -> {
            Consumer chosen = table.getSelectionModel().getSelectedItem();
            if (chosen == null) {
                showAlert(Alert.AlertType.INFORMATION, "Select a consumer",
                        "Select a consumer from the table first.");
                return;
            }
            showApplianceSetup(chosen);
        });
        formButtons.getChildren().add(configureAppliancesButton);

        addButton.setOnAction(e -> {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String address = addressField.getText().trim();
            String phone = phoneField.getText().trim();

            if (id.isEmpty() || name.isEmpty() || address.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing information",
                        "Consumer ID, name and address are required.");
                return;
            }
            if (id.length() > 30 || name.length() > 80 || address.length() > 180) {
                showAlert(Alert.AlertType.WARNING, "Input too long",
                        "Please shorten the ID, name or address and try again.");
                return;
            }
            if (!phone.isBlank() && !phone.matches("[0-9+() -]{7,18}")) {
                showAlert(Alert.AlertType.WARNING, "Invalid phone number",
                        "Enter a valid phone number or leave it blank.");
                return;
            }
            boolean duplicate = consumers.stream().anyMatch(c -> c.id.equalsIgnoreCase(id));
            if (duplicate) {
                showAlert(Alert.AlertType.WARNING, "Duplicate consumer",
                        "That consumer ID already exists. Use a unique ID.");
                return;
            }

            Consumer newConsumer = new Consumer(id, name, address, phone);
            consumers.add(newConsumer);
            if (saveData()) {
                idField.clear();
                nameField.clear();
                addressField.clear();
                phoneField.clear();
                dashboardConsumerId = newConsumer.id;
                showApplianceSetup(newConsumer);
                showDashboard();
            } else {
                consumers.removeIf(c -> c.id.equalsIgnoreCase(id));
            }
        });

        clearButton.setOnAction(e -> {
            idField.clear();
            nameField.clear();
            addressField.clear();
            phoneField.clear();
        });

        page.getChildren().addAll(formCard, search, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(page);
    }

    private void showApplianceSetup(Consumer consumer) {
        Dialog<Map<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("VoltEase - Household Setup");
        dialog.setHeaderText("Select the appliances available at " + consumer.name + "'s home");
        dialog.initOwner(primaryStage);

        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("Television", 5);
        defaults.put("Fan", 10);
        defaults.put("Refrigerator", 24);
        defaults.put("Lights", 8);
        defaults.put("Air Conditioner", 6);
        String[] names = {"Television", "Fan", "Refrigerator", "Lights", "Air Conditioner",
                "Inverter", "Washing Machine", "Water Pump"};
        String[] icons = {"📺", "🌀", "🧊", "💡", "❄️", "🔋", "🧺", "🚰"};
        int[] defaultHours = {5, 10, 24, 8, 6, 8, 2, 2};
        Map<String, CheckBox> checks = new LinkedHashMap<>();
        Map<String, Spinner<Integer>> spinners = new LinkedHashMap<>();

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        for (int i = 0; i < names.length; i++) {
            String appliance = names[i];
            CheckBox check = new CheckBox(icons[i] + "  " + appliance);
            check.setSelected(consumer.getApplianceLimits().containsKey(appliance)
                    || (consumer.getApplianceLimits().isEmpty() && defaults.containsKey(appliance)));
            Spinner<Integer> hours = new Spinner<>(1, 24,
                    consumer.getApplianceLimits().getOrDefault(appliance, defaultHours[i]));
            hours.setEditable(true);
            hours.setPrefWidth(90);
            checks.put(appliance, check);
            spinners.put(appliance, hours);
            grid.add(check, 0, i);
            grid.add(new Label("Daily limit:"), 1, i);
            grid.add(hours, 2, i);
        }
        Label note = new Label("Choose the appliances you have and set each daily usage limit (1–24 hours).");
        note.setWrapText(true);
        VBox content = new VBox(12, note, grid);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);
        ButtonType saveType = new ButtonType("Save appliance setup", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button != saveType) return null;
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String appliance : names) {
                if (checks.get(appliance).isSelected()) {
                    try {
                        int value = spinners.get(appliance).getValue();
                        if (value < 1 || value > 24) throw new NumberFormatException();
                        result.put(appliance, value);
                    } catch (RuntimeException ex) {
                        showAlert(Alert.AlertType.WARNING, "Invalid daily limit",
                                "Set a valid daily limit from 1 to 24 hours for " + appliance + ".");
                        return null;
                    }
                }
            }
            if (result.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Select an appliance",
                        "Please select at least one household appliance.");
                return null;
            }
            return result;
        });
        // Keep the dialog open if the user accidentally deselects every appliance.
        dialog.getDialogPane().lookupButton(saveType).addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    boolean anySelected = checks.values().stream().anyMatch(CheckBox::isSelected);
                    if (!anySelected) {
                        showAlert(Alert.AlertType.WARNING, "Select an appliance",
                                "Please select at least one household appliance.");
                        event.consume();
                    }
                });

        Optional<Map<String, Integer>> result = dialog.showAndWait();
        if (result.isPresent()) {
            Map<String, Integer> old = new LinkedHashMap<>(consumer.getApplianceLimits());
            consumer.setApplianceLimits(result.get());
            if (saveData()) {
                dashboardConsumerId = consumer.id;
                showAlert(Alert.AlertType.INFORMATION, "Setup saved",
                        "Household appliances and daily limits have been saved for " + consumer.name + ".");
            } else {
                consumer.setApplianceLimits(old);
            }
        }
    }

    private void showBillCalculator() {
        VBox page = contentPage("Electricity bill calculator", "Enter meter readings to calculate slab-wise charges and generate a monthly bill.");

        ComboBox<Consumer> consumerBox = new ComboBox<>(consumers);
        consumerBox.setPromptText("Select a registered consumer");
        consumerBox.setMaxWidth(Double.MAX_VALUE);
        consumerBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Consumer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.id + " — " + item.name);
            }
        });
        consumerBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Consumer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.id + " — " + item.name);
            }
        });

        TextField previousField = new TextField();
        previousField.setPromptText("Previous meter reading (whole units)");
        TextField currentField = new TextField();
        currentField.setPromptText("Current meter reading (whole units)");

        GridPane fields = new GridPane();
        fields.setHgap(14);
        fields.setVgap(12);
        addFormField(fields, 0, 0, "Consumer account", consumerBox);
        addFormField(fields, 1, 0, "Previous reading", previousField);
        addFormField(fields, 0, 1, "Current reading", currentField);

        Label unitsValue = resultValue("—");
        Label energyValue = resultValue("—");
        Label totalValue = resultValue("—");
        Label receipt = new Label("Your bill details will appear here after calculation.");
        receipt.setWrapText(true);
        receipt.setPadding(new Insets(18));
        receipt.setMinHeight(190);
        receipt.setMaxWidth(Double.MAX_VALUE);
        receipt.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; "
                + "-fx-border-color: #DCE5F0; -fx-border-radius: 10; -fx-font-family: 'Monospaced'; -fx-font-size: 12px;");

        HBox results = new HBox(14,
                metricBox("UNITS CONSUMED", unitsValue),
                metricBox("ENERGY CHARGE", energyValue),
                metricBox("TOTAL BILL", totalValue));
        Button calculateButton = primaryButton("Calculate & save bill");
        Button printButton = secondaryButton("Print receipt");
        printButton.setDisable(true);

        final BillRecord[] lastGeneratedBill = {null};

        calculateButton.setOnAction(e -> {
            Consumer selected = consumerBox.getValue();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Select consumer",
                        "Register and select a consumer before generating a bill.");
                return;
            }

            // Allow only one bill for each consumer in the current month.
            String billingMonth = YearMonth.now().toString();
            boolean billAlreadyExists = bills.stream().anyMatch(bill ->
                    bill.consumerId.equalsIgnoreCase(selected.id)
                            && bill.month.equals(billingMonth));
            if (billAlreadyExists) {
                showAlert(Alert.AlertType.WARNING, "Monthly bill already exists",
                        "A bill has already been generated for " + selected.name
                                + " for " + YearMonth.now().format(MONTH_FORMAT)
                                + ". Only one bill per consumer is allowed each month.");
                return;
            }

            String previousText = previousField.getText().trim();
            String currentText = currentField.getText().trim();
            if (previousText.isEmpty() || currentText.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Missing readings",
                        "Enter both previous and current meter readings.");
                return;
            }

            try {
                long previous = Long.parseLong(previousText);
                long current = Long.parseLong(currentText);

                if (previous < 0 || current < 0) {
                    showAlert(Alert.AlertType.WARNING, "Invalid readings",
                            "Meter readings cannot be negative.");
                    return;
                }
                if (current < previous) {
                    showAlert(Alert.AlertType.WARNING, "Invalid readings",
                            "Current reading cannot be less than previous reading.");
                    return;
                }

                long units = current - previous;
                double energy = calculateEnergyCharge(units);
                double total = energy + FIXED_CHARGE + OTHER_CHARGE;

                String billId = String.format(Locale.US, "B%04d", nextBillNumber);
                BillRecord record = new BillRecord(
                        billId, selected.id, selected.name, previous, current,
                        units, energy, FIXED_CHARGE, OTHER_CHARGE, total,
                        LocalDate.now(), YearMonth.now().toString());

                bills.add(record);
                if (!saveData()) {
                    bills.remove(record);
                    return;
                }
                nextBillNumber++;

                unitsValue.setText(String.valueOf(units));
                energyValue.setText(money(energy));
                totalValue.setText(money(total));
                receipt.setText(buildReceipt(record));
                lastGeneratedBill[0] = record;
                printButton.setDisable(false);

                showAlert(Alert.AlertType.INFORMATION, "Bill generated",
                        "Bill " + billId + " was calculated and saved successfully.");
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Invalid input",
                        "Meter readings must be valid whole numbers.");
            }
        });

        printButton.setOnAction(e -> printReceipt(receipt));

        VBox tariffCard = panel(
                new Label("Tariff details"),
                mutedLabel("First 100 units: ₹1.50/unit   •   101–200: ₹2.50/unit"),
                mutedLabel("201–300: ₹4.00/unit   •   Above 300: ₹6.00/unit"),
                mutedLabel("Fixed charge: ₹50.00   •   Other charge: ₹20.00")
        );

        VBox inputCard = panel(new Label("Meter information"), fields, calculateButton);
        VBox receiptCard = panel(new Label("Bill receipt"), results, receipt,
                new HBox(10, printButton));

        page.getChildren().addAll(inputCard, tariffCard, receiptCard);
        root.setCenter(new ScrollPane(page));
    }

    private double calculateEnergyCharge(long units) {
        if (units <= 100) {
            return units * RATE_FIRST_100;
        }
        if (units <= 200) {
            return 100 * RATE_FIRST_100 + (units - 100) * RATE_NEXT_100;
        }
        if (units <= 300) {
            return 100 * RATE_FIRST_100 + 100 * RATE_NEXT_100
                    + (units - 200) * RATE_NEXT_100_AFTER_200;
        }
        return 100 * RATE_FIRST_100 + 100 * RATE_NEXT_100
                + 100 * RATE_NEXT_100_AFTER_200
                + (units - 300) * RATE_ABOVE_300;
    }

    private String buildReceipt(BillRecord b) {
        return "                 VOLTEASE\n"
                + "        ELECTRICITY BILL RECEIPT\n"
                + "------------------------------------------\n"
                + "Bill ID          : " + b.billId + "\n"
                + "Consumer ID      : " + b.consumerId + "\n"
                + "Consumer Name    : " + b.consumerName + "\n"
                + "Billing date     : " + b.date.format(DATE_FORMAT) + "\n"
                + "Billing month    : " + YearMonth.parse(b.month).format(MONTH_FORMAT) + "\n"
                + "Previous reading : " + b.previousReading + "\n"
                + "Current reading  : " + b.currentReading + "\n"
                + "Units consumed   : " + b.units + "\n"
                + "------------------------------------------\n"
                + String.format(Locale.US, "Energy charge    : ₹%.2f%n", b.energyCharge)
                + String.format(Locale.US, "Fixed charge     : ₹%.2f%n", b.fixedCharge)
                + String.format(Locale.US, "Other charges    : ₹%.2f%n", b.otherCharge)
                + "------------------------------------------\n"
                + String.format(Locale.US, "TOTAL AMOUNT     : ₹%.2f%n", b.total)
                + "------------------------------------------\n"
                + "Thank you for using VoltEase!";
    }

    private void printReceipt(Label receipt) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job == null) {
            showAlert(Alert.AlertType.WARNING, "Printer unavailable",
                    "No printer is currently available.");
            return;
        }
        if (job.showPrintDialog(primaryStage)) {
            if (job.printPage(receipt)) {
                job.endJob();
                showAlert(Alert.AlertType.INFORMATION, "Print complete",
                        "The receipt was sent to the printer.");
            } else {
                job.cancelJob();
                showAlert(Alert.AlertType.ERROR, "Print failed",
                        "The receipt could not be printed.");
            }
        } else {
            job.cancelJob();
        }
    }

    private void showHistory() {
        VBox page = contentPage("Billing history", "Review all generated bills and search by bill ID, consumer or month.");

        TextField search = new TextField();
        search.setPromptText("Search bill ID, consumer ID, name or month");

        FilteredList<BillRecord> filtered = new FilteredList<>(bills, b -> true);
        search.textProperty().addListener((obs, oldValue, newValue) -> {
            String q = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(b -> q.isEmpty()
                    || b.billId.toLowerCase(Locale.ROOT).contains(q)
                    || b.consumerId.toLowerCase(Locale.ROOT).contains(q)
                    || b.consumerName.toLowerCase(Locale.ROOT).contains(q)
                    || b.month.toLowerCase(Locale.ROOT).contains(q));
        });

        TableView<BillRecord> table = createBillTable();
        table.setItems(filtered);
        VBox.setVgrow(table, Priority.ALWAYS);
        page.getChildren().addAll(search, table);
        root.setCenter(page);
    }

    private TableView<BillRecord> createBillTable() {
        TableView<BillRecord> table = new TableView<>();
        table.getColumns().add(textColumn("Bill ID", b -> b.billId, 90));
        table.getColumns().add(textColumn("Consumer ID", b -> b.consumerId, 110));
        table.getColumns().add(textColumn("Consumer", b -> b.consumerName, 150));
        table.getColumns().add(textColumn("Month", b -> YearMonth.parse(b.month).format(MONTH_FORMAT), 130));
        table.getColumns().add(textColumn("Units", b -> String.valueOf(b.units), 75));
        table.getColumns().add(textColumn("Amount", b -> money(b.total), 110));
        table.getColumns().add(textColumn("Date", b -> b.date.format(DATE_FORMAT), 110));
        styleTable(table);
        return table;
    }

    private void showReports() {
        VBox page = contentPage("Reports & summaries", "Consumer-wise totals and month-wise billing summaries.");

        List<String> monthValues = bills.stream()
                .map(b -> b.month)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        ComboBox<String> monthBox = new ComboBox<>();
        monthBox.getItems().add("All months");
        monthBox.getItems().addAll(monthValues);
        monthBox.setValue("All months");
        monthBox.setPrefWidth(220);

        Label monthBillTotal = resultValue("₹0.00");
        Label monthUnits = resultValue("0");
        Label monthBillCount = resultValue("0");

        HBox summaryCards = new HBox(14,
                metricBox("TOTAL BILLED", monthBillTotal),
                metricBox("UNITS BILLED", monthUnits),
                metricBox("BILLS GENERATED", monthBillCount));

        TableView<ConsumerSummary> summaryTable = new TableView<>();
        summaryTable.getColumns().add(textColumn("Consumer ID", s -> s.consumerId, 120));
        summaryTable.getColumns().add(textColumn("Consumer name", s -> s.consumerName, 190));
        summaryTable.getColumns().add(textColumn("Bills", s -> String.valueOf(s.billCount), 80));
        summaryTable.getColumns().add(textColumn("Total units", s -> String.valueOf(s.units), 100));
        summaryTable.getColumns().add(textColumn("Total amount", s -> money(s.total), 130));
        styleTable(summaryTable);

        Runnable refresh = () -> {
            String selected = monthBox.getValue();
            List<BillRecord> selectedBills = bills.stream()
                    .filter(b -> selected == null || selected.equals("All months") || b.month.equals(selected))
                    .collect(Collectors.toList());

            monthBillTotal.setText(money(selectedBills.stream().mapToDouble(b -> b.total).sum()));
            monthUnits.setText(String.valueOf(selectedBills.stream().mapToLong(b -> b.units).sum()));
            monthBillCount.setText(String.valueOf(selectedBills.size()));

            List<ConsumerSummary> summaries = consumers.stream().map(c -> {
                List<BillRecord> consumerBills = selectedBills.stream()
                        .filter(b -> b.consumerId.equalsIgnoreCase(c.id))
                        .collect(Collectors.toList());
                return new ConsumerSummary(c.id, c.name,
                        consumerBills.size(),
                        consumerBills.stream().mapToLong(b -> b.units).sum(),
                        consumerBills.stream().mapToDouble(b -> b.total).sum());
            }).collect(Collectors.toList());

            summaryTable.setItems(FXCollections.observableArrayList(summaries));
        };

        monthBox.setOnAction(e -> refresh.run());
        refresh.run();

        HBox filterRow = new HBox(12, new Label("Billing period:"), monthBox);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox reportCard = panel(new Label("Consumer-wise billing summary"), summaryTable);
        VBox.setVgrow(summaryTable, Priority.ALWAYS);
        page.getChildren().addAll(filterRow, summaryCards, reportCard);
        VBox.setVgrow(reportCard, Priority.ALWAYS);
        root.setCenter(page);
    }

    private <T> TableColumn<T, String> textColumn(
            String title, java.util.function.Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data ->
                new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private void styleTable(TableView<?> table) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: white; -fx-border-color: #DCE6F3; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 12px;");
        table.setPlaceholder(new Label("No records to display yet."));
    }

    private VBox panel(javafx.scene.Node... children) {
        VBox box = new VBox(14, children);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #E0E8F2; -fx-border-radius: 14; "
                + "-fx-effect: dropshadow(gaussian, rgba(25,55,95,0.06), 10, 0, 0, 2);");
        return box;
    }

    private VBox metricBox(String title, Label value) {
        Label heading = new Label(title);
        heading.setTextFill(MUTED);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        VBox box = new VBox(8, heading, value);
        box.setPadding(new Insets(15));
        box.setMinWidth(145);
        box.setStyle("-fx-background-color: #F5F8FE; -fx-background-radius: 12; "
                + "-fx-border-color: #E0E8F2; -fx-border-radius: 12;");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Label resultValue(String text) {
        Label label = new Label(text);
        label.setTextFill(BLUE);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 19));
        return label;
    }

    private void addFormField(GridPane grid, int column, int row, String label, Control control) {
        VBox field = new VBox(6, mutedLabel(label), control);
        control.setMaxWidth(Double.MAX_VALUE);
        grid.add(field, column, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private String money(double value) {
        return String.format(Locale.US, "₹%,.2f", value);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("VoltEase");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.exists(consumersFile)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(consumersFile))) {
                    Object data = in.readObject();
                    if (data instanceof List<?>) {
                        for (Object item : (List<?>) data) {
                            if (item instanceof Consumer) {
                                consumers.add((Consumer) item);
                            }
                        }
                    }
                }
            }
            if (Files.exists(billsFile)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(billsFile))) {
                    Object data = in.readObject();
                    if (data instanceof List<?>) {
                        for (Object item : (List<?>) data) {
                            if (item instanceof BillRecord) {
                                bills.add((BillRecord) item);
                            }
                        }
                    }
                }
            }
            nextBillNumber = bills.size() + 1;
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("Could not load VoltEase data: " + ex.getMessage());
        }
    }


    private boolean saveData() {
        try {
            Files.createDirectories(dataDirectory);
            writeObject(consumersFile, new ArrayList<>(consumers));
            writeObject(billsFile, new ArrayList<>(bills));
            return true;
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Could not save data",
                    "Your change could not be saved. Check folder permissions and try again.\n"
                            + ex.getMessage());
            return false;
        }
    }

    private void writeObject(Path file, Object data) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(data);
        }
    }

    @Override
    public void stop() {
        saveData();
    }

    public static class Consumer implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final String name;
        private final String address;
        private final String phone;
        // Added without changing serialVersionUID so existing consumer records remain readable.
        private Map<String, Integer> applianceLimits = new LinkedHashMap<>();

        Consumer(String id, String name, String address, String phone) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.phone = phone;
        }

        Map<String, Integer> getApplianceLimits() {
            if (applianceLimits == null) applianceLimits = new LinkedHashMap<>();
            return new LinkedHashMap<>(applianceLimits);
        }

        void setApplianceLimits(Map<String, Integer> limits) {
            applianceLimits = new LinkedHashMap<>(limits);
        }
    }

    public static class BillRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String billId;
        private final String consumerId;
        private final String consumerName;
        private final long previousReading;
        private final long currentReading;
        private final long units;
        private final double energyCharge;
        private final double fixedCharge;
        private final double otherCharge;
        private final double total;
        private final LocalDate date;
        private final String month;

        BillRecord(String billId, String consumerId, String consumerName,
                   long previousReading, long currentReading, long units,
                   double energyCharge, double fixedCharge, double otherCharge,
                   double total, LocalDate date, String month) {
            this.billId = billId;
            this.consumerId = consumerId;
            this.consumerName = consumerName;
            this.previousReading = previousReading;
            this.currentReading = currentReading;
            this.units = units;
            this.energyCharge = energyCharge;
            this.fixedCharge = fixedCharge;
            this.otherCharge = otherCharge;
            this.total = total;
            this.date = date;
            this.month = month;
        }
    }

    private static class ConsumerSummary {
        private final String consumerId;
        private final String consumerName;
        private final int billCount;
        private final long units;
        private final double total;

        ConsumerSummary(String consumerId, String consumerName,
                        int billCount, long units, double total) {
            this.consumerId = consumerId;
            this.consumerName = consumerName;
            this.billCount = billCount;
            this.units = units;
            this.total = total;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
