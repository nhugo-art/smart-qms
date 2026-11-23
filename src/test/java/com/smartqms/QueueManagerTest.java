package com.smartqms;

import com.smartqms.dsa.SortingAlgorithms.Algorithm;
import com.smartqms.model.ServiceType;
import com.smartqms.model.Ticket;
import com.smartqms.model.TicketStatus;
import com.smartqms.service.QueueManager;
import com.smartqms.service.ReportService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;

import static com.smartqms.model.ServiceType.*;
import static com.smartqms.model.TicketPriority.*;
import static org.junit.jupiter.api.Assertions.*;

class QueueManagerTest {
    private final TestClock clock = new TestClock();
    private final QueueManager manager = new QueueManager(clock);
    private final ReportService reports = new ReportService(manager);

    @Test
    void counterUsesPriorityThenFifoAndKeepsServicesIndependent() {
        Ticket first = manager.create(FINANCE, LOW);
        Ticket second = manager.create(FINANCE, LOW);
        Ticket high = manager.create(FINANCE, HIGH);
        Ticket emergency = manager.create(FINANCE, EMERGENCY);
        Ticket laterEmergency = manager.create(FINANCE, EMERGENCY);
        Ticket medium = manager.create(FINANCE, MEDIUM);
        Ticket registration = manager.create(REGISTRATION, LOW);
        assertEquals("FIN-001", first.id());
        assertEquals("REG-001", registration.id());
        assertEquals(5, manager.position(first));
        for (Ticket expected : List.of(emergency, laterEmergency, high, medium, first, second)) {
            assertSame(expected, manager.callNext(FINANCE));
            assertEquals(TicketStatus.SERVING, expected.status());
            assertThrows(IllegalStateException.class, () -> manager.callNext(FINANCE));
            manager.complete(FINANCE);
            assertEquals(TicketStatus.COMPLETED, expected.status());
        }
        assertThrows(IllegalStateException.class, () -> manager.callNext(FINANCE));
        assertSame(registration, manager.callNext(REGISTRATION));
        assertSame(registration, manager.skip(REGISTRATION));
        assertEquals(TicketStatus.SKIPPED, manager.lookup(" reg-001 ").orElseThrow().status());
        assertThrows(IllegalStateException.class, () -> manager.complete(REGISTRATION));
        assertThrows(IllegalStateException.class, () -> manager.skip(REGISTRATION));
        assertThrows(IllegalStateException.class, () -> manager.returnToWaiting(REGISTRATION));
    }

    @Test
    void returnRestoresFifoAndPriorityOrderAndUsesLatestServiceStart() {
        Ticket low = manager.create(FINANCE, LOW);
        Ticket nextLow = manager.create(FINANCE, LOW);
        clock.advance(5);
        manager.callNext(FINANCE);
        assertSame(low, manager.returnToWaiting(FINANCE));
        assertNull(low.serviceStartedAt());
        assertEquals(List.of(low, nextLow), manager.view(FINANCE).fifo());
        Ticket high = manager.create(FINANCE, HIGH);
        manager.callNext(FINANCE);
        manager.create(FINANCE, HIGH);
        manager.returnToWaiting(FINANCE);
        assertSame(high, manager.view(FINANCE).next());
        manager.callNext(FINANCE);
        manager.skip(FINANCE);
        manager.callNext(FINANCE);
        manager.skip(FINANCE);
        clock.advance(7);
        assertSame(low, manager.callNext(FINANCE));
        manager.complete(FINANCE);
        assertEquals(12, reports.statistics().averageWaitSeconds().orElseThrow());
    }

    @Test
    void reportsSearchAndResetReflectRealSessionState() {
        assertTrue(reports.statistics().averageWaitSeconds().isEmpty());
        Ticket low = manager.create(FINANCE, LOW);
        Ticket high = manager.create(FINANCE, HIGH);
        clock.advance(12);
        for (Algorithm algorithm : Algorithm.values()) {
            assertEquals(List.of(high, low), reports.sorted(algorithm, ReportService.SortField.PRIORITY, clock.instant()));
            assertEquals(List.of(low, high), reports.sorted(algorithm, ReportService.SortField.WAITING, clock.instant()));
            assertSame(high, manager.view(FINANCE).next());
        }
        for (var method : ReportService.SearchMethod.values()) {
            assertSame(high, reports.search(high.id(), method).ticket());
            assertNull(reports.search("FIN-999", method).ticket());
        }
        assertThrows(IllegalArgumentException.class, () -> manager.lookup("   "));
        assertThrows(IllegalArgumentException.class, () -> manager.lookup("malformed"));
        manager.callNext(FINANCE);
        clock.advance(8);
        manager.complete(FINANCE);
        assertNull(reports.search(high.id(), ReportService.SearchMethod.LINEAR).ticket());
        assertSame(high, reports.search(high.id(), ReportService.SearchMethod.BINARY).ticket());
        assertSame(high, reports.search(high.id(), ReportService.SearchMethod.HASH).ticket());
        assertEquals(12, reports.statistics().averageWaitSeconds().orElseThrow());
        assertEquals(1, reports.statistics().byStatus().get(TicketStatus.WAITING));
        assertEquals(2, reports.statistics().byService().get(FINANCE));
        assertEquals(1, reports.statistics().byPriority().get(HIGH));
        assertEquals(clock.instant(), high.completedAt());
        manager.reset();
        assertTrue(manager.records().isEmpty());
        assertTrue(manager.lookup(low.id()).isEmpty());
        assertEquals(0, reports.statistics().total());
        assertEquals(low.id(), manager.create(FINANCE, LOW).id());
    }

    @Test
    void demoDataCoversEveryServiceAndPriorityAndKeepsIdsUnique() {
        assertEquals(26, manager.loadDemoData());
        assertEquals(26, new HashSet<>(manager.records().stream().map(Ticket::id).toList()).size());
        assertEquals(26, new HashSet<>(manager.records().stream().map(Ticket::arrivalSequence).toList()).size());
        for (var service : ServiceType.values()) {
            assertEquals(3, manager.counter(service).priorityWaiting());
            assertTrue(manager.counter(service).waiting() >= 4);
        }
        assertEquals(4, reports.statistics().byPriority().size());
    }

    private static final class TestClock extends Clock {
        private Instant time = Instant.EPOCH;

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(time, zone); }
        @Override public Instant instant() { return time; }
        private void advance(long seconds) { time = time.plusSeconds(seconds); }
    }
}
