# Documentation System

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-11

## 목적

이 문서는 `piku-back`의 공식 문서 구조, 배치 규칙, 수명주기, 검증 규칙을 정의한다.

## 공식 카테고리

- `docs/architecture/`
- `docs/standards/`
- `docs/product-specs/`
- `docs/handoffs/`
- `docs/runbooks/`
- `docs/incident-retrospectives/`
- `docs/domain-models/`
- `docs/references/`
- `docs/generated/`
- `docs/archive/`

## 카테고리 경계

- `docs/runbooks/`는 장애 또는 운영 이슈가 발생했을 때 따라야 하는 단계별 대응 절차서를 보관한다.
- `docs/incident-retrospectives/`는 실제 발생한 장애의 상황, 원인, 해결 방안 검토, 선정 근거, 회고를 기록한 장애 회고 문서를 보관한다.

## 운영 원칙

- `AGENTS.md`는 짧은 맵만 제공한다.
- 공식 기준 문서는 반드시 `docs/` 아래에 존재해야 한다.
- 재사용 가치가 낮은 legacy 문서는 archive 대신 삭제할 수 있다.
- `docs/superpowers/`는 공식 문서가 아니다.
- 새 공식 문서를 만들면 반드시 해당 index에 연결한다.
- 문서 목록, 분류와 문서 간 이동은 `docs/README.md`와 `docs/indexes/`가 전담한다.
- 개별 공식 문서는 다른 공식 문서의 링크나 관련 문서 목록을 포함하지 않고 자신의 주제만 독립적으로 설명한다.
- 공식 문서에는 구현 소스코드 본문, 소스 파일 링크, 로컬 절대 경로와 소스 파일 위치를 포함하지 않는다.
- API 오류 응답의 중복 구현을 방지하기 위한 공용 Factory 코드는 `api-error-responses.md`에만 예외로 둘 수 있다.
- API 요청·응답 예시, 운영 명령, 설정 예시와 Mermaid 다이어그램은 문서 목적에 필요하면 사용할 수 있다.
