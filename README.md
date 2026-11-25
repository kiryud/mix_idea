# Idea Support Server

학생 아이디어 보조 앱에 연결하는 Kotlin / Spring Boot / MySQL 백엔드입니다.
기존 자체 회원·JWT 인증을 사용하며, 기능별 Controller → Service → Component 구조로 구현했습니다.

## 구현한 기능

- 카테고리 조회·추가·연관 단어 미리보기
- 단어 3개 추출, 최대 2개 고정, 방향성·리롤
- AI 아이디어 생성, 같은 조합 결과 재사용, 동시 생성 공유, 다시 조합
- 분야별 후속 가이드 생성·재사용: 조리법·구현 가이드·공모전 기획안·실행 계획
- 링크 공유 HTML, 최신순·인기순 피드, 내 글·반응 집계, 반응 변경·취소
- 기존 회원가입·로그인·토큰 갱신·본인 프로필
- MySQL 스키마·기본 단어 자동 적재, Swagger, 호출 제한·공통 오류

기본 카테고리는 음식 개발 149개, 프로그램 개발 152개, 공모전 주제선정 148개입니다.
공통 단어는 중복을 합쳐 441개이며 새 DB에 Liquibase가 자동 적재합니다. 일반 실행에서 시연 회원을 자동 생성하지 않습니다.
아이디어 기록 저장은 학생 프론트엔드의 localStorage/SQLite가 담당합니다.

## 실행

Java 17 JDK와 MySQL 8.0이 필요합니다. 기존 기반은 Kotlin 1.9.21 / Spring Boot 3.2.1 / Gradle 8.5입니다. React 시연에는 Node.js 20 이상도 필요합니다.
현재 환경에는 Java·MySQL·Docker가 자동 포함되어 있지 않을 수 있습니다. `scripts/start-local.ps1`, `scripts/build.ps1`, `scripts/test.ps1`는 실행 전에 Java 17을 확인하고, Docker 실행을 선택한 경우 Docker Desktop 엔진도 확인합니다.

처음 실행할 때는 환경 파일을 만듭니다. 템플릿은 안전한 로컬 mock AI와 Docker MySQL 기본 포트(3307)를 사용합니다.

```powershell
Copy-Item src/main/resources/.env.template src/main/resources/.env
```

Docker Desktop을 사용할 수 있는 경우:

```powershell
.\scripts\start-local.ps1 -UseDocker
```

Docker 실행도 `src/main/resources/.env`를 사용하며 MySQL은 기본적으로 `127.0.0.1:3307`, API는 8080입니다.
기존 MySQL을 사용하는 경우 resources 안 `.env`의 DB 주소·계정·비밀번호를 수정합니다.
DB는 사전에 생성하고 해당 DB에 대한 테이블 생성·조회·수정 권한을 부여해야 합니다.

```powershell
.\scripts\start-local.ps1
```

- Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- 상태: [http://localhost:8080/common/health](http://localhost:8080/common/health)
- 요청 예제: [docs/requests.http](docs/requests.http)

Swagger에서 회원가입 → 로그인 후 accessToken을 Authorize에 넣어 업무 API를 호출합니다.
실제 Gemini 설정은 `src/main/resources/.env`에서 `AI_MODE=gemini`, `GEMINI_API_KEY`, `AI_MODEL`을 지정한 뒤 재시작합니다.
모델은 해당 계정에서 사용 가능한 값을 입력합니다. 실제 모드 실패를 mock 성공으로 바꾸지 않습니다.
이 PC의 실제 환경 파일에는 `JWT_SECRET`, `SHA_KEY`, `AES_KEY`도 생성해 넣었습니다. 다른 환경은 각각 별도의 비밀값을 사용합니다.

## 문서와 유지보수

- [테스트와 React 시연 안내](docs/TESTING_AND_DEMO.md): Red → Green → Refactor 기록·빌드 결과·학생 실습
- [분야별 후속 가이드](docs/CATEGORY_GUIDES.md): 버튼별 제공 내용·사용자 카테고리 유형·신규 API
- [별도 React 데모](demo-web/README.md): 실제 백엔드를 연결한 클릭 시연 페이지
- [학생용 API 흐름 안내](docs/student-guide/API_USER_FLOW.md): 화면별 호출 순서·JSON 예제·ID 전달·오류 처리
- [배포용 Word 안내서](docs/student-guide/idea-support-api-user-flow.docx): 전체 사용자 흐름을 담은 10페이지 문서
- [온라인 MySQL ERD와 수정 원본](docs/schema/README.md): dbdiagram.io 관계도·DBML·현재 DB 구조
- [API 명세](docs/API_SPEC.md): 요청·응답·오류·프론트 연결
- [구성 및 처리 규칙](docs/BACKEND_PLAN.md): 패키지 역할·MySQL 구조·동시성·제약
- [작업 기록](docs/WORK_LOG.md): 검증 결과와 변경 내역
- [개발 규칙](Convention.md)
- [요구사항 원문](https://app.notion.com/p/3f89fa2a819d8048a0e6fca0e6645436)

기본 패키지와 Gradle group은 `com.ideasupport`, 프로젝트명은 `idea-support-server`입니다.
Controller는 기존 `@Api`/`@Auth`를 쓰고, Service는 컴포넌트 조합을, Component는 개별 처리를 맡습니다.
DTO·Entity·Repository는 각각 기존 `core/dto`, `core/entity`, `infra/repository`에 있습니다.

## 학생용 React 시연

[실습 화면 열기](http://127.0.0.1:5173/) — `demo-web`의 별도 React 페이지입니다. 기존 학생 프로젝트와 독립되어 있습니다.

```powershell
# 백엔드를 실행한 상태에서 별도 창으로 시작
.\scripts\start-demo.ps1
```

가입·로그인 → 카테고리 → 단어 고정·리롤 → Gemini 생성 → 확정·만드는 법 → 로컬 기록·링크·피드 → 다른 계정의 반응을 직접 눌러볼 수 있습니다. 오른쪽 LIVE LAB에서 실제 요청·응답을 확인합니다.

## 검증

```powershell
.\scripts\build.ps1 # 백엔드 + React 테스트 + 스타일 검사 + 두 빌드
.\scripts\test.ps1
# 폐기 가능한 별도 MySQL 테스트 DB가 있을 때 통합 테스트도 실행
$env:IDEA_TEST_DB_USER = 'YOUR_TEST_USER'
$env:IDEA_TEST_DB_PASSWORD = 'YOUR_TEST_PASSWORD'
.\scripts\test.ps1 -MySqlTestUrl 'jdbc:mysql://127.0.0.1:3306/idea_support_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
```

MySQL URL이 없으면 이 Windows PC의 격리 MySQL 테스트 DB를 33307 포트에 자동 준비합니다. 다른 환경은 `_test`로 끝나는 별도 MySQL DB 주소·계정을 지정합니다. DB 준비 실패를 건너뛰지 않고 빌드를 실패시킵니다. 일반 회원 기반 테스트 일부는 H2를 사용합니다.
실제 MySQL 8.0에서 마이그레이션·업무 API·동시 생성·캐시·공유·피드·반응을 확인했습니다.
실제 Gemini 아이디어 생성·만드는 법·같은 조합 재사용까지 확인했습니다. 별도 `demo-web`을 추가했으며 학생의 기존 프론트 소스 연동·기기 설치는 미검증입니다.
Windows의 Java 임시 경로 문제를 피하는 설정은 실행·테스트 스크립트에 포함했습니다.

## 컨테이너 빌드

기본 이미지명은 `local/idea-support-server`입니다. 원격 대상은 `containerImage` 또는 `CONTAINER_IMAGE`로 지정합니다.
Cloud Build 파일을 사용하려면 `_CONTAINER_IMAGE` 치환값이 필요합니다. 이번 작업에서 원격 업로드·배포는 하지 않았습니다.

## 업데이트 위치

- React 시연: `demo-web/src`, 프록시: `demo-web/vite.config.js`·`proxy-paths.js`
- 테스트: `src/test`·`demo-web/src/*.test.*`, 빌드 설정: `build.gradle.kts`·`scripts/build.ps1`
- 백엔드: `src/main/kotlin/com/ideasupport`
- MySQL 스키마·시드: `src/main/resources/db` — 적용한 변경 이력은 수정하지 말고 새 changeset을 추가합니다.
- 환경: `src/main/resources/.env`·`.env.template` 및 `src/main/resources/application*.yaml`
- 프론트: API 기본 주소·기존 로그인/갱신·DTO·오류 처리
- API 변경 시 `docs/API_SPEC.md`·Swagger·`docs/student-guide`의 흐름 안내, DB 변경 시 `docs/schema/idea-support.dbml`과 온라인 ERD를 함께 갱신합니다. 구성 변경은 `docs/BACKEND_PLAN.md`, 작업 결과는 `docs/WORK_LOG.md`에 기록합니다.

실제 `.env`는 Git과 JAR에서 제외합니다. 실행 파일만 옮길 때는 환경변수로 설정하거나 프로젝트의 resources 환경 파일 경로를 유지합니다. 키 발급·실제 연결 결과는 [Gemini 연결 안내](docs/GEMINI_SETUP.md)를 참고합니다.
