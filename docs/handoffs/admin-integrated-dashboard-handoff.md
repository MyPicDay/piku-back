# 관리자 통합 대시보드 프론트엔드 Handoff

- Status: Active
- Audience: Backend Engineers, Frontend Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-06-22

## 목적

관리자 첫 화면에서 서비스 핵심 규모와 최근 활동을 표시하기 위한 통합 대시보드 API 계약을 정의한다.

기존 `GET /api/admin/statistics/dashboard`와 `GET /api/admin/statistics/dashboard.csv`는 기간별 상세 통계 및 CSV 내보내기 API다. 이 문서의 통합 대시보드와 별개이며 기존 계약을 그대로 유지한다.

## API

| 항목 | 값 |
| --- | --- |
| Method | `GET` |
| Path | `/api/admin/dashboard` |
| 접근 조건 | 인증 완료된 모든 관리자 역할 |
| 요청 파라미터 | 없음 |
| 날짜 기준 | `Asia/Seoul` |

관리자 세션 쿠키를 포함해 요청한다. 성공 응답의 배열은 날짜 또는 기간 시작일 오름차순이다.

## 응답 영역

### 핵심 지표

| 필드 | 의미 |
| --- | --- |
| `keyMetrics.currentCumulativeMemberCount` | 탈퇴 여부와 관계없는 현재 누적 가입 회원 수 |
| `keyMetrics.cumulativeMemberCountSevenDaysAgo` | 최근 7일 시작 전까지 가입한 누적 회원 수 |
| `keyMetrics.recent30DayActiveUserCount` | 오늘을 포함한 최근 30일의 고유 활성 회원 수 |
| `keyMetrics.previous30DayActiveUserCount` | 최근 30일 바로 이전 30일의 고유 활성 회원 수 |
| `keyMetrics.currentAiPhotoSuccessCount` | 삭제 여부와 관계없는 누적 AI 사진 생성 성공 수 |
| `keyMetrics.aiPhotoSuccessCountSevenDaysAgo` | 최근 7일 시작 전까지의 누적 AI 사진 생성 성공 수 |
| `keyMetrics.currentDiaryCreationCount` | 삭제 여부와 관계없는 누적 일기 작성 수 |
| `keyMetrics.diaryCreationCountSevenDaysAgo` | 최근 7일 시작 전까지의 누적 일기 작성 수 |

누적 회원 수는 탈퇴 여부와 관계없이 회원 계정 생성 이력을 기준으로 한다.

### DAU·MAU 활동 정의

활성 사용자는 해당 기간에 다음 조건을 만족한 고유 회원이다.

- 로그인 완료 상태에서 관리자 API를 제외한 `/api/**` 요청을 한 번 이상 전송한다.
- HTTP 메서드는 제한하지 않지만 CORS preflight인 `OPTIONS` 요청은 제외한다.
- 응답 성공 여부와 관계없이 인증된 요청이 서버 통계 필터에 도달하면 활동으로 기록한다.
- 로그인 요청 자체는 요청 처리 시점에 아직 회원 인증 주체가 없으므로 활동으로 집계하지 않는다.
- 앱 실행 자체는 집계하지 않는다. 앱 실행 후 인증된 API 요청이 발생해야 활동으로 집계한다.
- 일기 조회·작성, AI 사진 생성 등 인증된 일반 API 요청은 모두 활동에 포함한다.
- 비로그인 요청과 관리자 `/api/admin/**` 요청은 DAU·MAU에서 제외한다.

DAU는 날짜별로 회원 식별자를 중복 제거하고, MAU는 전체 30일 구간에서 회원 식별자를 다시 중복 제거한다. MAU는 일별 DAU의 합이 아니다.

### 일간 활성 사용자 차트

`dailyActiveUsers`는 오늘과 이전 6일을 포함한 7개 항목을 항상 반환한다.

| 필드 | 의미 |
| --- | --- |
| `date` | 활동 날짜 |
| `dau` | 해당 날짜의 고유 활성 회원 수 |

활동이 없는 날짜도 `dau`가 0인 항목으로 포함한다.

### AI 사진 생성 현황

`aiPhotoGeneration`은 최근 7일 합계를 반환한다.

| 필드 | 의미 |
| --- | --- |
| `successCount` | 삭제 여부와 관계없는 AI 사진 생성 성공 건수 |
| `failureCount` | AI 사진 생성 실패 이벤트 건수 |

### 주간 활동 비교

`weeklyActivity`는 현재 ISO 주와 이전 3개 ISO 주를 포함한 4개 항목을 반환한다.

