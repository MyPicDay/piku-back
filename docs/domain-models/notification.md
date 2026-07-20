# Notification 도메인 모델

- Status: Active
- Audience: Engineers
- Source of Truth: Yes
- Last Reviewed: 2026-07-20

## 도메인 개요

Notification 도메인은 **서비스에서 발생한 알림 대상 사건을 사용자에게 전달하기 위한 알림 이력과 기기 토큰을 관리**하는 도메인입니다.

### 목적

- 친구 요청 및 수락, 좋아요, 댓글, 답글, 친구 일기 등의 알림 대상 사건을 수신 사용자에게 전달한다.
- 알림 이력(`Notification`)을 보존하여 사용자가 읽지 않은 알림을 확인하거나 읽음·삭제 처리할 수 있도록 한다.
- FCM 기기 토큰(`FcmToken`)을 관리하여 사용자가 앱을 사용하지 않는 상태에도 푸시 알림을 발송할 수 있는 기반을 제공한다.

### 핵심 책임

- 알림(`Notification`) 생성, 단건 읽음 처리, 전체 읽음 처리, 소프트 삭제 관리
- 알림 이력 저장이 확정된 뒤 실시간 알림과 푸시 알림 전달 허용
- 알림 타입(`NotificationType`)에 따른 메시지 문자열 자동 생성
- 유효하지 않은 기기 토큰 제거
- 사용자별 FCM 기기 토큰(`FcmToken`) 등록 및 갱신
- 익명 일기 관련 알림의 발신자 비식별 처리

### Application 구조

Notification Application은 외부 행위자의 의도에 따라 기록, 목록 조회, 읽음, 삭제,
전달, 스트림 구독과 Push Token 관리 유스케이스를 분리한다. Web·Social 이벤트·Diary와
User의 요청은 In Port를 통해 진입하며, 저장소·SSE·FCM·트랜잭션 완료 시점과 다른
Context 조회는 Notification이 소유한 목적별 Out Port 뒤에 둔다.

- `NotificationRecordingService`는 알림 이력을 기록하고 전달 요청을 예약한다.
- `NotificationPageQueryService`와 `NotificationListAssembler`는 알림 원본 Page를
  Notification 목록 표현으로 조합한다.
- `NotificationReadService`, `NotificationDeletionService`는 읽음과 삭제 생명주기를
  조정한다.
- `NotificationDeliveryService`는 SSE와 Push 전달을 best-effort로 수행한다.
- `NotificationStreamSubscriptionService`는 사용자별 SSE 연결과 초기 요약 전달을
  조정한다.
- `PushTokenService`는 운영·비운영 환경 구현과 무관한 Token 등록·해제 의도를
  제공한다.

알림 전달 시점은 `ScheduleNotificationDeliveryPort`가 표현한다. Spring 트랜잭션
동기화는 이 Port를 구현하는 Outgoing Adapter만 사용하며 Application Service는
프레임워크의 트랜잭션 완료 API를 직접 사용하지 않는다.

### 도메인 경계

- **Aggregate Root**: `Notification`, `FcmToken` (각각 독립적 Aggregate)
- `Notification`은 이벤트 발생 시점의 스냅샷으로, 생성 이후 알림 내용은 불변이다. 읽음 상태(`isRead`)와 삭제 생명주기(`deletedAt`)만 변경할 수 있다.
- 실시간 연결 관리와 푸시 공급자 호출은 Notification 모델의 책임이 아니다.
- 알림의 원인이 되는 업무 사실과 대상 컨텐츠의 상태는 Notification이 소유하지 않는다.
- 알림 원인이 익명 일기인지 여부는 Diary가 제공한 조회 맥락을 사용하며 Notification이 일기 공개 범위를 독자적으로 판단하지 않는다.
- User와 Diary의 내부 Entity·Repository를 직접 사용하지 않는다. Notification
  Cross-context Adapter가 공급자의 공개 Application 계약을
  `NotificationSenderView`, `NotificationDiaryContextView`로 번역한다.
- Social의 알림 대상 사건은 Social Application의 공개
  `SocialNotificationEvent` 계약으로 수신하고 Notification 명령으로 번역한다.

---

## 알림 타입(NotificationType)

_Enum_

### 상수

- `FRIEND_REQUEST` : 친구 요청 수신 알림
- `FRIEND_ACCEPT` : 내 친구 요청에 대한 수락 알림
- `COMMENT` : 내 일기에 달린 새로운 댓글 알림
- `REPLY` : 내 댓글에 달린 새로운 답글 알림
- `FRIEND_DIARY` : 내 친구가 새로 작성한 일기 등록 알림
- `LIKE` : 내 일기에 추가된 좋아요 알림

---

## 팝업/푸시 알림(Notification)

_Entity_

### 속성

