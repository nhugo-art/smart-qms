package com.smartqms.dsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArrayQueue<T> {
    private Object[] elements = new Object[8];
    private int head;
    private int size;

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void enqueue(T item) {
        Objects.requireNonNull(item);
        ensureCapacity();
        elements[index(size++)] = item;
    }

    public void restoreFirst(T item) {
        Objects.requireNonNull(item);
        ensureCapacity();
        head = (head + elements.length - 1) % elements.length;
        elements[head] = item;
        size++;
    }

    private void ensureCapacity() {
        if (size == elements.length) {
            Object[] expanded = new Object[elements.length * 2];
            for (int i = 0; i < size; i++) {
                expanded[i] = elements[index(i)];
            }
            elements = expanded;
            head = 0;
        }
    }

    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        T item = at(0);
        elements[head] = null;
        head = index(1);
        size--;
        return item;
    }

    public T peek() { return isEmpty() ? null : at(0); }

    public void clear() {
        elements = new Object[8];
        head = 0;
        size = 0;
    }

    public List<T> snapshot() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(at(i));
        }
        return List.copyOf(result);
    }

    private int index(int offset) { return (head + offset) % elements.length; }

    @SuppressWarnings("unchecked")
    private T at(int offset) { return (T) elements[index(offset)]; }
}
