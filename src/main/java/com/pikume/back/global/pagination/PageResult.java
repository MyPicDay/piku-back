package com.pikume.back.global.pagination;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public record PageResult<T>(
		List<T> content,
		int page,
		int size,
		long totalElements
) {

	public PageResult {
		content = content == null ? List.of() : List.copyOf(content);
	}

	public int totalPages() {
		if (size <= 0) {
			return 0;
		}
		return (int) Math.ceil((double) totalElements / size);
	}

	public int numberOfElements() {
		return content.size();
	}

	public boolean first() {
		return page <= 0;
	}

	public boolean last() {
		return page + 1 >= totalPages();
	}

	public boolean empty() {
		return content.isEmpty();
	}

	public long getTotalElements() {
		return totalElements;
	}

	public List<T> getContent() {
		return content;
	}

	public int getNumber() {
		return page;
	}

	public int getSize() {
		return size;
	}

	public Stream<T> stream() {
		return content.stream();
	}

	public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
		return new PageResult<>(
				content.stream().map(mapper).collect(Collectors.toList()),
				page,
				size,
				totalElements);
	}

	public static <T> PageResult<T> empty(PageQuery pageQuery) {
		return new PageResult<>(List.of(), pageQuery.page(), pageQuery.size(), 0);
	}
}
