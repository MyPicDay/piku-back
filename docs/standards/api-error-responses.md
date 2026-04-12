# API Error Responses

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-04-12

## 목적

이 문서는 `piku-back`의 API 실패 응답 표준을 정의한다.

## 기준

- 실패 응답은 RFC 9457 / Problem Details를 따른다.
- ad hoc 문자열 body와 bespoke error DTO는 새로 추가하지 않는다.
- 임시 호환이 필요하면 문서화된 예외로만 허용한다.

## 현재 구현 기준점

현재 공통 Problem Details 생성 기준점은 `src/main/java/com/pikume/back/global/error/ProblemDetailFactory.java` 이다.

```java
public class ProblemDetailFactory {

	public ProblemDetail create(ApiProblemType problemType, String detail, String instance) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(problemType.status(), detail);
		problemDetail.setType(problemType.type());
		problemDetail.setTitle(problemType.title());
		problemDetail.setInstance(URI.create(instance));
		return problemDetail;
	}
}
```

## 필수 원칙

- `type`, `title`, `status`, `detail`, `instance`를 기반으로 응답한다.
- 검증 오류는 필요한 경우 확장 필드로 `fieldErrors`를 사용한다.
- 성공 응답은 Problem Details 대상이 아니다.
