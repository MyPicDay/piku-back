package com.pikume.back.global.pagination;

import java.util.List;

public record PageQuery(
		int page,
		int size,
		List<SortQuery> sortOrders
) {

	public PageQuery {
		sortOrders = sortOrders == null ? List.of() : List.copyOf(sortOrders);
	}

	public static PageQuery of(int page, int size) {
		return new PageQuery(page, size, List.of());
	}

	public static PageQuery of(int page, int size, List<SortQuery> sortOrders) {
		return new PageQuery(page, size, sortOrders);
	}
}
