# Domain-Driven Design 원칙

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-11

## 목적

이 문서는 `piku-back`에서 Domain-Driven Design(DDD)을 적용할 때 사용하는 모델링 원칙과 경계 판단 기준을 정의한다.

DDD는 패키지 구조나 특정 프레임워크를 선택하는 방법이 아니다. 복잡한 비즈니스 문제를 도메인 전문가와 개발자가 함께 탐구하고, 명시적인 Bounded Context 안에서 공유 언어와 모델을 코드에 연결하는 접근이다.

## 1. 적용 관점

`piku-back`은 다음 순서로 DDD를 적용한다.

1. 비즈니스 문제와 Core Domain을 식별한다.
2. 도메인 전문가와 개발자가 Ubiquitous Language를 발전시킨다.
3. 서로 다른 모델과 언어가 유효한 경계를 Bounded Context로 명시한다.
4. Context 간 관계와 번역 방식을 Context Map에 기록한다.
5. Aggregate, Entity, Value Object, Domain Service와 Domain Event로 규칙을 구현한다.
6. 코드와 문서에서 발견한 새로운 통찰을 모델과 언어에 다시 반영한다.

패키지 이동이나 인터페이스 추가만으로 DDD가 적용됐다고 판단하지 않는다. 모델, 언어, 불변식과 책임이 더 명확해졌는지를 함께 평가한다.

## 2. 전략 설계

### DDD-STRATEGY-001 · Core Domain에 집중한다

- 제품 차별성과 핵심 비즈니스 성과에 직접 기여하는 모델을 Core Domain으로 식별한다.
- Supporting Subdomain과 Generic Subdomain에는 Core Domain과 같은 수준의 모델링 비용을 자동으로 적용하지 않는다.
- Core·Supporting·Generic 분류는 코드 복잡도가 아니라 사업적 중요성, 차별성, 변화 속도를 근거로 한다.
- 현재 분류 근거가 부족하면 임의로 확정하지 않고 Context Map에 미확정 상태와 필요한 확인 사항을 기록한다.

### DDD-LANGUAGE-001 · Ubiquitous Language를 사용한다

- 하나의 Bounded Context 안에서는 도메인 전문가, 문서, 테스트와 코드가 같은 핵심 용어를 사용한다.
- 클래스·메서드·Port·이벤트 이름은 기술 동작보다 도메인 의도와 결과를 우선 표현한다.
- 같은 단어가 Context마다 다른 의미를 가지면 억지로 통합하지 않고 각 의미와 경계를 명시한다.
- 용어의 의미가 바뀌면 문서만 고치지 않고 모델, 테스트와 코드 이름의 변경 필요도 함께 검토한다.

### DDD-BOUNDARY-001 · Bounded Context를 모델 경계로 판단한다

Bounded Context는 특정 모델과 Ubiquitous Language가 정의되고 적용되는 명시적 경계다. Java 최상위 패키지, Gradle 모듈, 데이터베이스 스키마, 배포 단위는 경계를 구현하는 수단 또는 단서일 수 있지만 그 자체가 Bounded Context의 충분조건은 아니다.

경계를 판단할 때 다음 근거를 함께 본다.

- 해당 영역이 해결하는 비즈니스 문제와 사용자
- 핵심 용어의 의미와 다른 영역에서의 의미 차이
- 상태 변경 규칙과 불변식의 소유자
- 모델과 데이터의 변경 권한
- 팀의 의사결정 및 릴리스 영향 범위
- 다른 모델과 만나는 지점, 번역 방식과 결합 비용

코드 구조만으로 경계를 추정한 경우에는 `Working Context` 또는 `Boundary Candidate`로 기록한다. 도메인 전문가와 모델을 검증하지 않은 상태에서 확정된 Bounded Context로 표현하지 않는다.

### DDD-CONTEXT-MAP-001 · 현재 지형을 먼저 기록한다

- Context Map은 모델 경계, 접점, 번역 방식, 공유 영역과 영향 방향을 기록한다.
- 런타임 호출 방향만으로 upstream/downstream 또는 Customer/Supplier 관계를 확정하지 않는다.
- 관계 패턴은 팀 간 영향력, 변경 협의 방식과 모델 소유권 근거가 있을 때 명명한다.
- 현재 직접 결합이나 모호한 경계도 숨기지 않고 그대로 기록한 뒤, 목표 구조와 마이그레이션은 별도 설계에서 다룬다.

