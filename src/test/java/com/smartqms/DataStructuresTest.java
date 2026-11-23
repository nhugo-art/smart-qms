package com.smartqms;

import com.smartqms.dsa.ArrayQueue;
import com.smartqms.dsa.BinaryMaxHeap;
import com.smartqms.dsa.SearchAlgorithms;
import com.smartqms.dsa.SortingAlgorithms;
import com.smartqms.model.ServiceQueue;
import com.smartqms.model.ServiceType;
import com.smartqms.model.Ticket;
import com.smartqms.model.TicketPriority;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DataStructuresTest {
    @Test
    void fifoWrapsAndGrowsWithoutLosingOrder() {
        var queue = new ArrayQueue<Integer>();
        assertNull(queue.peek());
        assertNull(queue.dequeue());
        IntStream.range(0, 8).forEach(queue::enqueue);
        assertEquals(0, queue.peek());
        assertEquals(8, queue.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(i, queue.dequeue());
        }
        IntStream.range(8, 20).forEach(queue::enqueue);
        assertEquals(IntStream.range(4, 20).boxed().toList(), queue.snapshot());
        queue.restoreFirst(3);
        for (int i = 3; i < 20; i++) {
            assertEquals(i, queue.dequeue());
        }
        assertTrue(queue.isEmpty());
        queue.enqueue(7);
        queue.clear();
        assertEquals(List.of(), queue.snapshot());
    }

    @Test
    void heapOrdersByPriorityAndThenArrival() {
        var heap = new BinaryMaxHeap<>(ServiceQueue.HEAP_ORDER);
        Ticket low = ticket(TicketPriority.LOW, 0);
        Ticket medium = ticket(TicketPriority.MEDIUM, 1);
        Ticket high = ticket(TicketPriority.HIGH, 2);
        Ticket earlier = ticket(TicketPriority.EMERGENCY, 3);
        Ticket later = ticket(TicketPriority.EMERGENCY, 4);
        List<Ticket> input = List.of(later, low, high, earlier, medium);
        assertNull(heap.extractMax());
        input.forEach(t -> {
            heap.insert(t);
            assertHeapProperty(heap);
        });
        assertSame(earlier, heap.peek());
        assertEquals(5, heap.size());
        for (Ticket expected : List.of(earlier, later, high, medium, low)) {
            assertSame(expected, heap.extractMax());
            assertHeapProperty(heap);
        }
        assertTrue(heap.isEmpty());
        heap.heapify(input);
        assertHeapProperty(heap);
        assertSame(earlier, heap.extractMax());
        heap.clear();
        assertNull(heap.peek());
    }

    private static Ticket ticket(TicketPriority priority, long sequence) {
        return new Ticket("FIN-" + sequence, ServiceType.FINANCE, priority, Instant.EPOCH, sequence);
    }

    private static void assertHeapProperty(BinaryMaxHeap<Ticket> heap) {
        List<Ticket> items = heap.snapshot();
        for (int i = 1; i < items.size(); i++) {
            assertTrue(ServiceQueue.HEAP_ORDER.compare(items.get((i - 1) / 2), items.get(i)) >= 0);
        }
    }

    @Test
    void searchFindsExactKeysAndHandlesMisses() {
        List<String> unsorted = List.of("FIN-003", "FIN-001", "FIN-002");
        assertEquals(new SearchAlgorithms.Result<>("FIN-001", 2),
                SearchAlgorithms.linear(unsorted, "FIN-001", s -> s));
        assertEquals(new SearchAlgorithms.Result<>(null, 3),
                SearchAlgorithms.linear(unsorted, "FIN-004", s -> s));
        List<String> sorted = List.of("FIN-001", "FIN-002", "FIN-003");
        for (String id : sorted) {
            assertEquals(id, SearchAlgorithms.binary(sorted, id, s -> s).item());
        }
        assertNull(SearchAlgorithms.binary(sorted, "FIN-004", s -> s).item());
        assertEquals(0, SearchAlgorithms.binary(List.<String>of(), "FIN-001", s -> s).comparisons());
    }

    @Test
    void allSortsHandleEmptySingletonDuplicatesAndLargerInputs() {
        List<Integer> random = new Random(18).ints(1000, -30, 30).boxed().toList();
        for (var algorithm : SortingAlgorithms.Algorithm.values()) {
            for (List<Integer> input : List.of(List.<Integer>of(), List.of(4), List.of(4, 2, 4, 1), random)) {
                List<Integer> source = new ArrayList<>(input);
                assertEquals(input.stream().sorted().toList(),
                        SortingAlgorithms.sortedCopy(source, Comparator.naturalOrder(), algorithm));
                assertEquals(input, source);
            }
        }
    }
}
