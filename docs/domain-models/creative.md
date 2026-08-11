# Creative 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-08-11

## 도메인 개요

Creative 도메인은 사용자의 일기 내용을 기반으로 AI 이미지를 생성하고, 생성 자산과 생성 이력의 연결 상태를 관리한다.

### 목적

- 사용자의 일기 내용을 AI 이미지 생성에 적합한 프롬프트로 변환한다.
- 사용자의 캐릭터 이미지를 참조 이미지로 사용해 일기 이미지 생성 품질을 높인다.
- 생성된 AI 이미지를 저장소에 보관하고, 일기 생성 완료 후 해당 일기와 생성 이력을 연결한다.
- 현재 일기 작성에 필요한 시각적 표현을 제공한다.
- AI 사진 성공 수의 원천 데이터를 제공한다.

### 핵심 책임

- AI 일기 이미지 생성 요청 처리
- 프롬프트 정책(`DiaryIllustrationPromptPolicy`)에 따른 생성 요청 문장 구성
- 선택 캐릭터 또는 현재 사용자 아바타의 참조 이미지 조회
- 생성된 이미지 저장과 접근 URL 제공
- 생성 이력(`DiaryImageGeneration`) 저장, 일기 연결, 파일 경로 변경, 폐기 관리
- 일일 생성 쿼터의 조회·소비·반환 조정
- AI 사진 성공 수와 생성 결과의 집계 기준 관리

### 도메인 경계

- **Aggregate Root**: `DiaryImageGeneration`
- 현재 기록 작성 흐름은 AI 이미지 생성을 필수 선행 조건으로 사용한다. 생성 실패는 기록 작성 시작 조건을 충족하지 못한 것으로 처리된다.
- Creative는 이미지 생성 요청, 생성 상태, 생성 자산, 할당량과 이력을 소유한다.
- 생성 이미지 Object Key, 미디어 타입과 private 캐시 정책은 Creative Storage Adapter가 소유한다.
- Diary 사진 저장 Adapter는 Creative 생성 이미지 Port를 구현하지 않는다.
- Creative는 생성 이미지가 기록에 연결되었는지를 식별하지만 기록의 본문, 사진 목록, 표시와 회고 정책은 소유하지 않는다.
- Creative는 별도의 감정 값과 감정 통계를 소유하지 않는다.
- 외부 AI 모델 호출, 이미지 저장과 참조 이미지 조회 기술은 Creative 모델의 책임이 아니다.

---

## AI 이미지 생성 이력(DiaryImageGeneration)

_Entity_

### 속성

- `id` : Long. 생성 이력의 고유 식별자
- `userId` : String. AI 이미지를 생성한 사용자 식별자
- `prompt` : String. AI 이미지 생성을 위해 사용한 프롬프트
- `filePath` : String. 생성된 이미지 파일의 저장소 경로
- `diaryId` : Long. 생성 이미지를 첨부한 일기 식별자. 아직 일기에 연결되지 않았으면 비어 있다.
- `createdAt` : LocalDateTime. 생성 이력 최초 저장 일시
- `updatedAt` : LocalDateTime. 생성 이력 최종 수정 일시
- `deletedAt` : LocalDateTime. `DiaryImageGeneration`이 소유하는 생성 이력 폐기 일시

### 행위

- 생성 사용자, 프롬프트와 저장 참조를 기준으로 생성 이력을 만든다.
- `attachToDiary` : 생성 이미지를 첨부한 일기 식별자를 기록한다.
- `updateFilePath(String newFilePath)` : 이미지 파일 경로를 변경한다.
- `discard()` : 생성 이력을 폐기 처리하고 `deletedAt`을 현재 시각으로 설정한다.
- `isDiscarded()` : 생성 이력이 폐기 처리되었는지 여부를 반환한다.

### 규칙

- 생성 성공 이력은 이미지 저장이 완료된 뒤 저장된다.
- 생성 사용자 식별자, 프롬프트와 이미지 저장 참조는 비어 있을 수 없다.
- `diaryId`가 비어 있는 생성 이력은 아직 일기에 첨부되지 않은 이미지로 해석한다.
- 하나의 생성 이력은 하나의 일기에만 연결할 수 있으며 이미 연결된 이력을 다른 일기로 변경하지 않는다.
- 폐기된 생성 이력은 일기에 연결할 수 없다.
- 생성 이력의 폐기는 물리 삭제가 아니라 `deletedAt` 값을 기록하는 소프트 딜리트로 처리한다.
- 활성 생성 자산 수는 폐기되지 않은 생성 이력만 집계한다.
- 생성 성공 수는 이후 폐기 여부와 관계없이 생성에 성공했던 이력을 포함한다.

