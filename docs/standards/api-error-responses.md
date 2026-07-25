# API Error Responses

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-25

## 목적

이 문서는 `piku-back`의 API 실패 응답 표준을 정의한다.

## 기준

- 실패 응답은 RFC 9457 / Problem Details를 따른다.
- ad hoc 문자열 body와 bespoke error DTO는 새로 추가하지 않는다.
- 임시 호환이 필요하면 문서화된 예외로만 허용한다.

## 공통 생성 원칙

- 모든 Web Adapter는 공용 Problem Details 생성 경로를 사용한다.
- 모듈마다 Problem Details 조립 규칙을 중복 구현하지 않는다.
- Application과 Domain 오류를 HTTP 표현으로 변환하는 책임은 Web Adapter 경계에 둔다.

## 공용 구현 기준

`ProblemDetailFactory`는 API 실패 응답을 생성하는 유일한 공용 Factory다. 모듈별 Factory나 별도의 Problem Details 조립 코드를 새로 만들지 않는다.

아래 코드는 문서 소스코드 금지 원칙의 예외이며 현재 공용 구현과 함께 유지한다.

```java
@Component
public class ProblemDetailFactory {

	public ProblemDetail create(ApiProblemType problemType, String detail, String instance) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(problemType.status(), detail);
		problemDetail.setType(problemType.type());
		problemDetail.setTitle(problemType.title());
		problemDetail.setInstance(URI.create(instance));
		return problemDetail;
	}

	public ProblemDetail validation(String detail, String instance, Map<String, String> fieldErrors) {
		ProblemDetail problemDetail = create(ValidationProblemType.INVALID_REQUEST, detail, instance);
		problemDetail.setProperty("fieldErrors", fieldErrors);
		return problemDetail;
	}
}
```

공용 Factory의 필드 구성이나 검증 오류 확장 방식이 변경되면 이 코드와 API 오류 응답 테스트를 함께 갱신한다.

## 예상하지 못한 기술 장애

- Domain 또는 Application 의미가 있는 예상 가능한 실패는 각 Context의 Web Adapter가 해당 의미를 보존한 Problem Details로 변환한다.
- 저장소 연결 장애, 타임아웃과 예상하지 못한 외부 시스템 오류처럼 Application 의미가 없는 미처리 장애는 전역 fallback이 공통 내부 서버 오류 Problem Details로 변환한다.
- 전역 fallback은 기술 원인과 내부 메시지를 응답에 노출하지 않고, 서버 로그와 구성된 운영 알림으로 장애 사실을 전달한다.
- 운영 알림 요청의 실패는 공통 내부 서버 오류 응답 작성을 중단시키지 않는다.
- 전역 fallback은 마지막 안전망이며 검증, 비즈니스 분기, 재시도, 보상과 같은 유스케이스 제어 흐름으로 사용하지 않는다.
- Context 전용 오류 코드는 예상하지 못한 기술 장애의 이름을 바꾸기 위한 목적으로 추가하지 않는다.

## Filter 기반 보안 오류

Spring Security Filter, `AuthenticationEntryPoint`와 `AccessDeniedHandler`처럼 MVC 예외 처리 밖에서 응답하는 구성요소도 공통 `ProblemDetailFactory`를 사용한다.

- Security 기술 모듈은 직렬화 책임을 `SecurityProblemResponseWriter` 한 곳에 둔다.
- 응답은 `application/problem+json`, UTF-8과 `Cache-Control: no-store`를 사용한다.
- 이미 커밋된 응답에는 오류 Body를 다시 쓰지 않는다.
- Spring 내부 예외, 저장소 원인과 원본 자격 증명은 외부 `detail`에 노출하지 않는다.
- Admin Application 오류 코드는 Security Web 문제 타입으로 번역하되 공개 type URI와 status 계약을 유지한다.

## 필수 원칙

- `type`, `title`, `status`, `detail`, `instance`를 기반으로 응답한다.
- 검증 오류는 필요한 경우 확장 필드로 `fieldErrors`를 사용한다.
- 성공 응답은 Problem Details 대상이 아니다.
