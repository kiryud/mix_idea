# 작업 기록

## 2026-10-28 — 개인 GitHub 계정 업로드 요청

- 사용자 지정 이메일 `2025014@daelim.ac.kr`의 공개 커밋을 GitHub API로 조회해 연결된 계정이 `MinQDU014`임을 확인했다. 로컬 Git의 작성자 이메일과 GitHub 로그인 계정은 별개이며, 현재 CLI 인증은 `teamsnowballMinQ`였다.
- 최종 업로드 대상은 개인 계정 `MinQDU014/idea-support-server`다. 앞서 만든 조직 저장소로는 푸시하지 않는다. 기존 조직 저장소는 삭제하지 않았다.
- 개인 계정 인증을 위한 GitHub 기기 코드 로그인을 시작하고 사용자에게 `MinQDU014`로 인증하도록 안내했다. 인증 완료 후 개인 비공개 저장소 생성·origin 교체·비밀값 제외 확인·첫 커밋 및 푸시를 진행한다.
- origin을 `https://github.com/MinQDU014/idea-support-server.git`로 수정하고 이 프로젝트의 커밋 작성자를 `MinQDU014 <2025014@daelim.ac.kr>`로 지정했다.
- 재발급한 인증 요청으로 `MinQDU014` 로그인 완료를 확인하고 개인 비공개 저장소를 생성했다. 업로드 대상 227개 파일의 비밀키 패턴·대용량 파일 검사를 통과했으며 실제 `.env`, `.local` 인증 정보와 빌드 결과는 제외했다. 초기 커밋과 main 브랜치 업로드를 완료했고 GitHub API로 커밋·작성자 계정·비밀 설정 파일 제외를 확인했다.
- 이후 업데이트 위치는 `MinQDU014/idea-support-server`의 main 브랜치다. 이번 변경은 Git 연결·업로드 작업이며 애플리케이션 코드 변경이나 빌드 재실행은 없다.

## 2026-10-28 — GitHub 프로젝트 연결 이름 수정

- 원인: 이 프로젝트 안에 `.git`이 없어 Git이 `C:/Users/teamsnowball/.git`을 발견했고, 상위 저장소의 `Team-Snowball/potpicks-prototype-harness` 원격 주소가 표시됐다. Gradle 프로젝트명은 이미 `idea-support-server`였다.
- 작업 전 기존 작업 기록·Convention·경로별 커밋을 확인했다. 상위 저장소의 최근 Markdown 변경은 다른 프로젝트의 작업으로, 이 프로젝트에 적용할 변경은 없었다.
- 현재 폴더에 독립적인 Git 저장소(main)를 초기화하고, GitHub에 비공개 `Team-Snowball/idea-support-server` 저장소를 만들어 origin으로 연결했다. 상위 저장소와 기존 potpicks 원격 저장소는 수정하지 않았다.
- 확인: Git 최상위 경로가 현재 프로젝트이고 fetch/push 원격이 모두 새 주소다. GitHub API에서도 이름과 비공개 상태를 확인했다. 실제 `.env`, `.local` 인증 정보, node_modules의 Git 제외도 확인했다.
- 소스 커밋·푸시는 이번 연결 변경에서 수행하지 않았다. 애플리케이션 코드를 바꾸지 않았으므로 빌드를 다시 실행하지 않았다.
- 업데이트 위치: 현재 프로젝트의 `.git/config` → `https://github.com/Team-Snowball/idea-support-server.git`. IDE에서 예전 이름이 남으면 현재 프로젝트 폴더를 다시 열어 Git 정보를 갱신한다.

## 2026-10-28 — Windows 빌드·인증 오류 진단과 분야별 후속 가이드

