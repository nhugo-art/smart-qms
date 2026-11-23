package com.smartqms.service;

import com.smartqms.model.ServiceQueue;
import com.smartqms.model.ServiceType;
import com.smartqms.model.Ticket;
import com.smartqms.model.TicketPriority;
import com.smartqms.model.TicketStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Session state. The Swing application accesses it on the event dispatch thread. */
public final class QueueManager {
    private static final Pattern TICKET_ID = Pattern.compile("[A-Z]{3}-[0-9]{3,}");
    private final Clock clock;
    private final Map<ServiceType, ServiceQueue> queues = new EnumMap<>(ServiceType.class);
    private final Map<ServiceType, Long> sequences = new EnumMap<>(ServiceType.class);
    private final Map<String, Ticket> ticketsById = new HashMap<>();
    private final List<Ticket> records = new ArrayList<>();
    private long arrivalSequence;

    public record QueueView(List<Ticket> fifo, List<Ticket> heap, Ticket serving, Ticket next) {}
    public record CounterView(int waiting, int priorityWaiting, Ticket serving) {}

    public QueueManager() { this(Clock.systemUTC()); }

    public QueueManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        reset();
    }

    public Instant now() { return clock.instant(); }

    public Ticket create(ServiceType service, TicketPriority priority) {
        ServiceQueue queue = queue(service);
        Objects.requireNonNull(priority);
        long sequence = sequences.get(service) + 1;
        String id = String.format(Locale.ROOT, "%s-%03d", service.prefix(), sequence);
        Ticket ticket = new Ticket(id, service, priority, now(), arrivalSequence + 1);
        if (ticketsById.putIfAbsent(id, ticket) != null) {
            throw new IllegalStateException("Ticket number already exists.");
        }
        queue.enqueue(ticket);
        sequences.put(service, sequence);
        arrivalSequence++;
        records.add(ticket);
        return ticket;
    }

    public Optional<Ticket> lookup(String input) {
        return Optional.ofNullable(ticketsById.get(normalizeId(input)));
    }

    static String normalizeId(String input) {
        String id = input == null ? "" : input.strip().toUpperCase(Locale.ROOT);
        if (!TICKET_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Enter a ticket number such as FIN-001.");
        }
        return id;
    }

    public List<Ticket> records() { return List.copyOf(records); }

    public QueueView view(ServiceType service) {
        ServiceQueue queue = queue(service);
        return new QueueView(queue.fifoSnapshot(), queue.heapSnapshot(), queue.serving(), queue.next());
    }

    public CounterView counter(ServiceType service) {
        ServiceQueue queue = queue(service);
        return new CounterView(queue.waitingCount(), queue.priorityCount(), queue.serving());
    }

    public Ticket callNext(ServiceType service) { return queue(service).callNext(now()); }
    public Ticket complete(ServiceType service) { return queue(service).finish(TicketStatus.COMPLETED, now()); }
    public Ticket skip(ServiceType service) { return queue(service).finish(TicketStatus.SKIPPED, now()); }
    public Ticket returnToWaiting(ServiceType service) { return queue(service).returnToWaiting(); }

    public int position(Ticket ticket) {
        if (ticketsById.get(ticket.id()) != ticket) {
            throw new IllegalArgumentException("Ticket is no longer in this session.");
        }
        return queue(ticket.service()).position(ticket);
    }

    public void reset() {
        ticketsById.clear();
        records.clear();
        arrivalSequence = 0;
        for (ServiceType service : ServiceType.values()) {
            queues.put(service, new ServiceQueue(service));
            sequences.put(service, 0L);
        }
    }

    public int loadDemoData() {
        int count = 0;
        for (ServiceType service : ServiceType.values()) {
            for (TicketPriority priority : TicketPriority.values()) {
                create(service, priority);
                count++;
            }
        }
        create(ServiceType.FINANCE, TicketPriority.LOW);
        create(ServiceType.FINANCE, TicketPriority.LOW);
        return count + 2;
    }

    private ServiceQueue queue(ServiceType service) {
        return queues.get(Objects.requireNonNull(service));
    }
}
