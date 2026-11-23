package com.smartqms.service;

import com.smartqms.dsa.SearchAlgorithms;
import com.smartqms.dsa.SortingAlgorithms;
import com.smartqms.model.ServiceType;
import com.smartqms.model.Ticket;
import com.smartqms.model.TicketPriority;
import com.smartqms.model.TicketStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public final class ReportService {
    private static final Comparator<Ticket> BY_ID = Comparator.comparing(Ticket::id);
    private final QueueManager manager;

    public enum SearchMethod {
        LINEAR("Linear Search", "Active tickets · O(n) search"),
        BINARY("Binary Search", "All tickets · O(log n) search after O(n log n) sorting by ticket ID"),
        HASH("Hash Lookup", "All tickets · HashMap lookup averages O(1), with O(n) worst-case bound");

        private final String label;
        private final String details;
        SearchMethod(String label, String details) {
            this.label = label;
            this.details = details;
        }
        public String details() { return details; }
        @Override public String toString() { return label; }
    }

    public enum SortField {
        ID("Ticket ID"), CREATED("Creation time"), PRIORITY("Priority"), WAITING("Waiting duration");

        private final String label;
        SortField(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public record SearchResult(Ticket ticket, String effort) {}
    public record Statistics(int total, Map<TicketStatus, Integer> byStatus, Map<ServiceType, Integer> byService,
                             Map<TicketPriority, Integer> byPriority, OptionalDouble averageWaitSeconds) {}

    public ReportService(QueueManager manager) { this.manager = manager; }

    public SearchResult search(String input, SearchMethod method) {
        String id = QueueManager.normalizeId(input);
        if (method == SearchMethod.HASH) {
            return new SearchResult(manager.lookup(id).orElse(null), "1 index lookup");
        }
        List<Ticket> records = manager.records();
        SearchAlgorithms.Result<Ticket> result = switch (method) {
            case LINEAR -> SearchAlgorithms.linear(records.stream()
                    .filter(t -> t.status() == TicketStatus.WAITING || t.status() == TicketStatus.SERVING)
                    .toList(), id, Ticket::id);
            case BINARY -> SearchAlgorithms.binary(
                    SortingAlgorithms.sortedCopy(records, BY_ID, SortingAlgorithms.Algorithm.MERGE), id, Ticket::id);
            case HASH -> throw new AssertionError("Hash lookup was already handled.");
        };
        return new SearchResult(result.item(), result.comparisons() + " ticket ID comparisons");
    }

    public List<Ticket> sorted(SortingAlgorithms.Algorithm algorithm, SortField field, Instant now) {
        List<Ticket> records = manager.records();
        Comparator<Ticket> order = switch (field) {
            case ID -> BY_ID;
            case CREATED -> Comparator.comparing(Ticket::createdAt);
            case PRIORITY -> Comparator.comparingInt((Ticket t) -> t.priority().rank()).reversed();
            case WAITING -> Comparator.comparing((Ticket t) -> t.waitingTime(now)).reversed();
        };
        if (field == SortField.WAITING) {
            records = records.stream().filter(t -> t.status() == TicketStatus.WAITING).toList();
        }
        return SortingAlgorithms.sortedCopy(records, order.thenComparingLong(Ticket::arrivalSequence), algorithm);
    }

    public Statistics statistics() {
        var statuses = zeroCounts(TicketStatus.class);
        var services = zeroCounts(ServiceType.class);
        var priorities = zeroCounts(TicketPriority.class);
        List<Ticket> records = manager.records();
        double totalWait = 0;
        for (Ticket ticket : records) {
            statuses.merge(ticket.status(), 1, Integer::sum);
            services.merge(ticket.service(), 1, Integer::sum);
            priorities.merge(ticket.priority(), 1, Integer::sum);
            if (ticket.status() == TicketStatus.COMPLETED) {
                Duration wait = ticket.waitingTime(ticket.serviceStartedAt());
                totalWait += wait.toSeconds() + wait.toNanosPart() / 1_000_000_000.0;
            }
        }
        int completed = statuses.get(TicketStatus.COMPLETED);
        return new Statistics(records.size(), Map.copyOf(statuses), Map.copyOf(services), Map.copyOf(priorities),
                completed == 0 ? OptionalDouble.empty() : OptionalDouble.of(totalWait / completed));
    }

    private static <E extends Enum<E>> EnumMap<E, Integer> zeroCounts(Class<E> type) {
        EnumMap<E, Integer> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0);
        }
        return counts;
    }
}
