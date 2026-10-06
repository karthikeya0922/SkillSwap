package com.skillswap.common.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Clamps client supplied paging values to sane bounds. */
public final class PageRequests {

	public static final int MAX_SIZE = 50;

	private PageRequests() {
	}

	public static Pageable of(int page, int size, Sort sort) {
		return PageRequest.of(Math.max(0, page), Math.min(MAX_SIZE, Math.max(1, size)), sort);
	}

	public static Pageable of(int page, int size) {
		return of(page, size, Sort.unsorted());
	}
}
