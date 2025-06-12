# piku-back

Piku 프로젝트의 백엔드 저장소입니다.

## 🚀 시작하기

### 사전 요구 사항

- [Docker](https://www.docker.com/get-started)가 설치되어 있어야 합니다.

### 설치 및 실행

1.  **저장소를 복제합니다.**

    ```bash
    git clone https://github.com/your-username/piku-back.git
    cd piku-back
    ```

2.  **환경 변수 파일을 생성합니다.**

    프로젝트 루트 디렉토리에 `.env` 파일을 생성하고 아래 내용을 복사하여 붙여넣으세요. 이 내용은 `.env.sample`을 기반으로 합니다.

    ```bash
    # .env

    # Dev Environment
    DEV_DB_PASSWORD=your_dev_db_password

    # Prod Environment
    PROD_DB_URL=
    PROD_DB_USERNAME=
    PROD_DB_PASSWORD=

    # JWT
    JWT_KEY=your_jwt_secret_key
    ```

    - `DEV_DB_PASSWORD`: 개발 환경 데이터베이스의 비밀번호를 입력합니다.
    - `JWT_KEY`: JWT 서명에 사용할 시크릿 키를 입력합니다.

3.  **Docker Compose를 사용하여 애플리케이션을 실행합니다.**

    다음 명령어를 실행하여 `dev` 프로필로 서비스를 시작합니다.

    ```bash
    docker compose --profile dev up -d
    ```

    **참고:** `docker-compose.yml` 설정에 따라 위 명령어는 **개발용 데이터베이스(`db`) 서비스만 실행**합니다. 웹 서버를 포함한 전체 개발 환경을 실행하려면 다른 프로필(`dev-server`)을 사용해야 할 수 있습니다.

    ```bash
    docker compose --profile dev-server up -d
    ```

## 🌱 개발 규칙

### 브랜치 전략

- 기능 단위로 브랜치를 생성하여 작업합니다.
- 브랜치 이름은 아래 컨벤션을 따릅니다.

#### 브랜치 이름 컨벤션

- `feature/기능명`: 새로운 기능 개발
- `fix/수정내용`: 버그 수정
- `docs/문서내용`: 문서 추가 또는 수정
- `refactor/리팩토링내용`: 코드 리팩토링

**예시:**

```bash
git checkout -b feature/login
```

### 커밋 컨벤션

커밋 메시지는 다음 형식을 따릅니다. 이를 통해 커밋 내역을 쉽게 파악하고 변경 사항을 추적할 수 있습니다.

**커밋은 관련된 이슈 번호를 포함해야 합니다.**

#### 커밋 메시지 형식

```
타입: 제목 #이슈번호

본문 (선택 사항)
```

#### 타입(Type)

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅, 세미콜론 누락, 코드 변경이 없는 경우
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드, 리팩토링 테스트 코드 추가
- `chore`: 빌드 업무 수정, 패키지 매니저 수정

**예시:**

```
feat: 로그인 기능 추가 #123

- 소셜 로그인 기능 구현
- JWT 토큰 발급 로직 추가
``` 