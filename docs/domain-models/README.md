# Domain Models

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-11

## 목적

이 디렉터리는 현재 문서화된 모델 경계 안의 Aggregate, Entity, Value Object, 상태, 행위와 비즈니스 규칙을 설명한다.

문서 목록과 탐색 경로는 Domain Models Index가 관리한다.

## 문서 책임

- Domain Model 문서는 Context 또는 Boundary Candidate 내부의 현재 모델과 불변식을 설명한다.
- Context 간 관계, 공개 계약 방향과 번역 상태는 이 디렉터리에서 중복 관리하지 않는다.
- Context 경계 상태와 전략 분류는 Context Map만 관리한다.
- 구현 파일, API, Use Case, Port, Repository, Adapter와 저장 쿼리는 Domain Model의 기준 정보가 아니다.
- Domain Model 문서가 존재한다는 사실만으로 Bounded Context가 영구 확정되지는 않는다.

## 작성 원칙

각 문서는 필요한 범위에서 다음 순서를 사용한다.

1. 도메인 개요와 목적
2. 핵심 책임과 경계
3. Aggregate와 값 객체
4. 상태와 행위
5. 비즈니스 불변식
6. 모델이 소유하는 도메인 능력과 명시적으로 소유하지 않는 책임

Domain Model 문서는 구현된 비즈니스 모델과 규칙을 설명하며 구현 작업 순서나 마이그레이션 체크리스트를 포함하지 않는다. 용어와 경계의 도메인 검증 근거가 부족하면 확정된 사실처럼 보완하지 않고 Context Map에 미확정 상태와 확인할 질문을 기록한다.
