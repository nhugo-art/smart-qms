package com.smartqms.gui;

import com.smartqms.model.Ticket;
import com.smartqms.model.TicketStatus;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class Ui {
    static final Color BACKGROUND = new Color(0xF2F6F7);
    static final Color INK = new Color(0x162C3B);
    static final Color MUTED = new Color(0x4D6371);
    static final Color TEAL = new Color(0x006D67);
    static final Color LINE = new Color(0xDFE8EB);
    static final Color ERROR = new Color(0xA32D32);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private Ui() {}

    static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        return panel;
    }

    static JPanel flow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 4));
        panel.setOpaque(false);
        return panel;
    }

    static JPanel stack() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    static JPanel heading(String title, String subtitle) {
        JPanel panel = stack();
        panel.add(label(title, 30, Font.BOLD, INK));
        panel.add(Box.createVerticalStrut(5));
        panel.add(label(subtitle, 14, Font.PLAIN, MUTED));
        return panel;
    }

    static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, style, size));
        label.setForeground(color);
        return label;
    }

    static JButton button(String title, boolean primary, Runnable action) {
        JButton button = new JButton(title);
        // Use consistent fill and focus painting across Swing look and feels.
        button.setUI(new BasicButtonUI() {
            @Override
            protected void paintFocus(Graphics graphics, AbstractButton focused, Rectangle view,
                                      Rectangle text, Rectangle icon) {
                graphics.setColor(focused.getForeground());
                graphics.drawRect(4, 4, focused.getWidth() - 9, focused.getHeight() - 9);
            }
        });
        button.setContentAreaFilled(true);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setBackground(primary ? TEAL : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : INK);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? TEAL : LINE),
                BorderFactory.createEmptyBorder(9, 13, 9, 13)));
        button.setRolloverEnabled(true);
        button.getModel().addChangeListener(event -> {
            boolean active = button.getModel().isRollover() || button.getModel().isArmed();
            button.setBackground(primary ? (active ? new Color(0x005953) : TEAL)
                    : (active ? new Color(0xE4F2F0) : Color.WHITE));
        });
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> action.run());
        return button;
    }

    static String timestamp(Instant time) { return DATE.format(time); }

    static String duration(Duration time) {
        long seconds = time.toSeconds();
        return seconds < 60 ? seconds + " s" : "%d min %02d s".formatted(seconds / 60, seconds % 60);
    }

    static String id(Ticket ticket) { return ticket == null ? "—" : ticket.id(); }

    static void message(JLabel label, String text, boolean error) {
        label.setForeground(error ? ERROR : MUTED);
        label.setText(text);
    }

    static final class DataTable extends JPanel {
        private final DefaultTableModel model;
        private final JLabel empty;
        private final CardLayout cards = new CardLayout();
        private final JPanel content = new JPanel(cards);

        DataTable(String title, String... columns) {
            super(new BorderLayout(0, 10));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            if (!title.isEmpty()) {
                add(label(title, 16, Font.BOLD, INK), BorderLayout.NORTH);
            }
            model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            JTable table = new JTable(model);
            table.setRowHeight(30);
            table.setFillsViewportHeight(true);
            table.setSelectionBackground(new Color(0xCBEAE6));
            table.setSelectionForeground(INK);
            table.setForeground(INK);
            table.setGridColor(LINE);
            table.setShowVerticalLines(false);
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                                                               boolean focused, int row, int column) {
                    Component cell = super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                    if (!selected) {
                        cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF5F9FA));
                        cell.setForeground(INK);
                    }
                    return cell;
                }
            };
            table.setDefaultRenderer(Object.class, renderer);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(LINE));
            content.add(scroll, "table");
            empty = label("", 14, Font.PLAIN, MUTED);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            content.setBackground(Color.WHITE);
            content.add(empty, "empty");
            add(content, BorderLayout.CENTER);
        }

        void rows(List<Object[]> rows, String emptyText) {
            model.setRowCount(0);
            rows.forEach(model::addRow);
            empty.setText(emptyText);
            cards.show(content, rows.isEmpty() ? "empty" : "table");
        }

        void tickets(List<Ticket> tickets, String emptyText, Instant now) {
            rows(tickets.stream().map(t -> new Object[] {
                    t.id(), t.service(), t.priority(), t.status(), timestamp(t.createdAt()),
                    t.status() == TicketStatus.WAITING ? duration(t.waitingTime(now)) : "—"
            }).toList(), emptyText);
        }
    }
}