---

## 프롬프트 정책(DiaryIllustrationPromptPolicy)

_Domain Policy_

### 책임

- 사용자가 작성한 일기 내용을 AI 일러스트 생성에 적합한 프롬프트로 변환한다.
- 서비스의 이미지 톤과 일기 기반 생성 목적을 일관되게 유지한다.

### 규칙

- 프롬프트 정책은 생성 요청의 문장 구성만 담당한다.
- 외부 AI 모델 호출, 저장소 업로드와 통계 기록은 이 정책의 책임이 아니다.

---

## AI 사진 통계

### 규칙

- AI 사진 성공 수의 원천 데이터는 `DiaryImageGeneration`이다.
- AI 사진 요청과 실패는 생성 성공 이력과 구분되는 사건이다.
- 활성 생성 자산 수와 생성 성공 수는 폐기 이력 포함 여부가 서로 다른 지표다.

---

## Application과 통합 경계

### 생성 오케스트레이션

현재 생성 흐름은 요청 통계 기록, 쿼터 소비, 선택적 캐릭터 식별자에 따른 참조 출처 선택, 참조 이미지 준비, 외부 AI 호출, 생성 이미지 저장, 표시 URL 해석과 생성 이력 기록 순서로 수행한다.

캐릭터 식별자가 있으면 Character가 요청 사용자에게 허용한 선택 캐릭터 참조만 사용한다. 식별자가 없을 때만 User가 공개한 현재 아바타 참조를 사용한다. 명시된 선택을 사용할 수 없거나 참조 이미지 준비가 실패하면 User 아바타로 대체하지 않는다.

이번 아키텍처 기준선은 현재 트랜잭션과 실패 의미를 변경하지 않는다. 외부 AI와 Object Storage 호출을 DB 트랜잭션 밖으로 분리하는 작업, 저장 후 이력 기록 실패의 고아 객체 삭제와 쿼터 보상 강화는 별도 기능·통합 강화 계획에서 결정한다.

### Character와 User 참조

- Creative Application은 캐릭터 식별자 유무에 따라 선택 캐릭터와 User 아바타 중 하나의 출처만 조회한다.
- 선택 캐릭터 조회는 Creative 소유 Out Port와 Cross-context Adapter가 Character의 공개 사용 가능 참조 계약을 호출해 수행한다.
- Character는 생성 유형, 소유권과 유형별 저장 참조 정규화를 소유하며 Creative에는 정규 저장 참조만 제공한다.
- User 공개 조회 계약은 사용자가 선택한 현재 아바타 저장 참조를 제공하고, Creative는 기존 레거시 참조를 생성 입력용 canonical 참조로 번역한다.
- Character 조회, User 조회와 Object Storage 로드는 목적별 Out Port와 Adapter가 담당한다.
- 다운로드 정책이 없는 절대 URL은 외부 AI의 참조 이미지로 사용하지 않는다.
- 고정 캐릭터 Object는 Storage Adapter의 제한된 캐시를 사용할 수 있다.

### Admin 통계

- Creative는 요청, 성공과 실패 기록 의도를 자신의 Out Port로 표현한다.
- Admin은 AI 사진 통계 기록을 위한 공개 Application 계약을 제공한다.
- Creative Adapter는 Admin Domain Enum과 저장 계약을 직접 참조하지 않는다.

### Diary 연결

- Diary는 생성 이력 조회, 사용 가능 여부, 경로 변경과 일기 연결의 목적별 Creative In Port를 사용한다.
- Creative는 생성 이력과 파일 참조를 소유하고 Diary는 일기 사진 목록과 공개 범위에 따른 배치 정책을 소유한다.

## 오류와 API 표현

- 일일 생성 한도 초과, 선택 캐릭터 사용 불가, 참조 이미지 부재, 외부 생성 실패, 저장 실패와 생성 이력 부재는 기술 중립 Creative 오류 코드로 표현한다.
- 존재하지 않는 선택 캐릭터와 다른 사용자 소유 캐릭터는 같은 404 Problem Details로 표현하고 유형, 실제 소유자와 저장 참조를 노출하지 않는다.
- Character, User와 Object Storage의 예상하지 못한 기술 장애는 선택 캐릭터 사용 불가로 축소하지 않고 전역 오류 경계로 전달한다.
- 외부 Provider 응답 본문, Base64 이미지와 저장소 내부 오류 메시지는 Application 오류와 로그에 포함하지 않는다.
- Web Adapter는 Creative 오류를 RFC 9457 Problem Details로 변환한다.
- AI 생성 성공 응답과 잔여 횟수 응답의 기존 JSON 필드는 유지한다.
