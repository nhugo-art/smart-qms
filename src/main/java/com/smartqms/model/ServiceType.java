package com.smartqms.model;

public enum ServiceType {
    REGISTRATION("Registration", "REG", "Enrollment and course registration"),
    FINANCE("Finance", "FIN", "Tuition, payments, and account inquiries"),
    TRANSCRIPT("Transcript", "TRN", "Academic records and transcript requests"),
    STUDENT_ID("Student ID", "SID", "New cards and replacement student IDs"),
    IT_SUPPORT("IT Support", "ITS", "Campus accounts and technical help"),
    GENERAL_INFORMATION("General Information", "GEN", "Campus services and general inquiries");

    private final String label;
    private final String prefix;
    private final String description;

    ServiceType(String label, String prefix, String description) {
        this.label = label;
        this.prefix = prefix;
        this.description = description;
    }

    public String prefix() { return prefix; }
    public String description() { return description; }
    @Override public String toString() { return label; }
}
