package com.smartqms.gui;

import com.smartqms.model.ServiceType;
import com.smartqms.model.Ticket;
import com.smartqms.model.TicketPriority;
import com.smartqms.model.TicketStatus;
import com.smartqms.service.QueueManager;

import javax.swing.*;
import java.awt.*;

final class CustomerPanel extends JPanel {
    private final QueueManager manager;
    private final JPanel body = new JPanel(new BorderLayout());

    CustomerPanel(QueueManager manager, Runnable roles) {
        super(new BorderLayout());
        this.manager = manager;
        JPanel navigation = Ui.flow();
        navigation.setOpaque(true);
        navigation.setBackground(Color.WHITE);
        navigation.add(Ui.button("Role selection", false, roles));
        navigation.add(Ui.button("Services", false, this::showHome));
        navigation.add(Ui.button("Check ticket status", false, this::showLookup));
        add(navigation, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    void showHome() {
        JPanel page = Ui.page();
        page.add(Ui.heading("Choose a service", "Select the university service you need."), BorderLayout.NORTH);
        JPanel services = new JPanel(new GridLayout(2, 3, 14, 14));
        services.setOpaque(false);
        for (ServiceType service : ServiceType.values()) {
            var state = manager.counter(service);
            JPanel card = Ui.card();
            card.add(Ui.label(service.toString(), 18, Font.BOLD, Ui.INK), BorderLayout.NORTH);
            JPanel details = Ui.stack();
            details.add(Ui.label(service.description(), 13, Font.PLAIN, Ui.MUTED));
            details.add(Box.createVerticalStrut(9));
            details.add(Ui.label("Waiting %d   ·   Priority %d".formatted(state.waiting(), state.priorityWaiting()),
                    13, Font.PLAIN, Ui.INK));
            details.add(Ui.label("Now serving " + Ui.id(state.serving()), 13, Font.PLAIN, Ui.MUTED));
            card.add(details, BorderLayout.CENTER);
            card.add(Ui.button("Select " + service, false, () -> showPriority(service)), BorderLayout.SOUTH);
            services.add(card);
        }
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(services, BorderLayout.NORTH);
        page.add(content, BorderLayout.CENTER);
        show(page);
    }

    private void showPriority(ServiceType service) {
        JPanel page = Ui.page();
        page.add(Ui.heading(service.toString(), service.description()), BorderLayout.NORTH);
        JPanel card = Ui.card();
        JLabel prompt = Ui.label("Priority (simulation)", 18, Font.BOLD, Ui.INK);
        JComboBox<TicketPriority> priorities = new JComboBox<>(TicketPriority.values());
        prompt.setLabelFor(priorities);
        card.add(prompt, BorderLayout.NORTH);
        JPanel controls = Ui.flow();
        controls.add(priorities);
        controls.add(Ui.button("Generate ticket", true,
                () -> showConfirmation(manager.create(service, (TicketPriority) priorities.getSelectedItem()))));
        card.add(controls, BorderLayout.CENTER);
        card.add(Ui.label("Priority tickets are called before the Low FIFO queue.", 13, Font.PLAIN, Ui.MUTED),
                BorderLayout.SOUTH);
        placeCard(page, card);
        show(page);
    }

    private void showConfirmation(Ticket ticket) {
        JPanel page = Ui.page();
        page.add(Ui.heading("Ticket created", "Keep your ticket number to check your status during this session."),
                BorderLayout.NORTH);
        JPanel card = ticketCard(ticket);
        JPanel actions = Ui.flow();
        actions.add(Ui.button("Check this ticket", true, () -> showLookup(ticket.id())));
        actions.add(Ui.button("Choose another service", false, this::showHome));
        card.add(actions, BorderLayout.SOUTH);
        placeCard(page, card);
        show(page);
    }

    private void showLookup() { showLookup(""); }

    private void showLookup(String initialId) {
        JPanel page = Ui.page();
        page.add(Ui.heading("Check ticket status", "Enter a ticket number from this session."), BorderLayout.NORTH);
        JPanel content = Ui.stack();
        JPanel form = Ui.card();
        JPanel controls = Ui.flow();
        JLabel caption = Ui.label("Ticket number", 13, Font.BOLD, Ui.INK);
        JTextField input = new JTextField(initialId, 16);
        caption.setLabelFor(input);
        controls.add(caption);
        controls.add(input);
        JLabel message = Ui.label(" ", 13, Font.PLAIN, Ui.MUTED);
        JPanel result = new JPanel(new BorderLayout());
        result.setOpaque(false);
        Runnable lookup = () -> {
            result.removeAll();
            try {
                Ticket ticket = manager.lookup(input.getText()).orElse(null);
                if (ticket == null) {
                    Ui.message(message, "No ticket with that number was found in this session.", true);
                } else {
                    Ui.message(message, " ", false);
                    result.add(ticketCard(ticket), BorderLayout.NORTH);
                }
            } catch (IllegalArgumentException error) {
                Ui.message(message, error.getMessage(), true);
            }
            result.revalidate();
            result.repaint();
        };
        controls.add(Ui.button("Find ticket", true, lookup));
        input.addActionListener(event -> lookup.run());
        form.add(controls, BorderLayout.CENTER);
        form.add(message, BorderLayout.SOUTH);
        content.add(form);
        content.add(Box.createVerticalStrut(12));
        content.add(result);
        placeCard(page, content);
        show(page);
        if (!initialId.isEmpty()) {
            lookup.run();
        }
        input.requestFocusInWindow();
    }

    private JPanel ticketCard(Ticket ticket) {
        JPanel card = Ui.card();
        card.add(Ui.label(ticket.id(), 38, Font.BOLD, Ui.TEAL), BorderLayout.NORTH);
        JPanel details = Ui.stack();
        details.add(Ui.label(ticket.service() + "   ·   " + ticket.priority() + " priority   ·   " + ticket.status(),
                17, Font.BOLD, Ui.INK));
        details.add(Box.createVerticalStrut(6));
        details.add(Ui.label("Created " + Ui.timestamp(ticket.createdAt()), 13, Font.PLAIN, Ui.MUTED));
        if (ticket.status() == TicketStatus.WAITING) {
            details.add(Ui.label("Estimated position " + manager.position(ticket), 15, Font.BOLD, Ui.TEAL));
        }
        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private static void placeCard(JPanel page, JPanel card) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.NORTH);
        page.add(wrapper, BorderLayout.CENTER);
    }

    private void show(JPanel page) {
        body.removeAll();
        body.add(page, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }
}
