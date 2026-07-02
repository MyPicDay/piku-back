# Creative 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-02

## 도메인 개요

Creative 도메인은 사용자의 일기 내용을 기반으로 AI 이미지를 생성하고, 생성된 이미지 파일과 생성 이력의 연결 상태를 관리하는 도메인입니다.

### 목적

- 사용자의 일기 내용을 AI 이미지 생성에 적합한 프롬프트로 변환한다.
- 사용자의 캐릭터 이미지를 참조 이미지로 사용해 일기 이미지 생성 품질을 높인다.
- 생성된 AI 이미지를 저장소에 보관하고, 일기 생성 완료 후 해당 일기와 생성 이력을 연결한다.
- 관리자 통계에서 AI 사진 성공 수의 원천 데이터를 제공한다.

### 핵심 책임

- AI 일기 이미지 생성 요청 처리
- 프롬프트 정책(`DiaryIllustrationPromptPolicy`)에 따른 생성 요청 문장 구성
- 사용자 캐릭터 참조 이미지 조회
- 생성된 이미지 저장과 접근 URL 제공
- 생성 이력(`DiaryImageGeneration`) 저장, 일기 연결, 파일 경로 변경, 폐기 관리
- AI 사진 성공 수 조회와 AI 사진 요청·실패 통계 이벤트 기록 연동

### 도메인 경계

- **Aggregate Root**: `DiaryImageGeneration`
- AI 모델 호출은 `GenerateDiaryIllustrationPort`를 통해 외부 어댑터에 위임한다.
- 이미지 저장소 접근은 `CreativeImageStoragePort`를 통해 인프라 어댑터에 위임한다.
- 캐릭터 참조 이미지는 `LoadCharacterReferencePort`를 통해 Character 도메인에 위임한다.
- 일기 작성 완료 후 생성 이력의 `diaryId`만 갱신하며, 일기 본문과 사진 목록의 소유권은 Diary 도메인에 있다.
- 관리자 통계 이벤트 기록은 Admin 도메인의 공개 포트를 호출하는 cross-context adapter를 통해 수행한다.

### 타 도메인과의 관계

| 도메인 | 관계 설명 |
| --- | --- |
| **Character** | 사용자 캐릭터 이미지를 AI 생성 참조 이미지로 제공한다. |
| **Diary** | AI 생성 이미지를 일기 사진으로 첨부하며, 첨부 완료 후 생성 이력의 `diaryId`를 갱신한다. |
| **Admin** | AI 사진 요청·실패 이벤트 기록을 수신하고, AI 사진 성공 수를 Creative 공개 포트로 조회한다. |

---

## AI 이미지 생성 이력(DiaryImageGeneration)

_Entity_

### 속성

- `id` : Long. 생성 이력의 고유 식별자
- `userId` : String. AI 이미지를 생성한 사용자 식별자
- `prompt` : String. AI 이미지 생성을 위해 사용한 프롬프트
- `filePath` : String. 생성된 이미지 파일의 저장소 경로
- `diaryId` : Long. 생성 이미지를 첨부한 일기 식별자. 아직 일기에 연결되지 않았으면 비어 있다.
- `createdAt` : LocalDateTime. 생성 이력 최초 저장 일시 (`BaseEntity` 공통)
- `updatedAt` : LocalDateTime. 생성 이력 최종 수정 일시 (`BaseEntity` 공통)
- `deletedAt` : LocalDateTime. `DiaryImageGeneration`이 소유하는 생성 이력 폐기 일시

### 행위

- `DiaryImageGeneration(userId, prompt, filePath)` : 생성 사용자, 프롬프트, 저장 경로를 기준으로 생성 이력을 만든다.
- `saveDiaryId(Long diaryId)` : 생성 이미지를 첨부한 일기 식별자를 기록한다.
- `updateFilePath(String newFilePath)` : 이미지 파일 경로를 변경한다.
- `discard()` : 생성 이력을 폐기 처리하고 `deletedAt`을 현재 시각으로 설정한다.
- `isDiscarded()` : 생성 이력이 폐기 처리되었는지 여부를 반환한다.

### 규칙

- 생성 성공 이력은 이미지 저장이 완료된 뒤 저장된다.
- `diaryId`가 비어 있는 생성 이력은 아직 일기에 첨부되지 않은 이미지로 해석한다.
- 생성 이력의 폐기는 물리 삭제가 아니라 `deletedAt` 값을 기록하는 소프트 딜리트로 처리한다.
- 기존 기간별 AI 사진 성공 통계는 폐기되지 않은 생성 이력만 집계할 수 있다.
- 관리자 통합 대시보드의 누적 AI 사진 성공 수와 일간 성공 수는 폐기 여부와 관계없이 생성 성공 이력을 포함한다.

---

## 프롬프트 정책(DiaryIllustrationPromptPolicy)

_Domain Policy_

### 책임

- 사용자가 작성한 일기 내용을 AI 일러스트 생성에 적합한 프롬프트로 변환한다.
- 서비스의 이미지 톤과 일기 기반 생성 목적을 일관되게 유지한다.

### 규칙

- 프롬프트 정책은 생성 요청의 문장 구성만 담당한다.
- 외부 AI 모델 호출, 저장소 업로드, 통계 이벤트 기록은 애플리케이션 서비스와 포트가 담당한다.

---

## AI 사진 통계

### 규칙

- AI 사진 성공 수의 원천 데이터는 `DiaryImageGeneration`이다.
- AI 사진 요청과 실패는 Admin 통계 이벤트로 기록한다.
- Creative 도메인은 Admin 도메인의 통계 저장 구조를 직접 알지 않고, 공개 포트와 cross-context adapter를 통해 이벤트 기록을 위임한다.
- Admin 도메인은 Creative 도메인의 공개 조회 포트를 통해 AI 사진 성공 집계를 조회한다.
