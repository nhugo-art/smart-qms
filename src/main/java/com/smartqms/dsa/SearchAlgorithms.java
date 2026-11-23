package com.smartqms.dsa;

import java.util.List;
import java.util.function.Function;

public final class SearchAlgorithms {
    private SearchAlgorithms() {}

    public record Result<T>(T item, int comparisons) {}

    public static <T> Result<T> linear(List<T> items, String target, Function<T, String> key) {
        int comparisons = 0;
        for (T item : items) {
            comparisons++;
            if (key.apply(item).equals(target)) {
                return new Result<>(item, comparisons);
            }
        }
        return new Result<>(null, comparisons);
    }

    /** Requires random-access items in ascending lexicographic key order. */
    public static <T> Result<T> binary(List<T> items, String target, Function<T, String> key) {
        int left = 0;
        int right = items.size() - 1;
        int comparisons = 0;
        while (left <= right) {
            int middle = left + (right - left) / 2;
            T item = items.get(middle);
            int relation = key.apply(item).compareTo(target);
            comparisons++;
            if (relation == 0) {
                return new Result<>(item, comparisons);
            }
            if (relation < 0) {
                left = middle + 1;
            } else {
                right = middle - 1;
            }
        }
        return new Result<>(null, comparisons);
    }
}