| 필드 | 의미 |
| --- | --- |
| `periodStartDate` | ISO 주 시작일인 월요일 |
| `periodEndDate` | ISO 주 종료일인 일요일 |
| `newMemberCount` | 탈퇴 여부와 관계없이 해당 주에 가입한 회원 수 |
| `diaryCreationCount` | 삭제 여부와 관계없이 해당 주에 작성된 일기 수 |

현재 주의 `periodEndDate`도 일요일이다. 오늘 이후 날짜의 데이터는 집계하지 않는다.

### 날짜별 데이터 요약

`dailySummary`는 오늘과 이전 6일을 포함한 7개 항목을 항상 반환한다.

| 필드 | 의미 |
| --- | --- |
| `date` | 집계 날짜 |
| `newMemberCount` | 탈퇴 여부와 관계없이 해당 날짜에 가입한 회원 수 |
| `dau` | 해당 날짜의 고유 활성 회원 수 |
| `diaryCreationCount` | 삭제 여부와 관계없이 해당 날짜에 작성된 일기 수 |
| `aiPhotoRequestCount` | 해당 날짜의 AI 사진 생성 요청 이벤트 수 |

데이터가 없는 날짜도 모든 수치가 0인 항목으로 포함한다.

## 값 형식과 일관성

- 모든 `count`와 `dau` 값은 0 이상의 정수다.
- 모든 날짜는 `YYYY-MM-DD` 형식이다.
- `dailyActiveUsers.dau`와 같은 날짜의 `dailySummary.dau`는 반드시 동일하다.
- 일간 배열은 날짜 오름차순이며 최근 7일을 빠짐없이 포함한다.
- 주간 배열은 기간 시작일 오름차순이며 최근 4개 ISO 주를 빠짐없이 포함한다.

## 증감률 표시 규칙

API는 현재 값과 비교 기준 값을 제공하며 증감률을 직접 반환하지 않는다. 프론트엔드가 증감률을 표시할 때 다음 규칙을 사용한다.

| 이전 값 | 현재 값 | 표시 |
| --- | --- | --- |
| 0 | 0 | `0%` |
| 0 | 양수 | `신규` |
| 양수 | 모든 값 | `(현재 값 - 이전 값) / 이전 값 × 100` |

계산된 비율의 반올림 자릿수와 양수 기호 표시는 프론트엔드 표현 정책으로 통일한다. `Infinity`, `NaN` 또는 임의의 `100%`를 표시하지 않는다.

## 기간 예시

조회 기준일이 2026년 6월 22일인 경우 다음 기간을 사용한다.

- 최근 7일: 2026년 6월 16일~2026년 6월 22일
- 7일 전 누적 기준: 2026년 6월 15일 종료 시점
- 최근 30일: 2026년 5월 24일~2026년 6월 22일
- 직전 30일: 2026년 4월 24일~2026년 5월 23일
- 최근 4주: 2026년 6월 1일 시작 주부터 2026년 6월 22일 시작 주까지
- 현재 주 표시 기간: 2026년 6월 22일~2026년 6월 28일

## 오류 처리

관리자 인증과 저장소 오류는 RFC 9457 Problem Details 형식이다.

| 상황 | 상태 | Problem Type URI | 처리 |
| --- | --- | --- | --- |
| 인증 세션 없음·만료·폐기 | 401 | `https://api.pikume.com/problems/security/unauthenticated` | 로컬 관리자 상태를 비우고 로그인 흐름 시작 |
| 관리자 권한 거부 | 403 | `https://api.pikume.com/problems/security/forbidden` | 요청 중단 및 권한 상태 안내 |
| 처리되지 않은 서버·통계 원천 오류 | 500 | `https://api.pikume.com/problems/common/internal-server-error` | 장애 상태 표시, 즉시 반복 요청하지 않음 |
| 관리자 세션 저장소 확인 불가 | 503 | `https://api.pikume.com/problems/admin/session-store-unavailable` | 장애 상태 표시, 즉시 반복 요청하지 않음 |

프론트엔드는 오류 문자열이 아니라 `status`와 `type`을 기준으로 분기한다.

## QA 기준

- 모든 관리자 역할에서 통합 대시보드를 조회할 수 있다.
- 최근 7일 배열은 데이터 유무와 관계없이 7개 항목이다.
- 최근 4주 배열은 현재 부분 주를 포함해 4개 항목이다.
- 현재 주 종료일은 일요일로 표시된다.
- DAU와 MAU에 비로그인 방문자가 포함되지 않는다.
- 동일 날짜의 `dailyActiveUsers.dau`와 `dailySummary.dau`가 일치한다.
- 신규 가입자 수에는 조회 시점에 탈퇴한 회원의 가입 이력도 포함된다.
- 삭제된 일기와 AI 사진 성공 이력이 누적 작성·생성 수에 포함된다.
- 기존 통계 대시보드의 기간 파라미터와 CSV 내보내기가 그대로 동작한다.