- **최신 범위 정정:** 사용자가 분야별 후속 가이드만 수정하도록 요청했다. 아래 빌드 환경·인증 진단의 코드 수정은 진행하지 않는다. 작업 후 기존 별도 테스트 DB 환경값으로 백엔드 빌드만 검증한다.
- 사용자 정정: 비음식의 후속 기능을 없애는 것이 아니라 분야에 적합한 버튼과 내용을 제공해야 한다. 2026-10-27 음식 전용 제한은 후속 가이드의 최종 요구사항이 아니다.
- 작업 전 경로별 최근 커밋·작업 기록·첨부 실패 로그·현재 실행 코드를 확인했다. 이 경로의 최근 변경 Markdown 커밋은 없다.
- 첨부 빌드는 `prepareTestDatabase`에서 `pwsh` 실행 파일을 못 찾아 실패했다. Windows PowerShell 5.1과 기본 설치 경로까지 지원하도록 수정한다.
- 제시한 `minq` 가입은 현재 409 중복 아이디, 로그인은 200이다. 과거 500 원인은 현재 재현하지 못했으므로 추측으로 확정하지 않는다. 프록시·서버·입력·DB 오류를 구분하고 비밀값 없이 원인과 요청 추적 번호를 제공한다.
- 후속 기능: 음식 조리법, 프로그램 구현 가이드, 공모전 기획안, 사용자 카테고리 실행 계획. 사용자 카테고리는 생성 시 지원 유형을 선택하고 기본값은 general이다. 이를 DB와 DTO에 명시해 이름 추측에 의존하지 않는다.
- Red → Green → Refactor로 오류 재현과 회귀 테스트부터 보강한다. DB는 MySQL과 기존 자체 인증을 유지하고 실제 기기 설치는 생략한다.
- 최종 구현: `main/app/guide`의 Controller → Service → Component, 신규 `/app/ideas/{id}/guide`, 기존 `/recipe` 호환 경로를 구성했다. 저장된 유형으로 만드는 법·구현 가이드·공모전 기획안·실행 계획을 구분하며 비음식 후속 기능을 숨기거나 거절하지 않는다.
- 사용자 카테고리 생성·미리보기에 `guideType` 선택을 추가했다. 기본 general, food/software/contest/general을 지원하고 DB v3의 `categories.guide_type`에 저장한다. 같은 이름의 기존 카테고리는 기존 유형을 유지한다.
- Gemini 생성 지시문과 검증을 분야별로 분리했다. 프로그램은 실제 기능·구현·테스트, 공모전은 문제·차별점·검증·제안서, 일반 분야는 첫 실험·단계·성공 기준을 다룬다. 음식은 기존 조리법 검증을 재사용한다.
- React는 항상 해당 분야의 후속 버튼을 제공하고 공유 페이지도 올바른 제목과 준비 항목을 표시한다. guide 전체를 기록에 저장하며 예전 음식 기록은 변환해 읽는다. 과거 타입 없는 plan은 요청 시 현재 분야의 가이드로 교체한다.
- Red: 신규 백엔드 가이드 계약 테스트와 React 네 종류 버튼 테스트의 실패를 확인했다. 이후 가이드 테스트 33개가 통과했으며 Gemini 타입별 응답 디코딩 테스트도 추가했다.
- 최종 검증: 별도 `_test` MySQL 환경값을 지정해 `gradlew.bat build -x demoBuild --console=plain` 성공(47초, 종료값 0). 백엔드 185개, 관련 React 32개 모두 통과, 실패·건너뜀 0. 기존 커버리지 기준·스타일 검사도 통과했다.
- API·구성·학생용 Markdown·로컬 DBML 및 [분야별 가이드 안내](CATEGORY_GUIDES.md)를 갱신했다. 기존 Word·온라인 ERD는 이번 범위에서 재발행하지 않았다.
- 요청한 범위에 따라 빌드 스크립트·인증 코드를 바꾸거나 실행 중인 서버를 재시작하지 않았다. 실제 Gemini 호출과 일반 개발 DB의 수동 변경도 하지 않았다. 백엔드를 재시작하면 새 v3 마이그레이션과 가이드 API가 적용된다.
- 업데이트 위치: `main/app/guide`, `core/dto/app/guide`, `db/v3/001-category-guides.sql`, `demo-web/src/components/IdeaWizard.jsx`·`GuideView.jsx`, `docs/API_SPEC.md`.
## 2026-10-27 — 만드는 법을 음식 전용으로 수정

