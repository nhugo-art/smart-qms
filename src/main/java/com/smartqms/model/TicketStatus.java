package com.smartqms.model;

public enum TicketStatus {
    WAITING("Waiting"), SERVING("Serving"), COMPLETED("Completed"), SKIPPED("Skipped");

    private final String label;
    TicketStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
