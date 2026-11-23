package com.smartqms.dsa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class SortingAlgorithms {
    private SortingAlgorithms() {}

    public enum Algorithm {
        INSERTION("Insertion Sort", "Best O(n), average and worst O(n²)"),
        QUICK("Quick Sort", "Average O(n log n), worst O(n²)"),
        MERGE("Merge Sort", "O(n log n) time, O(n) auxiliary space");

        private final String label;
        private final String complexity;

        Algorithm(String label, String complexity) {
            this.label = label;
            this.complexity = complexity;
        }

        public String complexity() { return complexity; }
        @Override public String toString() { return label; }
    }

    public static <T> List<T> sortedCopy(List<T> source, Comparator<? super T> order, Algorithm algorithm) {
        List<T> items = new ArrayList<>(source);
        switch (algorithm) {
            case INSERTION -> insertion(items, order);
            case QUICK -> quick(items, order, 0, items.size() - 1);
            case MERGE -> mergeSort(items, new ArrayList<>(items), order, 0, items.size());
        }
        return List.copyOf(items);
    }

    private static <T> void insertion(List<T> items, Comparator<? super T> order) {
        for (int i = 1; i < items.size(); i++) {
            T value = items.get(i);
            int j = i;
            while (j > 0 && order.compare(items.get(j - 1), value) > 0) {
                items.set(j, items.get(j - 1));
                j--;
            }
            items.set(j, value);
        }
    }

    private static <T> void quick(List<T> items, Comparator<? super T> order, int first, int last) {
        while (first < last) {
            T pivot = items.get(first + (last - first) / 2);
            int lower = first;
            int index = first;
            int upper = last;
            while (index <= upper) {
                int relation = order.compare(items.get(index), pivot);
                if (relation < 0) {
                    Collections.swap(items, lower++, index++);
                } else if (relation > 0) {
                    Collections.swap(items, index, upper--);
                } else {
                    index++;
                }
            }
            // Recurse into the smaller partition to bound stack depth.
            if (lower - first < last - upper) {
                quick(items, order, first, lower - 1);
                first = upper + 1;
            } else {
                quick(items, order, upper + 1, last);
                last = lower - 1;
            }
        }
    }

    private static <T> void mergeSort(List<T> items, List<T> buffer, Comparator<? super T> order,
                                      int first, int end) {
        if (end - first < 2) {
            return;
        }
        int middle = first + (end - first) / 2;
        mergeSort(items, buffer, order, first, middle);
        mergeSort(items, buffer, order, middle, end);
        for (int i = first; i < end; i++) {
            buffer.set(i, items.get(i));
        }
        int left = first;
        int right = middle;
        for (int i = first; i < end; i++) {
            if (right == end || (left < middle && order.compare(buffer.get(left), buffer.get(right)) <= 0)) {
                items.set(i, buffer.get(left++));
            } else {
                items.set(i, buffer.get(right++));
            }
        }
    }
}