- 사용자 정정: 공모전·프로그램의 실행 계획이 ‘만드는 법’으로 생성·공유되는 것은 요구 범위를 벗어난다.
- 작업 전 최근 경로별 커밋과 현재 작업 기록·API·구현·테스트를 확인했다. 이 폴더에 해당하는 최근 변경 Markdown 커밋은 없다.
- 먼저 비음식 요청 거절, 기존 잘못 저장된 실행 계획 숨김, React 버튼/기록 표시의 실패 회귀 테스트를 추가한다. 이후 서버 생성 정책·공유 화면·React·학생 안내를 함께 수정한다.
- 기존 데이터는 삭제하지 않고 표시·재사용 대상에서 제외한다. 기기 설치는 생략한다.
- Red 확인: 서버 회귀 25개 중 5개, React 범위 회귀 4개 중 3개가 실패했다. `.local/red-food-only.log`, `.local/red-food-only-react.log`에 실행 증거를 보관한다.
- `RecipeEligibilityComponent`에서 기본 음식 개발 카테고리의 안정된 시드 ID로 자격을 판단한다. 카테고리/아이디어 DTO에 `supportsRecipe`를 추가하고 비음식 요청은 캐시 조회 전에 422/1111로 거절한다. 분야 정보가 없는 사용자 카테고리를 이름으로 추측해 허용하지 않는다.
- Gemini 지시문·출력 스키마와 결과 검증을 food 전용으로 바꿨다. 기존 plan은 표시·재사용에서 제외하고 음식에 잘못 저장된 plan은 정상 food 생성 시 교체한다. 비음식의 기존 plan 행은 삭제하지 않는다.
- React의 버튼·확정 안내·기록 복원·저장은 같은 자격 값으로 제어한다. 공유 HTML도 음식 레시피만 표시한다. 기존 잘못 저장된 공모전/프로그램 레시피도 숨긴다.
- API·구성·학생용 Markdown/Word 안내를 수정했다. Word는 10페이지를 다시 렌더링했고 6페이지 외 9페이지는 이전 검수 이미지와 픽셀이 동일함을 확인했다. 변경된 6페이지는 원본 크기로 검수했다. DB 스키마 변경은 없다.
- 최종 전체 빌드 성공: 백엔드 148개·React 27개, 실패·건너뜀 0, 스타일/커버리지 기준 통과. 업무 라인 99.1%·분기 84.4%다.
- 새 JAR로 API를 재시작했다. 사용자가 제시한 정확한 두 아이디어의 기존 공유 URL을 찾아 브라우저로 열어 ‘만드는 법’이 보이지 않는 것을 확인했다. React에서도 같은 공모전의 상세 결과에 만드는 법 버튼이 없는 것을 확인했다.
- 업데이트 위치: 서버 `main/app/recipe`·`main/app/share`·DTO, React `demo-web/src/components`, 계약 `docs/API_SPEC.md`·`docs/student-guide`. 학생 기존 프론트는 `supportsRecipe`를 기준으로 버튼을 제어하도록 연결한다.

## 2026-10-27 — 테스트 보강과 React 학생 시연 화면

