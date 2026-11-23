package com.smartqms.model;

import com.smartqms.dsa.ArrayQueue;
import com.smartqms.dsa.BinaryMaxHeap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ServiceQueue {
    public static final Comparator<Ticket> HEAP_ORDER = Comparator
            .comparingInt((Ticket ticket) -> ticket.priority().rank())
            .thenComparing(Comparator.comparingLong(Ticket::arrivalSequence).reversed());

    private final ServiceType service;
    private final ArrayQueue<Ticket> fifo = new ArrayQueue<>();
    private final BinaryMaxHeap<Ticket> heap = new BinaryMaxHeap<>(HEAP_ORDER);
    private final List<Ticket> history = new ArrayList<>();
    private Ticket serving;

    public ServiceQueue(ServiceType service) { this.service = Objects.requireNonNull(service); }

    public List<Ticket> fifoSnapshot() { return fifo.snapshot(); }
    public List<Ticket> heapSnapshot() { return heap.snapshot(); }
    public List<Ticket> historySnapshot() { return List.copyOf(history); }
    public int waitingCount() { return fifo.size() + heap.size(); }
    public int priorityCount() { return heap.size(); }
    public Ticket serving() { return serving; }
    public Ticket next() { return heap.isEmpty() ? fifo.peek() : heap.peek(); }

    public void enqueue(Ticket ticket) {
        if (ticket.service() != service || ticket.status() != TicketStatus.WAITING) {
            throw new IllegalArgumentException("Only waiting tickets for " + service + " can join this queue.");
        }
        if (ticket.priority() == TicketPriority.LOW) {
            fifo.enqueue(ticket);
        } else {
            heap.insert(ticket);
        }
    }

    public Ticket callNext(Instant now) {
        if (serving != null) {
            throw new IllegalStateException(serving.id() + " is already being served at " + service + ".");
        }
        Ticket next = heap.isEmpty() ? fifo.dequeue() : heap.extractMax();
        if (next == null) {
            throw new IllegalStateException("No customers waiting for " + service + ".");
        }
        next.start(now);
        serving = next;
        return next;
    }

    public Ticket finish(TicketStatus result, Instant now) {
        if (result != TicketStatus.COMPLETED && result != TicketStatus.SKIPPED) {
            throw new IllegalArgumentException("Choose Completed or Skipped.");
        }
        Ticket ticket = requireServing();
        ticket.finish(result, now);
        history.add(ticket);
        serving = null;
        return ticket;
    }

    public Ticket returnToWaiting() {
        Ticket ticket = requireServing();
        ticket.returnToWaiting();
        if (ticket.priority() == TicketPriority.LOW) {
            // The counter removed the head of this FIFO when it called this ticket.
            fifo.restoreFirst(ticket);
        } else {
            heap.insert(ticket);
        }
        serving = null;
        return ticket;
    }

    public int position(Ticket ticket) {
        if (ticket.service() != service || ticket.status() != TicketStatus.WAITING) {
            throw new IllegalArgumentException("Queue position is available for waiting tickets in this service.");
        }
        if (ticket.priority() == TicketPriority.LOW) {
            return heap.size() + fifo.snapshot().indexOf(ticket) + 1;
        }
        return 1 + (int) heap.snapshot().stream()
                .filter(other -> HEAP_ORDER.compare(other, ticket) > 0)
                .count();
    }

    private Ticket requireServing() {
        if (serving == null) {
            throw new IllegalStateException("No ticket is currently serving at " + service + ".");
        }
        return serving;
    }
}
