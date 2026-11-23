package com.smartqms.model;

public enum TicketPriority {
    LOW(1, "Low"), MEDIUM(2, "Medium"), HIGH(3, "High"), EMERGENCY(4, "Emergency");

    private final int rank;
    private final String label;

    TicketPriority(int rank, String label) {
        this.rank = rank;
        this.label = label;
    }

    public int rank() { return rank; }
    @Override public String toString() { return label; }
}