- 요청: Red → Green → Refactor에 맞춰 기능별 테스트를 구현하고 빌드의 성공·실패 표시, 별도 React 클릭 시연 화면을 만든다.
- 작업 전 기존 문서·테스트·Gradle·커밋 상태를 확인했다. 기존 16개 테스트의 범위를 확장하며 이 폴더의 독립 Git 이력은 없다.
- 작업 계획과 실패·성공 증거, 실행법은 [테스트와 시연 안내](TESTING_AND_DEMO.md)에 기록한다. 실제 기기 설치는 생략한다.
- 기존 16개에서 백엔드 테스트를 143개로 확장했고, React 테스트 23개를 추가했다. 단위·HTTP/권한·MySQL 마이그레이션/시드·캐시·동시성·AI 실패·공유·피드·반응·브라우저 저장을 검증한다.
- 실제 Red 실행으로 만드는 법 수량/단계 제목의 내용 검사 누락, 로컬 React Origin 거절, `/s` 프록시의 `/src` 침범을 확인한 후 수정했다. Gemini 네트워크 경계와 공통 MySQL 테스트 준비·정리 책임도 분리했다.
- Gradle `build`가 백엔드 및 React 테스트, 스타일 검사, 두 빌드와 업무 패키지 커버리지 기준을 검사한다. 매 빌드에 테스트별 PASSED/FAILED/SKIPPED와 총계를 출력하고 실패 시 비정상 종료한다. HTML·JUnit XML·JaCoCo 리포트를 생성한다.
- 자동 격리 MySQL 테스트 DB(33307, `_test` 이름 검사)를 준비하고 통합 테스트의 조건부 건너뛰기를 제거했다. 테스트는 실제 `.env`·Gemini를 사용하지 않는다. 일반 개발 DB(33306)는 유지한다.
- 학생 업무 API 패키지 라인 99.1%·분기 84.4%, 전체 라인 75.3%·분기 60.2%를 측정했다. 업무 패키지는 라인 95%·분기 80% 아래에서 빌드를 실패시키도록 설정했다.
- `demo-web`에 독립 React/Vite 실습 화면을 구현했다. 실제 API 요청·HTTP 상태·시간·업무 응답을 보여 주며 인증 토큰과 비밀번호는 숨긴다. API 클라이언트의 한 번 갱신·동시 갱신 공유, 원문 스냅샷 저장과 오류 시 입력 유지도 검증했다.
- 실제 브라우저 클릭으로 두 회원의 가입·로그인, 단어 고정/다시 뽑기, Gemini 생성·만드는 법, 내 기록 저장, 링크 공유·피드 게시·다른 회원 반응 변경/취소를 확인했다. 시간 초과 504 후 재시도 201도 확인했다.
- `scripts/build.ps1`, `scripts/start-demo.ps1`, `demo-web/README.md`, 테스트/시연 안내와 루트 README를 갱신했다. 기존 학생 프론트 자체를 수정하거나 기기에 설치한 것은 아니다.
- 최종 전체 빌드는 1분 12초, 종료값 0, 백엔드 143개·React 23개 모두 통과했다. 실패·건너뜀은 0이며 커버리지 기준과 스타일 검사도 통과했다. 최종 JAR에 실제 `.env`가 없는 것과 문서 링크를 확인했다. API·MySQL·React는 사용자 시연을 위해 실행 상태로 두었다.

## 2026-10-27 — 온라인 ERD와 학생용 API 흐름 문서

- 요청: 유명 무료 온라인 스키마 사이트에서 직접 관계도를 작성하고 사용자 흐름에 맞춘 API 사용 안내를 만든다.
- 작업 전 현재 작업 기록·API 명세·MySQL 마이그레이션·경로별 최근 커밋을 확인했다. 이 폴더의 독립 커밋 이력은 없다.
- 온라인 도구는 dbdiagram.io를 선택했다. 테이블 구조만 사용하며 DB 비밀번호·키·회원 데이터는 전달하지 않는다.
- 실제 MySQL의 컬럼·FK·제약을 대조해 DBML을 남기고, 학생용 Word·Markdown에는 화면별 호출 순서와 상태·ID 전달 관계를 설명한다.
- dbdiagram.io 편집기에 전체 DBML과 프로젝트 제목을 직접 입력하고 정상 렌더링을 확인했다. 비회원 편집기의 계정 저장·공유에는 로그인이 필요해, 공식 DBML-in-Link 기능으로 로그인 없이 재열람 가능한 읽기 전용 온라인 링크를 만들었다. 계정에 저장된 다이어그램은 아니다.
- [온라인 ERD 안내](schema/README.md), [수정용 DBML](schema/idea-support.dbml), [실제 사이트 관계도 이미지](schema/idea-support-erd.png), 비밀값 없는 메타데이터 스냅샷을 보관했다. 현재 MySQL의 11개 테이블·66개 컬럼·12개 FK와 대조했고 nullable 참조도 반영했다. Liquibase 관리 테이블 2개는 제외했다.
- [학생용 Markdown 안내](student-guide/API_USER_FLOW.md)와 [10페이지 Word 안내서](student-guide/idea-support-api-user-flow.docx)를 작성했다. 로그인부터 카테고리·단어 추출·생성·재조합·확정·만드는 법·로컬 기록·링크 공유·피드·반응까지 버튼, 요청, 응답 ID, 다음 화면 상태를 설명한다.
- 기존 자체 인증, 확정 API 없음, 로컬 기록 저장, 링크 글과 피드 글 ID 구분, 반응 취소 시 null 전송, 실패 시 UI 처리도 명시했다. 학생 프론트 소스 자체를 연결해 검증한 것은 아니다.
- Word를 실제 Word 레이아웃으로 PDF 변환한 뒤 제공된 문서 렌더러로 10페이지 모두 이미지화해 배치·표·코드 예제를 확인했다. 제목의 기본 장식선을 제거하고 응답 필드 경로 `idea.source`를 수정했다. 검수용 PDF·페이지 이미지는 `.local`에만 둔다.
- 온라인 링크의 압축 내용을 다시 풀어 DBML 원본과 일치함을 확인했다. 문서의 JSON 예제 9개를 파싱했고 문서·이미지의 로컬 링크도 모두 확인했다. 실행 중인 Swagger와 대조해 학생용 인증·업무·공유·상태 경로가 안내에 포함됨을 확인했다. 운영·개발 전용 경로는 학생 흐름에서 제외했다.
- README와 API 명세에서 새 문서로 연결했다. 백엔드 코드·DB 데이터는 변경하지 않았으므로 서버 테스트를 반복하지 않았다. 연결 기기가 없어 설치·기기 확인은 생략했다.

