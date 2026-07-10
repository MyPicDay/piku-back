# Domain Models

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-10

## 목적

이 디렉터리는 각 Bounded Context 내부의 Aggregate, Entity, Value Object, 상태, 행위와 비즈니스 규칙을 설명한다.

전체 Context 목록, 소유 책임과 Context 간 호출 방향은 [Bounded Context Map](../architecture/bounded-context-map.md), 공통 계층과 Port·Adapter 규칙은 [DDD + 헥사고날 아키텍처](../architecture/ddd-hexagonal-architecture.md)를 기준으로 한다.

## 문서 책임

- Domain Model 문서는 하나의 Context 내부 모델과 불변식을 설명한다.
- Context 간 관계도와 공개 계약 방향은 이 디렉터리에서 중복 관리하지 않는다.
- 구현 파일, Repository 메서드와 특정 Adapter 위치는 Domain Model의 기준 정보가 아니다.
- 현재 구현의 아키텍처 경계가 공통 규칙과 다르면 [아키텍처 미비점 분석](../architecture/ddd-hexagonal-architecture-gap-analysis.md)에 차이를 기록한다.

## Active Documents

| Context | 핵심 역할 | 문서 |
| --- | --- | --- |
| **Admin** | 관리자 계정·인증·세션, 감사와 운영 통계 | [admin.md](admin.md) |
| **Creative** | AI 일기 이미지 생성과 생성 이력 | [creative.md](creative.md) |
| **Diary** | 일기, 사진과 공개 범위 | [diary.md](diary.md) |
| **Feed** | 피드 구성, 정렬과 열람 이력 | [feed.md](feed.md) |
| **Notification** | 알림 이력·전달과 기기 푸시 토큰 | [notification.md](notification.md) |
| **Social** | 친구 관계, 댓글과 좋아요 | [social.md](social.md) |
| **User** | 사용자 식별, 프로필 관리와 닉네임 점유 | [user.md](user.md) |

Character, Recommendation, Support를 포함한 전체 Context 책임은 Context Map에 정의한다. 전용 Domain Model 문서를 추가할 때에는 이 목록과 [Domain Models Index](../indexes/domain-models-index.md)를 함께 갱신한다.

## 작성 원칙

각 문서는 필요한 범위에서 다음 순서를 사용한다.

1. 도메인 개요와 목적
2. 핵심 책임과 경계
3. Aggregate와 값 객체
4. 상태와 행위
5. 비즈니스 불변식
6. 다른 Context에 공개하거나 소비하는 능력

Domain Model 문서는 구현된 비즈니스 모델과 규칙을 설명하며 구현 작업 순서나 마이그레이션 체크리스트를 포함하지 않는다.
