package com.smartqms.gui;

import com.smartqms.service.QueueManager;

import javax.swing.*;
import java.awt.*;

public final class MainFrame extends JFrame {
    private final CardLayout layout = new CardLayout();
    private final JPanel screens = new JPanel(layout);
    private final CustomerPanel customer;
    private final AdminPanel admin;

    public MainFrame(QueueManager manager) {
        super("Smart QMS — University Service Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.INK);
        header.setBorder(BorderFactory.createEmptyBorder(13, 28, 13, 28));
        header.add(Ui.label("Smart QMS", 18, Font.BOLD, Color.WHITE), BorderLayout.WEST);
        JLabel location = Ui.label("University Service Center", 13, Font.PLAIN, new Color(0xD7E6EC));
        header.add(location, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        customer = new CustomerPanel(manager, () -> layout.show(screens, "roles"));
        admin = new AdminPanel(manager, () -> layout.show(screens, "roles"));
        screens.add(roleSelector(), "roles");
        screens.add(customer, "customer");
        screens.add(admin, "admin");
        add(screens, BorderLayout.CENTER);

        JLabel notice = Ui.label("Session data is cleared when the application restarts.", 12, Font.PLAIN, Ui.MUTED);
        notice.setBorder(BorderFactory.createEmptyBorder(8, 28, 10, 28));
        notice.setOpaque(true);
        notice.setBackground(Ui.BACKGROUND);
        add(notice, BorderLayout.SOUTH);
        layout.show(screens, "roles");
    }

    private JPanel roleSelector() {
        JPanel page = Ui.page();
        JPanel content = Ui.stack();
        content.add(Ui.heading("Choose a role", "University service center queue simulation"));
        content.add(Box.createVerticalStrut(25));
        JPanel choices = new JPanel(new GridLayout(1, 2, 16, 0));
        choices.setOpaque(false);
        choices.setAlignmentX(Component.LEFT_ALIGNMENT);
        choices.add(roleCard("Customer", "Get a ticket or check its status.", () -> {
            customer.showHome();
            layout.show(screens, "customer");
        }));
        choices.add(roleCard("Administrator", "Operate counters and review session reports.", () -> {
            admin.openDashboard();
            layout.show(screens, "admin");
        }));
        content.add(choices);
        page.add(content, BorderLayout.NORTH);
        return page;
    }

    private JPanel roleCard(String title, String description, Runnable open) {
        JPanel card = Ui.card();
        card.add(Ui.label(title, 22, Font.BOLD, Ui.INK), BorderLayout.NORTH);
        card.add(Ui.label(description, 14, Font.PLAIN, Ui.MUTED), BorderLayout.CENTER);
        JPanel actions = Ui.flow();
        actions.add(Ui.button("Continue as " + title, true, open));
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }
}