## 2026-10-27 — resources 환경 설정과 실제 Gemini 연결

- 사용자 요청: `.env`와 기존 `.env.template`을 `src/main/resources`에 두고 DB·Gemini 설정을 채운다. 옆 브라우저에서 키 발급을 돕고 실제 AI까지 검증한다.
- 작업 전 최근 경로별 커밋과 기존 작업 문서·실행 설정·AI 구현을 확인했다. 이 폴더의 독립 Git 이력은 없다.
- 환경 파일의 실제 비밀값은 문서·로그·빌드 산출물에 포함하지 않는다.
- 아래 설정·실제 AI 연결과 회귀 검증을 완료했다.

### 설정과 실제 연결

- `src/main/resources/.env`에 프로젝트 전용 MySQL 주소·계정·비밀번호, Gemini 키·모델, 무작위 JWT/SHA/AES 비밀값을 채웠다. 실제 값은 이 문서에 적지 않는다.
- `.env.template`도 같은 위치에서 관리하고 중복 루트 `.env.example`을 제거했다.
- Spring Boot native config import로 해당 파일을 읽도록 변경하고 외부 dotenv 의존성은 제거했다.
- `processResources`에서 실제 `.env`를 제외하고 Git 제외도 확인했다. 템플릿은 포함되지만 비밀 파일은 JAR에 포함되지 않는다.
- 프로젝트 MySQL은 `127.0.0.1:33306`, DB는 `idea_support`이다. 기존 MySQL 서비스는 수정하지 않았다.
- `scripts/start-mysql.ps1`로 프로젝트 DB를 다시 시작하며 기본 시작 스크립트가 이를 이용한다. Docker 선택 시에도 resources 환경 파일을 전달한다.
- 사용자가 키 생성과 로컬 저장을 명시적으로 승인했다. 옆 Google AI Studio에서 `idea-support-server` 프로젝트와 키를 만들고 실제 환경 파일에 저장했다. 결제 활성화는 하지 않았다.
- 실제 키로 모델 목록을 조회하고 `gemini-3.5-flash-lite`의 사용 가능 여부를 확인했다.
- Spring Security의 임의 비밀번호 자동 생성은 제외하고 기존 자체 인증만 사용한다.

### 실제 AI 검증

