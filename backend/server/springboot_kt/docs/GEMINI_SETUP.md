# 실제 Gemini·DB 연결 안내

갱신: 2026-10-27 (한국 시간)

## 현재 연결

- 환경 파일: `src/main/resources/.env`
- 예제 파일: 같은 폴더의 `.env.template`
- MySQL: 이 프로젝트 전용 `127.0.0.1:33306`, DB `idea_support`, 회원 `idea_support`
- DB 비밀번호·JWT/SHA/AES 키: 실제 환경 파일에 설정되어 있음
- AI_MODE: `gemini`
- AI_MODEL: `gemini-3.5-flash-lite`
- Gemini 키: 사용자 승인 후 Google AI Studio의 `idea-support-server` 프로젝트에 생성하여 실제 환경 파일에 저장

실제 비밀값은 이 문서·템플릿·채팅에 기록하지 않는다. `.env`는 Git과 JAR에서 제외한다.
Spring Boot가 해당 경로의 파일을 properties 형식으로 직접 읽는다. 별도 루트 `.env`나 외부 dotenv 라이브러리는 필요 없다.
IDE 실행 시 작업 디렉터리는 프로젝트 루트로 둔다.

## 실행

```powershell
.\scripts\start-local.ps1
```

이 PC에서는 필요한 경우 프로젝트 전용 MySQL을 먼저 시작하고 서버를 실행한다.
다른 PC에서는 resources의 템플릿을 같은 폴더의 `.env`로 복사해 해당 PC의 DB·Gemini 값을 넣는다.
실제 키가 들어 있는 `.env`를 다른 학생에게 공유할지는 별도로 결정하고, 템플릿에는 복사하지 않는다.
Docker를 선택할 경우에도 스크립트가 resources의 환경 파일을 `--env-file`로 전달한다.
이미 전용 MySQL이 33306 포트를 사용 중이면 같은 포트에 Docker MySQL을 동시에 띄우지 않는다.

- [Swagger](http://localhost:8080/swagger-ui/index.html)
- [상태 확인](http://localhost:8080/common/health)
- [Google AI Studio 키 관리](https://aistudio.google.com/api-keys)

Swagger에서 회원가입 → 로그인 → Authorize에 accessToken 입력 후 요청한다.
`docs/requests.http`도 사용할 수 있다.

## 실제 검증

실제 Google API로 키 인증·사용 가능한 모델 목록을 조회한 후, 실제 서버 API를 통해 확인했다.

| 확인 | 결과 |
| --- | --- |
| DB·기본 단어 준비 | health status=ok, database=up, seedReady=true |
| 실제 AI 모드 | health aiMode=gemini |
| 아이디어 생성 | 201, source=gemini, 약 1,832ms |
| 같은 조합 재요청 | cached=true, 약 39ms |
| 만드는 법 | 201, source=gemini, 약 1,962ms |
| 다시 조합 | 새 Gemini 결과가 parentIdeaId로 원본과 연결되어 저장됨 |
| 연관 단어 AI 매칭 | 건강 관리 주제에서 15개 선택, 약 2,705ms |
| 사용자 카테고리·추출 | 매칭 결과로 생성하고 3단어 추출 성공 |
| 회귀 테스트 | 16개 통과, 실패·건너뜀 0. 실제 MySQL 통합 5개 포함 |
| 비밀 파일 제외 | Git 제외 확인, 실행 JAR에 실제 .env 파일 0개 |

첫 실제 아이디어 예시: **화산송이 매콤 치즈 감자구이** — 감자·치즈·고추 조합.
시간은 이 PC에서 한 번 측정한 값이며 모든 요청의 속도를 보장하는 수치는 아니다.

후보가 많은 의미 매칭은 기존 5초에서 시간 초과가 확인되어 독립 제한 시간으로 분리했다.
아이디어 5초, 만드는 법 20초, 매칭 15초이며 같은 `.env`에서 조정할 수 있다.
관련 후보가 10개 미만인 카테고리는 억지로 생성하지 않고 422 안내를 반환한다.

## 설정 변경 위치

DB 주소·계정·키·모델·AI 시간 제한은 `src/main/resources/.env`를 바꾸고 서버를 재시작한다.
새 설정 항목은 `.env.template`과 application YAML에도 반영한다.
API 계약은 `docs/API_SPEC.md`, 작업 결과는 `docs/WORK_LOG.md`에 함께 기록한다.
