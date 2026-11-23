package com.smartqms.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class Ticket {
    private final String id;
    private final ServiceType service;
    private final TicketPriority priority;
    private final Instant createdAt;
    private final long arrivalSequence;
    private TicketStatus status = TicketStatus.WAITING;
    private Instant serviceStartedAt;
    private Instant completedAt;

    public Ticket(String id, ServiceType service, TicketPriority priority, Instant createdAt, long arrivalSequence) {
        this.id = Objects.requireNonNull(id);
        this.service = Objects.requireNonNull(service);
        this.priority = Objects.requireNonNull(priority);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.arrivalSequence = arrivalSequence;
    }

    public String id() { return id; }
    public ServiceType service() { return service; }
    public TicketPriority priority() { return priority; }
    public Instant createdAt() { return createdAt; }
    public long arrivalSequence() { return arrivalSequence; }
    public TicketStatus status() { return status; }
    public Instant serviceStartedAt() { return serviceStartedAt; }
    public Instant completedAt() { return completedAt; }

    void start(Instant now) {
        serviceStartedAt = now;
        status = TicketStatus.SERVING;
    }

    void finish(TicketStatus result, Instant now) {
        status = result;
        if (result == TicketStatus.COMPLETED) {
            completedAt = now;
        }
    }

    void returnToWaiting() {
        serviceStartedAt = null;
        status = TicketStatus.WAITING;
    }

    public Duration waitingTime(Instant now) {
        Duration duration = Duration.between(createdAt, serviceStartedAt == null ? now : serviceStartedAt);
        return duration.isNegative() ? Duration.ZERO : duration;
    }
}
