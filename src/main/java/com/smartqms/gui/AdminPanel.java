package com.smartqms.gui;

import com.smartqms.dsa.SortingAlgorithms.Algorithm;
import com.smartqms.model.ServiceType;
import com.smartqms.model.Ticket;
import com.smartqms.model.TicketPriority;
import com.smartqms.model.TicketStatus;
import com.smartqms.service.QueueManager;
import com.smartqms.service.ReportService;
import com.smartqms.service.ReportService.SearchMethod;
import com.smartqms.service.ReportService.SortField;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class AdminPanel extends JPanel {
    private static final String[] TICKET_COLUMNS =
            {"Ticket ID", "Service", "Priority", "Status", "Created", "Time waiting"};

    private final QueueManager manager;
    private final ReportService reports;
    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);
    private final JComboBox<ServiceType> serviceChoice = new JComboBox<>(ServiceType.values());
    private final JLabel notice = Ui.label(" ", 13, Font.PLAIN, Ui.MUTED);
    private final JLabel counterMessage = Ui.label(" ", 13, Font.PLAIN, Ui.MUTED);
    private final JLabel serving = Ui.label(" ", 24, Font.BOLD, Ui.TEAL);
    private final JLabel next = Ui.label(" ", 14, Font.PLAIN, Ui.MUTED);
    private final JLabel queueCounts = Ui.label(" ", 13, Font.PLAIN, Ui.INK);
    private final JLabel dashboardTotals = Ui.label(" ", 16, Font.BOLD, Ui.INK);
    private final JLabel analyticsTotals = Ui.label(" ", 16, Font.BOLD, Ui.INK);
    private final JLabel averageWait = Ui.label(" ", 15, Font.PLAIN, Ui.MUTED);
    private final JLabel searchDetails = Ui.label(" ", 13, Font.PLAIN, Ui.MUTED);
    private final JLabel searchMessage = Ui.label(" ", 13, Font.PLAIN, Ui.MUTED);
    private final JLabel sortDetails = Ui.label(" ", 13, Font.PLAIN, Ui.MUTED);
    private final JTextField searchInput = new JTextField(14);
    private final JComboBox<SearchMethod> searchMethod = new JComboBox<>(SearchMethod.values());
    private final JComboBox<Algorithm> sortMethod = new JComboBox<>(Algorithm.values());
    private final JComboBox<SortField> sortField = new JComboBox<>(SortField.values());
    private final Ui.DataTable serviceTable = new Ui.DataTable("Service counters",
            "Service", "Waiting", "Priority waiting", "Now serving");
    private final Ui.DataTable fifo = new Ui.DataTable("Low FIFO queue", "Ticket ID", "Priority", "Created");
    private final Ui.DataTable heap = new Ui.DataTable("Priority heap · array order", "Ticket ID", "Priority", "Created");
    private final Ui.DataTable searchResults = new Ui.DataTable("Search result", TICKET_COLUMNS);
    private final Ui.DataTable reportTable = new Ui.DataTable("Session tickets", TICKET_COLUMNS);
    private final Ui.DataTable byService = new Ui.DataTable("Tickets by service", "Service", "Tickets");
    private final Ui.DataTable byPriority = new Ui.DataTable("Tickets by priority", "Priority", "Tickets");

    AdminPanel(QueueManager manager, Runnable roles) {
        super(new BorderLayout());
        this.manager = manager;
        reports = new ReportService(manager);

        JPanel navigation = Ui.flow();
        navigation.setOpaque(true);
        navigation.setBackground(Color.WHITE);
        navigation.add(Ui.button("Role selection", false, roles));
        navigation.add(Ui.button("Dashboard", false, this::openDashboard));
        navigation.add(Ui.button("Queues & counters", false, () -> display("queues", this::refreshQueue)));
        navigation.add(Ui.button("Search", false, () -> display("search", this::clearSearch)));
        navigation.add(Ui.button("Reports", false, () -> display("reports", this::refreshReport)));
        navigation.add(Ui.button("Analytics", false, () -> display("analytics", this::refreshAnalytics)));
        add(navigation, BorderLayout.NORTH);

        screens.add(dashboard(), "dashboard");
        screens.add(queues(), "queues");
        screens.add(search(), "search");
        screens.add(reports(), "reports");
        screens.add(analytics(), "analytics");
        add(screens, BorderLayout.CENTER);

        notice.setBorder(BorderFactory.createEmptyBorder(4, 28, 7, 28));
        notice.setOpaque(true);
        notice.setBackground(Ui.BACKGROUND);
        add(notice, BorderLayout.SOUTH);
    }

    void openDashboard() { display("dashboard", this::refreshDashboard); }

    private void display(String screen, Runnable refresh) {
        Ui.message(notice, " ", false);
        Ui.message(counterMessage, " ", false);
        refresh.run();
        cards.show(screens, screen);
    }

    private JPanel dashboard() {
        JPanel page = Ui.page();
        JPanel top = Ui.stack();
        top.add(Ui.heading("Administrator dashboard", "Monitor the six service counters in this session."));
        top.add(Box.createVerticalStrut(13));
        top.add(dashboardTotals);
        page.add(top, BorderLayout.NORTH);
        page.add(serviceTable, BorderLayout.CENTER);

        JPanel actions = Ui.flow();
        actions.add(Ui.button("Load Demo Data", false, () -> {
            int count = manager.loadDemoData();
            refreshDashboard();
            Ui.message(notice, "Added " + count + " demo tickets.", false);
        }));
        actions.add(Ui.button("Reset Simulation", false, this::reset));
        page.add(actions, BorderLayout.SOUTH);
        return page;
    }

    private JPanel queues() {
        JPanel page = Ui.page();
        JPanel top = Ui.stack();
        top.add(Ui.heading("Queues & counters", "Inspect waiting tickets and operate one counter at a time."));
        top.add(Box.createVerticalStrut(10));
        JPanel context = Ui.flow();
        JLabel serviceLabel = Ui.label("Service", 13, Font.BOLD, Ui.INK);
        serviceLabel.setLabelFor(serviceChoice);
        context.add(serviceLabel);
        context.add(serviceChoice);
        context.add(queueCounts);
        top.add(context);
        serviceChoice.addActionListener(event -> {
            Ui.message(counterMessage, " ", false);
            refreshQueue();
        });

        JPanel counter = Ui.card();
        JPanel status = Ui.stack();
        status.add(serving);
        status.add(next);
        counter.add(status, BorderLayout.NORTH);
        JPanel operations = Ui.flow();
        operations.add(Ui.button("Call Next", true,
                () -> operate(manager::callNext, "Called")));
        operations.add(Ui.button("Complete Serving Ticket", false,
                () -> operate(manager::complete, "Completed")));
        operations.add(Ui.button("Skip Serving Ticket", false,
                () -> operate(manager::skip, "Skipped")));
        operations.add(Ui.button("Return to Waiting", false,
                () -> operate(manager::returnToWaiting, "Returned to waiting")));
        counter.add(operations, BorderLayout.CENTER);
        counter.add(counterMessage, BorderLayout.SOUTH);
        counter.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(counter);

        page.add(top, BorderLayout.NORTH);
        page.add(pair(fifo, heap), BorderLayout.CENTER);
        return page;
    }

    private JPanel search() {
        JPanel page = Ui.page();
        JPanel top = Ui.stack();
        top.add(Ui.heading("Ticket search", "Find a ticket by its service-prefixed number."));
        top.add(Box.createVerticalStrut(10));
        JPanel controls = Ui.flow();
        JLabel idLabel = Ui.label("Ticket number", 13, Font.BOLD, Ui.INK);
        idLabel.setLabelFor(searchInput);
        controls.add(idLabel);
        controls.add(searchInput);
        JLabel methodLabel = Ui.label("Method", 13, Font.BOLD, Ui.INK);
        methodLabel.setLabelFor(searchMethod);
        controls.add(methodLabel);
        controls.add(searchMethod);
        controls.add(Ui.button("Search tickets", true, this::runSearch));
        top.add(controls);
        top.add(searchDetails);
        top.add(Box.createVerticalStrut(6));
        top.add(searchMessage);
        searchMethod.addActionListener(event -> clearSearch());
        searchInput.addActionListener(event -> runSearch());
        page.add(top, BorderLayout.NORTH);
        page.add(searchResults, BorderLayout.CENTER);
        return page;
    }

    private JPanel reports() {
        JPanel page = Ui.page();
        JPanel top = Ui.stack();
        top.add(Ui.heading("Ticket reports", "Sort a snapshot of the current session."));
        top.add(Box.createVerticalStrut(10));
        JPanel controls = Ui.flow();
        JLabel algorithmLabel = Ui.label("Algorithm", 13, Font.BOLD, Ui.INK);
        algorithmLabel.setLabelFor(sortMethod);
        controls.add(algorithmLabel);
        controls.add(sortMethod);
        JLabel fieldLabel = Ui.label("Sort by", 13, Font.BOLD, Ui.INK);
        fieldLabel.setLabelFor(sortField);
        controls.add(fieldLabel);
        controls.add(sortField);
        controls.add(Ui.button("Apply sort", true, this::refreshReport));
        sortMethod.addActionListener(event -> refreshReport());
        sortField.addActionListener(event -> refreshReport());
        top.add(controls);
        top.add(sortDetails);
        page.add(top, BorderLayout.NORTH);
        page.add(reportTable, BorderLayout.CENTER);
        return page;
    }

    private JPanel analytics() {
        JPanel page = Ui.page();
        JPanel top = Ui.stack();
        top.add(Ui.heading("Session analytics", "Ticket counts and average wait for completed tickets."));
        top.add(Box.createVerticalStrut(14));
        top.add(analyticsTotals);
        top.add(Box.createVerticalStrut(6));
        top.add(averageWait);
        page.add(top, BorderLayout.NORTH);
        page.add(pair(byService, byPriority), BorderLayout.CENTER);
        return page;
    }

    private void refreshDashboard() {
        dashboardTotals.setText(totals(reports.statistics()));
        List<Object[]> rows = new ArrayList<>();
        for (ServiceType service : ServiceType.values()) {
            QueueManager.CounterView state = manager.counter(service);
            rows.add(new Object[] {service, state.waiting(), state.priorityWaiting(), Ui.id(state.serving())});
        }
        serviceTable.rows(rows, "No service counters.");
    }

    private void refreshQueue() {
        var state = manager.view(selectedService());
        queueCounts.setText("%d waiting · %d priority waiting".formatted(
                state.fifo().size() + state.heap().size(), state.heap().size()));
        serving.setText(state.serving() == null ? "Counter available"
                : "Now serving " + state.serving().id() + " · " + state.serving().priority());
        next.setText(state.next() == null ? "No customers waiting."
                : "Next waiting ticket " + state.next().id() + " · " + state.next().priority());
        fifo.rows(queueRows(state.fifo()), "No Low-priority tickets waiting.");
        heap.rows(queueRows(state.heap()), "No priority tickets waiting.");
    }

    private List<Object[]> queueRows(List<Ticket> tickets) {
        return tickets.stream().map(t -> new Object[] {t.id(), t.priority(), Ui.timestamp(t.createdAt())}).toList();
    }

    private ServiceType selectedService() { return (ServiceType) serviceChoice.getSelectedItem(); }
    private SearchMethod selectedSearch() { return (SearchMethod) searchMethod.getSelectedItem(); }

    private void operate(Function<ServiceType, Ticket> operation, String action) {
        try {
            Ticket ticket = operation.apply(selectedService());
            refreshQueue();
            Ui.message(counterMessage, action + " " + ticket.id() + ".", false);
        } catch (IllegalStateException error) {
            Ui.message(counterMessage, error.getMessage(), true);
        }
    }

    private void clearSearch() {
        searchDetails.setText(selectedSearch().details());
        Ui.message(searchMessage, " ", false);
        searchResults.tickets(List.of(), "Enter a ticket number to search.", manager.now());
    }

    private void runSearch() {
        try {
            var result = reports.search(searchInput.getText(), selectedSearch());
            searchResults.tickets(result.ticket() == null ? List.of() : List.of(result.ticket()),
                    selectedSearch() == SearchMethod.LINEAR ? "No matching active ticket."
                            : "No matching ticket in this session.", manager.now());
            Ui.message(searchMessage, (result.ticket() == null ? "No match" : "Ticket found")
                    + " · " + result.effort(), result.ticket() == null);
        } catch (IllegalArgumentException error) {
            clearSearch();
            Ui.message(searchMessage, error.getMessage(), true);
        }
    }

    private void refreshReport() {
        Algorithm algorithm = (Algorithm) sortMethod.getSelectedItem();
        SortField field = (SortField) sortField.getSelectedItem();
        String order = switch (field) {
            case ID -> "Ticket ID ascending";
            case CREATED -> "Earliest creation first";
            case PRIORITY -> "Highest priority first";
            case WAITING -> "Longest wait first · Waiting tickets only";
        };
        sortDetails.setText(algorithm + " · " + algorithm.complexity() + " · " + order);
        Instant now = manager.now();
        reportTable.tickets(reports.sorted(algorithm, field, now),
                field == SortField.WAITING ? "No waiting tickets." : "No tickets in this session.", now);
    }

    private void refreshAnalytics() {
        var stats = reports.statistics();
        analyticsTotals.setText(totals(stats));
        averageWait.setText(stats.averageWaitSeconds().isPresent()
                ? String.format(Locale.ROOT, "Average wait %.1f seconds", stats.averageWaitSeconds().getAsDouble())
                : "Average wait available after a ticket is completed.");
        List<Object[]> services = new ArrayList<>();
        List<Object[]> priorities = new ArrayList<>();
        if (stats.total() > 0) {
            for (ServiceType service : ServiceType.values()) {
                services.add(new Object[] {service, stats.byService().get(service)});
            }
            for (TicketPriority priority : TicketPriority.values()) {
                priorities.add(new Object[] {priority, stats.byPriority().get(priority)});
            }
        }
        byService.rows(services, "No tickets in this session.");
        byPriority.rows(priorities, "No tickets in this session.");
    }

    private String totals(ReportService.Statistics stats) {
        var statuses = stats.byStatus();
        return "Created %d    ·    Waiting %d    ·    Serving %d    ·    Completed %d    ·    Skipped %d".formatted(
                stats.total(), statuses.get(TicketStatus.WAITING), statuses.get(TicketStatus.SERVING),
                statuses.get(TicketStatus.COMPLETED), statuses.get(TicketStatus.SKIPPED));
    }

    private void reset() {
        int decision = JOptionPane.showConfirmDialog(this, "Clear all tickets and restart ticket numbering?",
                "Reset Simulation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (decision == JOptionPane.OK_OPTION) {
            manager.reset();
            serviceChoice.setSelectedIndex(0);
            searchInput.setText("");
            searchMethod.setSelectedIndex(0);
            sortMethod.setSelectedIndex(0);
            sortField.setSelectedIndex(0);
            refreshDashboard();
            refreshQueue();
            clearSearch();
            refreshReport();
            refreshAnalytics();
            Ui.message(notice, "Simulation reset.", false);
        }
    }

    private static JPanel pair(JPanel left, JPanel right) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setOpaque(false);
        panel.add(left);
        panel.add(right);
        return panel;
    }
}