- resources 환경 파일만 읽는 실제 서버에서 health가 DB up·gemini·seedReady true임을 확인했다.
- 실제 아이디어 생성 201/source=gemini, 1,832ms. 생성 예시: 화산송이 매콤 치즈 감자구이.
- 같은 조합 재요청은 cached=true, 39ms로 결과를 재사용했다.
- 실제 만드는 법 201/source=gemini, 1,962ms. 재조합도 원본 parentIdeaId로 연결된 새 Gemini 결과를 저장했다.
- 큰 후보 풀의 AI 매칭에서 기존 5초 제한 초과를 확인하고 매칭 제한을 15초로 분리했다. 출력도 핵심 10~20개로 좁히고 매칭 캐시 버전을 갱신했다.
- 건강 관리 주제에서 실제 AI가 연관 단어 15개를 선택(2,705ms)했고 사용자 카테고리 생성·3단어 추출에 성공했다.
- 후보가 부족한 주제는 422를 반환하는 것도 확인했다. 관계없는 단어를 채워 정상처럼 처리하지 않는다.
- 실제 Gemini 설정이 존재하는 상태에서도 회귀 테스트는 별도 mock·테스트 DB로 분리되어 16개 통과, 실패·건너뜀 0. 실행 JAR의 실제 `.env` 파일은 0개다.
- 비밀 파일을 제외한 실행 JAR로 다시 기동해 정상 연결을 확인했다. 코드 스타일 검사·빌드도 통과했다.
- Swagger는 학생용 app 그룹이 기본으로 표시되게 정리했다.
- 최종 실행본에서 `/common/health`의 gemini·DB up, Swagger 기본 그룹 app, JAR의 비밀 환경 파일 0개를 재확인했다. 사용자가 바로 호출할 수 있도록 API 8080·프로젝트 MySQL 33306은 실행 상태로 두었다.

### 업데이트 위치

- 현재 DB·Gemini·모델·시간 제한은 `src/main/resources/.env`, 공개 설정 예제는 같은 폴더의 `.env.template`.
- 실행·측정 결과는 `docs/GEMINI_SETUP.md`, API 계약은 `docs/API_SPEC.md`, 구조는 `docs/BACKEND_PLAN.md`.
- 학생 프론트에는 API 주소·기존 로그인·요청/응답을 연결하면 된다. 실제 기기 설치는 사용자 지시대로 생략했다.

## 2026-10-27 — 업무 API 실제 구현 시작

- 사용자가 실제 개발을 요청했다. 기능 패키지 내부의 Controller → Service → Component 구조를 따른다.
- Controller는 기존 `@Api`, `@Auth`와 Swagger를 활용한다. Service는 컴포넌트 연결·트랜잭션 흐름 조합을 맡고, 세부 역할은 Component에 둔다.
- DTO는 `core/dto`, Entity는 `core/entity`, Repository는 `infra/repository`에 배치한다.
- MySQL·기존 자체 인증을 유지하고 카테고리, 추출, AI 생성·재사용·재조합, 만드는 법, 공유·피드·반응을 구현한다.
- 작업 전 계획·API·기존 코드·Convention을 읽었다. 이 폴더의 최근 커밋 이력은 여전히 없다.
- 실제 기기 설치는 생략했다.

### 구현 완료

- 기능별 `category`, `word`, `idea`, `recipe`, `post`, `share`, `health`의 Controller·Service·Component를 추가했다. 기존 user 기능도 동일한 책임 분리로 정리했다.
- Service는 Repository를 직접 호출하지 않는다. DTO·Entity·Repository는 기존 위치에 추가했다.
- 카테고리·미리보기·생성, 추출·고정·방향성, 아이디어 생성·재사용·재조합·조회, 만드는 법, 공유 HTML·피드·반응을 구현했다.
- 공통 생성 조정, 회원/IP 호출 제한, AI 입력·출력 검증, 실제 Gemini 어댑터와 명시적 mock 어댑터를 추가했다.
- 기존 JWT의 경로·응답·비밀번호 처리 방식을 유지했다. 서명·만료·issuer·토큰 종류·회원 존재 검증을 보완하고 local 인증 우회와 본인 프로필의 ADMIN 제한을 해제했다. 기존 토큰은 다시 로그인해야 한다.
- 개발 전용 비밀번호 조회·IP 초기화·오류/락 시연 경로는 차단했다. 비밀번호·토큰·요청 본문을 로그에 남기지 않도록 정리했다.
- MySQL 업무 테이블 9개와 FK·UNIQUE·조회 인덱스, 기본 시드를 추가했다. 음식 149개·프로그램 152개·공모전 148개, 공통 단어 441개·카테고리 연결 449개다.
- 초안의 별도 키워드·메타데이터 테이블은 작은 단어 풀에 맞춰 words.keywords와 후보 내용 해시로 단순화했다.
- `@Api`의 매핑·Swagger 속성에 AliasFor를 명시하고, API 문서에 공개 공유 그룹을 추가했다.
- Compose와 `.env.example`, 로컬 시작·테스트 스크립트, `docs/requests.http`, 실행·구성·API 문서를 갱신했다.

