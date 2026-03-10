package com.pikume.back.global.pagination;

public record SortQuery(
		String property,
		SortDirection direction
) {

	public static SortQuery asc(String property) {
		return new SortQuery(property, SortDirection.ASC);
	}

	public static SortQuery desc(String property) {
		return new SortQuery(property, SortDirection.DESC);
	}
}
