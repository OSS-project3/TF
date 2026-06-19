package com.example.teamflow.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> items) {
        return new PageResponse<>(items, 0, items.size(), items.size(), 1);
    }
}
