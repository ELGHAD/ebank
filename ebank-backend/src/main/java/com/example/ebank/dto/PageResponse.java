package com.example.ebank.dto;

import java.util.List;

/** Stable JSON shape for paginated results (instead of serializing Spring's Page implementation). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
