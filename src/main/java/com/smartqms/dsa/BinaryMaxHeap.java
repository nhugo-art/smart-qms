package com.smartqms.dsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class BinaryMaxHeap<T> {
    private final ArrayList<T> elements = new ArrayList<>();
    private final Comparator<? super T> order;

    public BinaryMaxHeap(Comparator<? super T> order) {
        this.order = Objects.requireNonNull(order);
    }

    public int size() { return elements.size(); }
    public boolean isEmpty() { return elements.isEmpty(); }
    public T peek() { return isEmpty() ? null : elements.getFirst(); }
    public void clear() { elements.clear(); }
    public List<T> snapshot() { return List.copyOf(elements); }

    public void insert(T item) {
        elements.add(Objects.requireNonNull(item));
        int child = size() - 1;
        while (child > 0) {
            int parent = (child - 1) / 2;
            if (!greater(child, parent)) {
                break;
            }
            Collections.swap(elements, child, parent);
            child = parent;
        }
    }

    public T extractMax() {
        T max = peek();
        if (max != null) {
            T last = elements.removeLast();
            if (!isEmpty()) {
                elements.set(0, last);
                siftDown(0);
            }
        }
        return max;
    }

    public void heapify(List<? extends T> items) {
        List<T> copy = List.copyOf(items);
        elements.clear();
        elements.addAll(copy);
        for (int i = size() / 2 - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    private void siftDown(int parent) {
        while (parent < size() / 2) {
            int child = parent * 2 + 1;
            if (child + 1 < size() && greater(child + 1, child)) {
                child++;
            }
            if (!greater(child, parent)) {
                return;
            }
            Collections.swap(elements, child, parent);
            parent = child;
        }
    }

    private boolean greater(int a, int b) {
        return order.compare(elements.get(a), elements.get(b)) > 0;
    }
}
