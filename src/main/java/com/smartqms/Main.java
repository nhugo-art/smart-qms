package com.smartqms;

import com.smartqms.gui.MainFrame;
import com.smartqms.service.QueueManager;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame(new QueueManager()).setVisible(true));
    }
}
