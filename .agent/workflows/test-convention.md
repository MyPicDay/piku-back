---
description: 테스트 코드 작성 시 따라야 할 규칙
---

## 테스트 코드 작성 규칙

### 1. Given / When / Then 구분

- `// given`, `// when`, `// then` 주석을 사용하지 **않는다**.
- 대신 **빈 줄(개행)**으로 섹션을 구분한다.

```java
@Test
@DisplayName("키워드로 검색 시 결과가 정상 반환된다")
void returnsSearchResults() {
    String keyword = "피쿠";
    Pageable pageable = PageRequest.of(0, 10);
    given(repository.search(keyword)).willReturn(List.of(user));

    Page<UserSearchResult> result = service.search(keyword, pageable);

    assertThat(result.getContent()).hasSize(1);
}
```

### 2. 테스트 구조

- `@ExtendWith(MockitoExtension.class)` 사용
- `@Nested` + `@DisplayName`으로 메서드별 그룹핑
- AssertJ (`assertThat`) 사용
- BDDMockito (`given`, `then`) 사용

### 3. API 응답 테스트 규칙

- API 오류 응답을 변경하거나 추가할 때는 RFC 9457 / Problem Details 형식을 기본으로 검증한다.
- 최소 검증 항목은 `status`, `detail`이며, `instance`, `type`, `code` 같은 확장 필드를 사용한다면 함께 검증한다.
- 하위 호환 때문에 임시 예외를 둘 경우, 왜 예외가 필요한지와 최종적으로 Problem Details로 수렴할 계획을 테스트 또는 문서에서 드러내야 한다.
