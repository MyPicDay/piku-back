# Photo WebP Optimization Runbook

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-06-05

## 목적

이 문서는 일기 사진을 스케줄링으로 WebP 객체로 변환하는 백그라운드 작업의 동작, 설정, 상태 전이, 운영 확인 절차를 정의한다.

## 개요

`PhotoOptimizationScheduler`는 `photo.optimization.enabled=true`일 때만 등록된다. 스케줄은 `photo.optimization.fixed-delay-ms` 간격으로 실행되며, 매 실행마다 `PhotoOptimizationService.optimizePendingPhotos()`를 호출한다.

작업은 `photos.optimization_status = PENDING`이고 `optimized_url IS NULL`인 사진을 `photo.optimization.batch-size` 개수만큼 조회한다. 대상 객체를 로드한 뒤 WebP bytes로 변환하고, 원본 object key의 확장자만 `.webp`로 바꾼 위치에 저장한다. 변환 성공 시 `optimized_url`, `optimized_at`, `optimization_status = SUCCEEDED`를 저장한다.

## 설정

설정 prefix는 `photo.optimization`이다.

| 설정 | 환경 변수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `enabled` | `PHOTO_OPTIMIZATION_ENABLED` | `false` | WebP 최적화 스케줄러 활성화 여부 |
| `fixed-delay-ms` | `PHOTO_OPTIMIZATION_FIXED_DELAY_MS` | `300000` | 이전 실행 완료 후 다음 실행까지 대기 시간 |
| `batch-size` | `PHOTO_OPTIMIZATION_BATCH_SIZE` | `10` | 한 번에 처리할 PENDING 사진 수 |
| `max-attempts` | `PHOTO_OPTIMIZATION_MAX_ATTEMPTS` | `3` | 실패 후 최종 FAILED로 전환하기 전 최대 시도 수 |
| `quality` | `PHOTO_OPTIMIZATION_QUALITY` | `0.82` | WebP 변환 품질 |

## 대상 사진

새 사진 생성 시 확장자 기준으로 최적화 상태를 초기화한다.

- `jpg`, `jpeg`, `png`, `bmp`: `PENDING`
- `webp`: `SUCCEEDED`, `optimized_url`은 원본 `url`
- 확장자가 없거나 지원하지 않는 확장자: `SKIPPED`

기존 데이터는 `V6__add_photo_webp_optimization_columns.sql` 마이그레이션에서 같은 규칙으로 초기화된다.

## 상태 전이

1. 스케줄러가 `PENDING` 대상을 조회한다.
2. 서비스가 대상 photo row를 `PROCESSING`으로 claim한다. 이미 다른 실행이 claim한 row는 건너뛴다.
3. 원본 object key에서 WebP object key를 만들 수 없으면 `SKIPPED`로 전환한다.
4. 변환과 저장이 성공하면 `SUCCEEDED`로 전환하고 `optimized_url`을 저장한다.
5. 실패하면 `optimization_attempt_count`를 1 증가시킨다.
6. 다음 시도 수가 `max-attempts` 미만이면 다시 `PENDING`으로 둔다.
7. 다음 시도 수가 `max-attempts` 이상이면 `FAILED`로 전환한다.

## 저장 및 캐시 정책

WebP 객체 저장은 `StoreObjectPort.storeObject(file, objectKey)` 경로를 사용한다. 백엔드는 object key prefix를 기준으로 S3/MinIO `Cache-Control` metadata를 지정한다.

`public/` object는 브라우저 캐시 5분, CDN/shared cache 20분 정책을 사용한다. `private/` object는 `no-store` 정책을 사용한다.

WebP object key는 원본 object key와 같은 public/private scope를 유지해야 한다.

## 운영 확인

스케줄러 활성화 여부를 확인할 때는 실행 환경의 `PHOTO_OPTIMIZATION_ENABLED` 값을 먼저 확인한다. 값이 `true`가 아니면 스케줄러는 등록되지 않는다.

처리 진행 상황은 `photos` 테이블의 `optimization_status`, `optimization_attempt_count`, `optimization_last_attempt_at`, `optimized_url`로 확인한다.

대표 로그 이벤트는 다음과 같다.

- `event=photo_optimization_claim outcome=skipped`: claim 실패로 대상 건너뜀
- `event=photo_optimization outcome=skipped`: WebP key 생성 불가로 SKIPPED 처리
- `event=photo_optimization outcome=succeeded`: WebP 변환 및 저장 성공
- `event=photo_optimization outcome=failed`: 변환, 원본 로드, 저장 중 실패

## 장애 대응

1. `PHOTO_OPTIMIZATION_ENABLED`가 기대값인지 확인한다.
2. `photo.optimization.batch-size`, `fixed-delay-ms`, `max-attempts`, `quality` 설정이 배포 환경에 반영됐는지 확인한다.
3. `FAILED` row가 증가하면 `event=photo_optimization outcome=failed` 로그의 `reason`, `photoId`, `objectKey`를 확인한다.
4. 원본 object key가 없거나 스토리지에서 로드되지 않으면 스토리지 객체 존재 여부를 확인한다.
5. 변환 실패가 반복되면 원본 이미지 bytes가 실제 이미지 형식인지, WebP ImageIO writer가 런타임에 로드되는지 확인한다.
6. 설정 변경 뒤 재처리가 필요하면 `FAILED` row를 수동으로 `PENDING`으로 되돌리기 전에 실패 원인을 먼저 제거한다.