### 검증 결과

- 기존 Java 내부 통신 오류는 짧은 Windows 임시 경로를 `jdk.net.unixdomain.tmpdir`와 `java.io.tmpdir`에 지정해 해결했다. 스크립트에도 반영하고 실제 실행을 확인했다.
- `scripts/test.ps1` 실행 결과 **16개 통과, 실패 0, 건너뜀 0**. 이 중 실제 MySQL 통합 테스트는 5개다.
- 설치된 MySQL **8.0.41**을 별도 데이터 디렉터리·포트·DB·계정으로 띄워 검증했다. 기존 사용자 DB를 사용하지 않았다.
- MySQL 검증: 빈 DB 마이그레이션, 시드, 가입·로그인·갱신·프로필, 카테고리, 추출·고정, 동일 조합 재사용, 동시 AI 1회, 실패 결과 미저장·재시도, 만드는 법 캐시, link/feed 구분, 공유 HTML, 반응 변경·취소·내 글 거절, 최신/인기 커서, hidden 제외, Swagger 계약.
- 단위 검증: 서명·만료·토큰 종류·삭제 회원, 진행 작업 공유·실패 후 재시도, 호출 제한 Retry-After, 공유 HTML 이스케이프.
- `ktlintCheck`, `bootJar` 성공. `build/libs/idea-support-server-0.0.1-SNAPSHOT.jar` 생성.
- 패키지 경로·문서 로컬 링크·JSON 예시·Service의 Repository 직접 사용 여부·이전 명칭 잔존 여부 정적 확인에서 문제 없음. 주 소스 Kotlin 파일 112개.
- `docker compose config --quiet` 성공. 실제 Docker 실행 대신 설치된 MySQL로 검증했다.
- 실행 JAR를 local 프로필·127.0.0.1:18080에서 기동해 실제 HTTP도 확인했다. health ok/DB up/seedReady true, 회원가입·로그인, 추출, 생성 201·재요청 cached=true, 만드는 법 201, 공개 HTML 200, Swagger UI 200 및 앱 경로 14개를 확인했다.
- 검증 API와 격리 MySQL 프로세스는 확인 후 종료했다. 비밀값·임시 DB·검증 원본은 Git 제외 경로 `.local`에만 두었다.

### 남은 외부 확인·업데이트 대상

- 실제 Gemini 유료 호출은 하지 않았다. 사용할 계정의 `GEMINI_API_KEY`, `AI_MODEL` 설정 후 실제 응답·지연을 확인해야 한다.
- 학생 프론트 소스가 없어 실제 화면 연동은 미확인이다. API 주소·기존 로그인/갱신·DTO·오류 매핑을 학생 프로젝트에서 연결한다.
- 다중 서버 조정, 브라우저 종료 즉시 외부 AI 과금 취소, 운영 부하·콘텐츠 심사·신고는 이번 검증 범위에 포함하지 않는다.
- 코드: `src/main/kotlin/com/ideasupport`, DB: `src/main/resources/db`, 설정: `.env`·application YAML, 연동 계약: `docs/API_SPEC.md`·Swagger, 구조: `docs/BACKEND_PLAN.md`.
- 노션 원문은 수정하지 않았다. 원격 배포·기기 설치도 수행하지 않았다.

## 2026-10-27 — 프로젝트 명칭 정리와 백엔드 설계

