package com.skillswap.common.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
		boolean last) {

	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages(), page.isLast());
	}

	public static <T> PageResponse<T> of(List<T> all, int page, int size) {
		int safeSize = Math.max(1, size);
		int from = Math.min(all.size(), Math.max(0, page) * safeSize);
		int to = Math.min(all.size(), from + safeSize);
		int totalPages = (int) Math.ceil(all.size() / (double) safeSize);
		return new PageResponse<>(all.subList(from, to), page, safeSize, all.size(), totalPages,
				page >= totalPages - 1);
	}
}