- `id` : Long. 단일 알림 메시지의 고유 식별자
- `receiverId` : String. 알림을 수신받아야 하는 대상 사용자의 식별자
- `senderId` : String. 알림 발생을 유발한 주체 사용자의 식별자
- `type` : NotificationType. 발생한 알림의 성격/종류
- `diaryId` : Long. 알림의 원인이 된 일기(Diary)의 식별자 (이벤트에 따라 Null 가능)
- `isRead` : Boolean. 수신자의 알림 확인/읽음 처리에 대한 여부
- `createdAt` : LocalDateTime. 알림 생성 일시
- `updatedAt` : LocalDateTime. 최종 수정 일시
- `deletedAt` : LocalDateTime. `Notification`이 소유하는 삭제 처리 일시

### 행위

- `Notification(receiverId, senderId, type, diaryId)` : 특정 사용자에게 보낼 새로운 알림 객체를 생성한다.
- `markAsRead()` : 해당 알림을 수신자가 읽음 처리한다.
- `delete()` : 알림을 논리적 삭제 처리하고 삭제 일시를 기록한다.
- `isDeleted()` : 알림이 삭제 처리되었는지 여부를 반환한다.

### 규칙

- 알림이 최초로 생성될 때 읽음 여부(`isRead`)는 항상 `false` (안 읽음) 상태로 초기화된다.
- 수신자(`receiverId`)는 필수 값으로 결측될 수 없다.
- 생성된 알림 내용은 불변이며, 읽음 상태(`isRead`)는 읽음 처리(`markAsRead()`)를 통해서만 변경한다.
- 알림 이력 저장이 확정되지 않은 상태에서는 외부 전달을 시도하지 않는다.
- 알림 삭제 시 물리적 데이터 삭제 대신 `deletedAt` 값을 갖는 논리적 삭제 구조를 따른다.

### 익명 일기 알림 정책

- 익명 일기가 새로 작성되었을 때는 작성자를 추정할 수 있는 `FRIEND_DIARY` 알림을 생성하지 않는다.
- 익명 일기의 댓글, 답글과 좋아요 알림은 기존 수신 대상에게 전달하되 외부 알림 표현의 발신자를 `익명`으로 표시한다.
- 알림 목록, 실시간 이벤트와 푸시 본문에는 실제 발신자 식별자, 닉네임과 아바타를 포함하지 않는다.
- 내부 알림 이력의 발신자 식별자는 전달 대상과 권한 판단을 위해 유지할 수 있지만 외부 표현에 노출하지 않는다.
- 익명 일기와 연결된 알림은 관련 일기 식별자를 통해 상세로 이동할 수 있으나 작성자 프로필로 연결하지 않는다.

---

## FCM 기기 토큰(FcmToken)

_Entity_

### 속성

- `id` : Long. 기기 토큰 할당 이력의 고유 식별자
- `userId` : String. 해당 기기를 소유 및 사용 중인 사용자 식별자
- `token` : String. 푸시 알림 서버(FCM)로부터 발급된 기기 별 고유 전달 토큰
- `deviceId` : String. 사용자가 접속한 기기(스마트폰 등)의 자체 식별자

### 행위

- `updateToken(String newToken)` : 만료되거나 갱신된 푸시 알림 토큰으로 값을 수정한다.

### 규칙

- 하나의 기기(`deviceId`)를 식별하여 해당 기기에 대한 푸시 토큰(`token`)을 관리한다.
- 특정 기기의 토큰은 만료될 수 있으며, 새 토큰이 발급되면 기존 값을 교체한다.
- `prod` 환경에서는 JPA Adapter가 Token을 저장·조회·해제한다.
- `!prod` 환경에서는 현재 Token 등록·조회·해제를 무저장으로 처리한다. 개발용 Token
  저장은 별도 기능 변경으로 다룬다.

---

## 조회와 전달 경계

- 알림 목록은 저장된 `Notification` Page를 먼저 조회한 뒤 User 발신자 요약과 Diary
  맥락을 일괄 조회해 조합한다.
- 연결된 Diary 맥락이 사라진 알림은 현재 목록에서 제외한다. 현재 Page에서 제외된
  수만큼 응답의 전체 개수를 조정하는 기존 동작을 유지한다.
- 닉네임, 아바타, 일기 식별자, 썸네일, 일기 날짜와 작성자 식별자처럼 원인 사건에
  따라 존재하지 않을 수 있는 응답 메타데이터는 `null`을 허용한다.
- 알림 저장이 커밋된 뒤 SSE와 Push 전달을 시작한다. SSE 실패가 Push 시도를 막지
  않으며, 개별 Push 실패 Token은 해제를 시도한다.
- 외부 전달은 현재 best-effort다. 재시도, Outbox, 중복 방지와 전달 보장 강화는
  별도 기능·운영 설계 범위다.