- 요청: 기본 Kotlin/Spring Boot 템플릿의 기존 조직·서비스 명칭을 아이디어 보조 앱에 맞게 정리하고, 학생 프론트엔드 연동을 위한 API와 구성을 작성한다.
- 작업 전 `README.md`, `Convention.md`, Gradle 설정, 인증·DB·Swagger 설정과 노션 원문을 확인했다.
- 현재 폴더에는 `.git`이 없고 상위 사용자 홈의 Git 저장소만 감지된다. 이 폴더 경로에 해당하는 커밋 이력과 최근 변경 Markdown은 없다. 상위 저장소의 다른 프로젝트는 수정하지 않는다.
- 요구사항 출처: [5팀 아이디어 보조 어플](https://app.notion.com/p/3f89fa2a819d8048a0e6fca0e6645436), 마지막 수정 표시 2026-10-21. 본문 기준이며 첨부 PDF/PPTX 내부와 학생 프론트엔드 소스는 확인하지 않았다.
- 이번 범위: 프로젝트·패키지·문서·환경설정의 명칭 변경 및 구현 계획 작성. 앱 전용 API의 실제 구현 완료를 의미하지 않는다.
- 실기기 연결·설치 검증은 사용자 지시에 따라 수행하지 않는다.

### 최종 기준

- 작업 중 사용자 정정으로 **MySQL과 기존 자체 회원·JWT 인증 유지**를 확정했다. DB 전환·익명 인증 도입 계획은 폐기하고 최종 문서에서 제거했다.
- 원문과 다른 결정은 사용자 지시를 우선하며 노션 원문은 수정하지 않았다.

### 변경

- 프로젝트명·Spring application 이름·Gradle group·시작 클래스·Swagger 제목·소스/테스트 패키지·H2 SQL 클래스 참조를 아이디어 보조 서비스로 통일했다.
- 소스와 테스트를 `com.ideasupport` 경로로 이동했다.
- 환경변수 예제의 이전 사내 DB 주소와 서비스명·계정 참조를 제거했다. dev DB 연결은 공통 DB 환경변수를 사용한다.
- 컨테이너 기본명은 `local/idea-support-server`, 원격 주소는 containerImage / CONTAINER_IMAGE로 전달하도록 변경했다. 원격 업로드는 하지 않았다.
- 기존 자체 JWT 코드에서 사용하지 않던 외부 인증 SDK 주입을 제거하고, 그 SDK·설정 빈·환경변수·전용 테스트를 삭제했다. 남은 관련 변수명도 일반 토큰 이름으로 정리했다.
- 기존 UserService·Jwt의 회원가입·로그인·갱신 동작과 MySQL 기반은 유지했다.
- README·Convention 갱신, `docs/BACKEND_PLAN.md`·`docs/API_SPEC.md` 작성. 신규 업무 API는 구현 예정으로 명시했다.

### 검증

- 컴파일 전 기준 확인과 변경 후 컴파일을 시도했으나 Gradle이 작업 실행 전 `java.io.IOException: Unable to establish loopback connection`으로 중단됐다.
- IPv4 지정·Gradle JVM 옵션·설치된 다른 Java 17로 확인해도 같은 시작 오류가 발생했다. 컴파일·테스트 성공으로 보고하지 않는다.
- 실제 DB 접속, 외부 AI 유료 호출, 서버 시연·배포, 기기 설치는 수행하지 않았다.
- 최종 정적 검사: Kotlin 63개 파일의 패키지·디렉터리 일치, 텍스트 91개 파일의 기존 조직·서비스·외부 인증 참조 잔존 여부, 문서 내 로컬 링크, JSON 예시 22개 파싱을 확인했다. 발견된 문제는 없다.
- YAML 파서는 현재 환경에 없어 YAML 전체 파싱 검증은 수행하지 못했다.

### 다음 업데이트 대상

- 서버: `src/main/kotlin/com/ideasupport`, MySQL 마이그레이션: `src/main/resources/db`.
- 설정: `src/main/resources`, 계약: `docs/API_SPEC.md`, 계획: `docs/BACKEND_PLAN.md`, 결과 기록: 이 문서.
- 학생 프론트: API 기본 주소, 기존 로그인·토큰 갱신 연결, DTO·오류 매핑.
- 노션을 추후 갱신할 경우 6주차 서버·DB·인증 선택을 현재 기준에 맞춰 수정한다.
