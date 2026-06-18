package com.miguelpimenta.buildlog.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope. We map Spring Data's {@link Page} into this
 * rather than serializing a {@code PageImpl} directly, whose JSON shape Spring
 * warns is unstable across versions.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
