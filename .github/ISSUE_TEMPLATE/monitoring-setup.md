---
name: monitoring-setup
about: Prometheus + Grafana 기반 모니터링 시스템 구축 및 Discord 알림 연동
title: "[feature] Prometheus + Grafana 모니터링 시스템 구축"
labels: enhancement, infrastructure, monitoring
assignees: ''

---

## 📋 어떤 기능인가요?

서버 상태를 실시간으로 모니터링하고, 장애 발생 시 Discord 웹훅으로 알림을 받을 수 있는 모니터링 시스템을 구축합니다.

### 주요 기능
- Prometheus를 통한 Spring Boot 메트릭 수집
- Grafana 대시보드로 시각화 (CPU, 메모리, HTTP 요청, DB 커넥션 등)
- 서버 다운 시 Discord 웹훅 알림
- Actuator 엔드포인트 IP 제한으로 보안 강화

## ✅ 구현 목록

### 1. Prometheus 설정
- [x] `monitoring/prometheus/prometheus.yml` 생성
- [x] Spring Boot 메트릭 scrape 설정 (`/actuator/prometheus`)
- [x] Docker 환경에서 호스트 접근을 위한 `host.docker.internal` 설정
- [x] 15초 간격 메트릭 수집

### 2. Grafana 설정
- [x] Grafana 대시보드 프로비저닝 설정
- [x] Prometheus 데이터소스 연결
- [x] 알림 규칙 설정 (`health-check-rules.yaml`)
  - 서버 다운 감지 (`up{job="piku-back"} == 0`)
  - 1분 이상 다운 시 알림 발생
- [x] Discord 웹훅 contact point 설정

### 3. Spring Boot 설정
- [x] Actuator 엔드포인트 활성화 (`/actuator/health`, `/actuator/prometheus`)
- [x] Micrometer Prometheus 의존성 추가
- [x] IP 기반 접근 제어 구현
  - localhost 항상 허용
  - 사설 IP 대역 허용 (192.168.0.0/16, 172.18.0.0/16)
  - 환경변수로 허용 IP 관리 (`MONITORING_ALLOWED_IPS`)
- [x] 헬스 체크 상세 정보 보안 설정 (`when-authorized`)

### 4. 불필요한 코드 제거
- [x] `ServerHealthScheduler.java` 삭제 (자가 체크 로직 불필요)
- [x] `HealthCheckProperties.java` 삭제
- [x] `application.yml`에서 `monitoring.health-check` 설정 제거

### 5. Grafana 대시보드 구축
- [x] `piku-backend-dashboard.json` 생성
- [x] 서버 상태 패널 (UP/DOWN)
- [x] HTTP 요청률 그래프
- [x] 평균 응답 시간
- [x] JVM 메모리 사용량
- [x] JVM 스레드 수
- [x] CPU 사용률 (System/Process)
- [x] DB 커넥션 풀 상태

### 6. Docker Compose 설정
- [x] Prometheus 컨테이너 추가
- [x] Grafana 컨테이너 추가
- [x] Discord 웹훅 환경변수 설정 (`DISCORD_WEBHOOK_URL`)
- [x] 볼륨 마운트 설정 (데이터 영속성)

### 7. 환경 변수 설정
- [x] `.env.sample`에 `DISCORD_WEBHOOK_URL` 추가
- [x] `.env.sample`에 `MONITORING_ALLOWED_IPS` 추가
- [x] 주석으로 사용법 설명

## 📊 모니터링 아키텍처

```
Spring Boot (IntelliJ)
    ↓ (expose metrics)
/actuator/prometheus
    ↓ (scrape every 15s)
Prometheus (Docker)
    ↓ (query metrics)
Grafana (Docker)
    ↓ (alert if down > 1min)
Discord Webhook
```

## 🔒 보안 설정

### Actuator 엔드포인트 접근 제어
- **허용 IP**:
  - `127.0.0.1` (localhost)
  - `192.168.0.0/16` (사설 IP 대역)
  - `172.18.0.0/16` (Docker 브리지 네트워크)
  - 환경변수로 추가 IP 설정 가능

### 헬스 체크 상세 정보
- `show-details: when-authorized` - 인증된 요청만 상세 정보 제공
- IP 제한과 조합하여 외부 노출 방지

## 📁 변경된 파일

### 신규 생성
- `monitoring/prometheus/prometheus.yml`
- `monitoring/grafana/provisioning/datasources/datasource.yml`
- `monitoring/grafana/provisioning/dashboards/dashboard.yml`
- `monitoring/grafana/provisioning/alerting/health-check-rules.yaml`
- `monitoring/grafana/provisioning/alerting/notification-policies.yaml`
- `monitoring/grafana/provisioning/alerting/contact-points.yaml`
- `monitoring/grafana/dashboards/piku-backend-dashboard.json`

### 수정
- `docker-compose.yml` - Prometheus, Grafana 서비스 추가
- `src/main/java/store/piku/back/global/config/SecurityConfig.java` - Actuator IP 제한
- `src/main/resources/application.yml` - Management 엔드포인트 설정
- `.env.sample` - Discord 웹훅, 모니터링 IP 환경변수 추가

### 삭제
- `src/main/java/store/piku/back/global/monitoring/ServerHealthScheduler.java`
- `src/main/java/store/piku/back/global/monitoring/HealthCheckProperties.java`

## 💬 참고사항

### 설정 방법

1. `.env` 파일에 Discord 웹훅 URL 추가:
   ```bash
   DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/YOUR_ID/YOUR_TOKEN
   ```

2. Docker 컨테이너 시작:
   ```bash
   docker compose --profile dev up -d
   ```

3. 접속 URL:
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3010 (admin/admin)

### 알림 테스트
서버를 중지하면 1분 후 Discord로 알림이 전송됩니다.

### 추가 개선 가능 사항
- [ ] 응답 시간 임계값 알림 추가
- [ ] 메모리 사용량 임계값 알림 추가
- [ ] 에러율 모니터링 및 알림
- [ ] Slack, Email 등 추가 알림 채널 연동
- [ ] cAdvisor 추가 (Docker 컨테이너 메트릭)

## 🔗 관련 이슈

#166