## 3. 전술 설계

### DDD-MODEL-001 · 모델이 비즈니스 규칙을 표현한다

- Entity는 식별성과 생명주기를 가지며 상태 변경 행위로 규칙을 보호한다.
- Value Object는 속성의 조합으로 의미가 정해지고 가능한 한 불변으로 다룬다.
- 특정 Entity나 Value Object에 자연스럽게 속하지 않는 중요한 도메인 연산은 Domain Service로 표현할 수 있다.
- 단순 조회 조합, 트랜잭션 순서와 외부 시스템 호출은 Domain Service가 아니라 Application 책임이다.
- 데이터 운반만 하는 모델을 기본값으로 삼지 않으며, 중요한 규칙이 Application Service나 Adapter로 흩어지지 않게 한다.

### DDD-AGGREGATE-001 · Aggregate가 일관성 경계를 보호한다

- Aggregate는 함께 일관성을 유지해야 하는 Entity와 Value Object의 경계다.
- 외부 객체는 Aggregate Root를 통해서만 Aggregate 내부 상태를 변경한다.
- 하나의 트랜잭션에서 반드시 지켜야 하는 불변식은 Aggregate 경계 안에서 동기적으로 보호한다.
- 다른 Aggregate를 객체 그래프로 직접 소유하기보다 식별자로 참조하는 방식을 우선 검토한다.
- Bounded Context와 Aggregate는 같은 개념이 아니다. 하나의 Context는 여러 Aggregate를 가질 수 있다.
- 기존 테이블 관계나 JPA 연관관계만으로 Aggregate 경계를 결정하지 않는다.

### DDD-REPOSITORY-001 · Repository는 Aggregate 접근을 표현한다

- 상태 변경을 위한 Repository는 직접 접근이 필요한 Aggregate Root를 대상으로 한다.
- 조회 기준은 저장 기술이 아니라 Ubiquitous Language와 Application의 목적을 표현한다.
- 화면·통계·검색을 위한 Read Model은 Aggregate를 억지로 복원하지 않고 별도 조회 모델을 사용할 수 있다.
- Repository와 조회 Port를 분리할지는 모델 일관성, 변경 이유와 조회 특성에 따라 결정한다.

### DDD-EVENT-001 · Domain Event는 이미 일어난 도메인 사실을 표현한다

- Domain Event는 Context 안에서 중요하게 인식되는 과거형 비즈니스 사실이다.
- 이벤트를 도입하기 전에 발행 이유, 소비자, 전달 시점, 실패와 중복 처리 정책을 명시한다.
- 단순한 메서드 호출을 이벤트로 바꾸는 것만으로 Context 결합이 제거되지는 않는다.
- 다른 Context로 공개하는 이벤트는 내부 Domain Event와 분리된 Published Contract가 필요할 수 있다.

## 4. Application 및 기술 아키텍처와의 관계

- Domain은 비즈니스 상태와 규칙을 표현한다.
- Application은 유스케이스, 권한 확인, 트랜잭션과 여러 도메인 행위의 순서를 조정한다.
- Adapter는 HTTP, 데이터베이스, 메시징, 파일 저장소와 외부 SDK 세부사항을 변환한다.
- 이 분리는 DDD 모델을 보호하는 수단이며, 패키지 구조 자체가 모델의 품질을 보장하지 않는다.
- 조회 전용 흐름은 Domain 객체를 항상 경유할 필요가 없지만, 상태 변경 규칙을 우회해서는 안 된다.

## 5. 모델 변경과 검증

- 리팩터링 전에 변경 대상 용어, 규칙, Aggregate와 Context 소유권을 확인한다.
- 코드와 문서가 충돌하면 현재 동작을 사실로 기록하되, 코드가 곧 올바른 모델이라고 가정하지 않는다.
- 모델 변경은 대표 시나리오와 불변식 테스트로 검증한다.
- Context 경계 변경이 데이터 소유권, API, 이벤트 또는 트랜잭션을 바꾸면 별도 설계와 마이그레이션 검토가 필요하다.
- 아키텍처 테스트는 의존 방향을 보호할 수 있지만 Ubiquitous Language와 모델 타당성을 대신 검증하지 못한다.

## 참고 기준

- [Eric Evans, DDD Reference](https://www.domainlanguage.com/ddd/reference/)
- [Eric Evans, Domain-Driven Design Reference PDF](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf)
