# Documentation System

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-04-12

## 목적

이 문서는 `piku-back`의 공식 문서 구조, 배치 규칙, 수명주기, 검증 규칙을 정의한다.

## 공식 카테고리

- `docs/architecture/`
- `docs/standards/`
- `docs/product-specs/`
- `docs/handoffs/`
- `docs/runbooks/`
- `docs/domain-models/`
- `docs/references/`
- `docs/generated/`
- `docs/archive/`

## 운영 원칙

- `AGENTS.md`는 짧은 맵만 제공한다.
- 공식 기준 문서는 반드시 `docs/` 아래에 존재해야 한다.
- 재사용 가치가 낮은 legacy 문서는 archive 대신 삭제할 수 있다.
- `docs/superpowers/`는 공식 문서가 아니다.
- 새 공식 문서를 만들면 반드시 해당 index에 연결한다.
