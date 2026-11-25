# API 명세 — Idea Support

작성일: 2026-10-27. [구현 계획](BACKEND_PLAN.md)과 함께 읽는다.

화면별 연결 순서는 [학생용 API 흐름 안내](student-guide/API_USER_FLOW.md), 테이블 관계는 [온라인 MySQL ERD](schema/README.md)를 참고한다. 같은 흐름 안내의 [Word 문서](student-guide/idea-support-api-user-flow.docx)도 제공한다.

**MySQL과 기존 자체 인증을 사용한다.** 아래 회원·업무 API를 구현했다.
학생 프론트엔드 코드는 아직 확인하지 않았으므로 학생 화면과의 최종 필드 매핑은 별도 확인이 필요하다.

## 1. 공통 규칙

- 로컬 API 주소 제안: `http://localhost:8080`.
- 기존 경로 규칙을 유지해 신규 업무 API는 `/app` 아래에 둔다. 노션의 루트 경로를 그대로 호출하면 안 된다.
- JSON 요청은 `Content-Type: application/json`, 인증은 `Authorization: Bearer <accessToken>`.
- 가입·로그인·토큰 갱신·공유 HTML·health 외 업무 API에는 기존 회원 토큰을 보낸다.
- 성공 응답은 DTO를 바로 반환한다. 별도 `data` 래퍼를 추가하지 않는다.
- 업무 ID는 UUID 문자열, 회원의 외부 식별자는 기존 `uid`, 내부 FK는 `users.idx`다.
- 시각은 UTC ISO-8601, 페이지 끝의 `nextCursor`는 null, 빈 목록은 `items: []`다.
- 새 리소스 생성은 201, 기존 결과 재사용·조회·반응 변경은 200으로 제안한다. 기존 회원 API는 현재 200 유지.
- 인증 사용자·게시 작성자는 토큰으로 결정한다. 클라이언트가 보낸 userId를 신뢰하지 않는다.

### 공통 업무 오류

기존 `ServerErrorResponseDto`의 `code: number`, `name: string`, `message: string`을 재사용한다.
아래 업무 오류 번호는 기존 enum과 생성 JSON에 반영했다.
입력 검증 실패도 이 형식을 사용한다. 예상하지 못한 오류는 500 / 9997 / INTERNAL_SERVER_ERROR다.

```json
{
  "code": 1103,
  "name": "DIRECTION_TOO_NARROW",
  "message": "연관 단어가 부족합니다. 방향을 조금 넓혀 주세요."
}
```

| HTTP | code / name | 의미 |
| --- | --- | --- |
| 400 | 1100 / INVALID_INPUT | 형식·길이·단어 수·고정 단어·중복·커서 오류 |
| 401 | 기존 1 / AUTHORIZATION_NOT_FOUND, 2 / INVALID_TOKEN | 인증 누락·유효하지 않은 토큰 |
| 403 | 1101 / OWN_POST_REACTION_FORBIDDEN | 내 글 반응 불가 |
| 404 | 1102 / RESOURCE_NOT_FOUND | 카테고리·아이디어·글·공유 없음 또는 숨김 |
| 422 | 1103 / DIRECTION_TOO_NARROW | 방향성에 맞는 후보 부족 |
| 422 | 1104 / WORDS_INSUFFICIENT | 카테고리 단어 부족 |
| 422 | 1105 / CATEGORY_MATCH_INSUFFICIENT | 사용자 카테고리 매칭 단어 10개 미만 |
| 422 | 1106 / UNSAFE_CONTENT | 부적절한 입력·출력 |
| 429 | 1107 / RATE_LIMITED | 호출 제한. Retry-After 헤더 제공 |
| 502 | 1108 / AI_RESPONSE_INVALID | AI JSON·필수 필드·허용 ID 검증 실패 |
| 503 | 1109 / AI_UNAVAILABLE | AI 키·제공자 연결 문제 |
| 504 | 1110 / AI_TIMEOUT | 생성 제한 시간 초과 |

문자열 길이 제안: 카테고리명 1~50자, 방향성 0~200자. 텍스트는 trim하고 연속 공백을 정리한다.
입력 검증 실패 메시지는 프론트에서 그대로 안내할 수 있게 한국어로 작성한다.

## 2. 기존 회원 API — 유지

### POST /app/user/sign-up

현재 DTO의 필수 필드는 아래 다섯 개다. 서비스가 저장하는 필드는 id·pw·email·name이며,
birth는 현재 DTO에서 필수지만 서비스 저장에는 사용되지 않는다. 이번 작업에서 회원 계약은 변경하지 않았다.

```json
{
  "id": "student01",
  "pw": "USER_INPUT_PASSWORD",
  "email": "student@example.com",
  "name": "학생1",
  "birth": "2000-01-01"
}
```

필수 필드는 `id`, `pw`, `email`, `name`, `birth`다.
나머지 nickName·phoneNumber·providerName·termsOfService·privacyPolicy·alertPolicy·referralCodeReceived는 선택 필드다.
응답은 기존 UserCommonDto로 회원 uid·email·name·생성 시각 등을 반환한다. 회원가입은 토큰을 발급하지 않으므로 다음 로그인 API를 호출한다.

### POST /app/user/login

요청:

```json
{"id":"student01","pw":"USER_INPUT_PASSWORD"}
```

200 응답:

```json
{"accessToken":"ACCESS_TOKEN","refreshToken":"REFRESH_TOKEN"}
```

### POST /app/user/refresh-token

요청과 200 응답:

```json
{"refreshToken":"REFRESH_TOKEN"}
```

```json
{"accessToken":"NEW_ACCESS_TOKEN"}
```

프론트는 인증 실패 시 갱신 후 원요청을 최대 한 번 재시도한다. 갱신도 실패하면 로그인으로 이동한다.
갱신 시 서명·만료·issuer·refresh 용도·회원 존재를 확인한다. refresh 토큰을 업무 API에 쓰거나 access 토큰으로 갱신할 수 없다.

### GET /app/user/profile

기존 본인 프로필 조회다. 일반 USER도 자신의 프로필을 조회하도록 ADMIN 제한을 해제했다. 인증은 local을 포함해 모든 프로필에서 적용한다.

## 3. 카테고리 — 구현됨

### GET /app/categories

기본 카테고리를 먼저, 사용자 카테고리를 useCount 내림차순·id 순으로 반환한다.

```json
{
  "items": [
    {"id":"11111111-1111-4111-8111-111111111111","name":"음식 개발","kind":"default","wordCount":100,"useCount":12,"supportsRecipe":true}
  ]
}
```

### POST /app/categories/preview

요청:

```json
{"name":"캠핑 요리"}
```

200 응답 예시:

```json
{
  "name":"캠핑 요리",
  "existingCategoryId":null,
  "canCreate":true,
  "matchedWordCount":24,
  "words":[{"id":"22222222-2222-4222-8222-222222222222","text":"감자"}],
  "matchSource":"ai"
}
```

words는 화면용 일부 미리보기이며 matchedWordCount가 전체 매칭 수다.
matchSource는 `keyword | ai | mock | cache`다. 방향성 없는 추출은 `random`이다. 생성 가능 후보는 10개 이상이어야 한다. 이미 있는 카테고리면 canCreate:false와 기존 ID를 반환한다.
중복 이름이면 existingCategoryId를 채우고 해당 카테고리로 이동할 수 있다.
연관 단어가 부족하면 422 CATEGORY_MATCH_INSUFFICIENT를 반환한다.
입력 멈춤 후 호출하도록 디바운스하고 오래된 preview 응답은 프론트에서 무시한다.

### POST /app/categories

```json
{"name":"캠핑 요리"}
```

응답은 카테고리 객체와 created 플래그다.

```json
{
  "category":{"id":"11111111-1111-4111-8111-111111111111","name":"캠핑 요리","kind":"user","wordCount":24,"useCount":0,"supportsRecipe":false},
  "created":true
}
```

신규는 201, 정규화 이름이 기존에 있으면 200·created:false다.
미리보기 후보를 서버에서 다시 검증하며, 클라이언트가 임의 단어 ID를 넣어 카테고리를 만들지는 않는다.

## 4. 단어 추출 — 구현됨

### POST /app/words/draw

```json
{
  "categoryId":"11111111-1111-4111-8111-111111111111",
  "lockedWordIds":[],
  "previousWordIds":[],
  "direction":"매콤한 캠핑 요리"
}
```

categoryId는 필수, 나머지는 생략 가능하다. lockedWordIds는 서로 다른 0~2개,
previousWordIds는 직전 단어 0~3개다. 서버는 모두 해당 카테고리의 유효한 단어인지 검사한다.

```json
{
  "words":[
    {"id":"22222222-2222-4222-8222-222222222222","text":"감자","locked":false},
    {"id":"33333333-3333-4333-8333-333333333333","text":"치즈","locked":false},
    {"id":"44444444-4444-4444-8444-444444444444","text":"고추","locked":false}
  ],
  "direction":"매콤한 캠핑 요리",
  "matchSource":"keyword"
}
```

항상 서로 다른 3개를 반환한다. 고정 단어는 유지하고 나머지만 뽑는다.
방향성 후보 캐시가 없는 최초 요청은 AI 후보 선택으로 더 오래 걸릴 수 있다.
키워드·AI 매칭 결과가 부족하면 명시적인 422를 반환한다.

## 5. 아이디어 — 구현됨

### POST /app/ideas

```json
{
  "categoryId":"11111111-1111-4111-8111-111111111111",
  "wordIds":["22222222-2222-4222-8222-222222222222","33333333-3333-4333-8333-333333333333","44444444-4444-4444-8444-444444444444"],
  "direction":"매콤한 캠핑 요리"
}
```

wordIds는 서로 다른 유효한 3개다. AI 요청 전에 카테고리 소속을 검사한다.
응답:

```json
{
  "idea":{
    "id":"55555555-5555-4555-8555-555555555555",
    "categoryId":"11111111-1111-4111-8111-111111111111",
    "words":[
      {"id":"22222222-2222-4222-8222-222222222222","text":"감자"},
      {"id":"33333333-3333-4333-8333-333333333333","text":"치즈"},
      {"id":"44444444-4444-4444-8444-444444444444","text":"고추"}
    ],
    "direction":"매콤한 캠핑 요리",
    "title":"매콤한 치즈 감자 구이",
    "content":"감자에 치즈와 고추를 더해 만드는 간편한 캠핑 요리입니다.",
    "parentIdeaId":null,
    "source":"gemini",
    "hasRecipe":false,
    "supportsRecipe":true,
    "createdAt":"2026-10-27T01:00:00Z"
  },
  "cached":false
}
```

source는 `gemini | mock`, cached는 이번 요청이 저장 결과를 재사용했는지를 나타낸다.
신규 201, 재사용 200·cached:true다. 단어 순서만 바뀐 동일 조합도 재사용한다.
같은 조합 동시 요청은 진행 중 생성을 공유한다. 제한 시간 초과·검증 실패 결과는 저장하지 않는다.

### GET /app/ideas/{id}

저장된 결과를 `{ "idea": ... }`로 반환한다. 생성 호출 없이 결과 화면 복원에 사용한다.
ID를 모르면 복원할 수 없으므로 프론트는 마지막 결과 ID나 결과 스냅샷을 저장한다.
숨겨졌거나 없는 결과는 404다.

### POST /app/ideas/{id}/recombine

본문은 `{}` 또는 생략한다. 원본의 카테고리·단어·방향성으로 새로운 변형을 생성한다.
응답은 생성과 같으며 parentIdeaId가 원본 ID이고 cached:false다.
각 호출은 새 생성이며 호출 제한을 적용한다. 자동 재시도로 여러 변형이 생기지 않도록 프론트는 중복 클릭을 막는다.

## 6. 분야별 후속 가이드

### POST /app/ideas/{id}/guide

본문은 `{}` 또는 생략한다. 모든 카테고리에서 후속 기능을 제공한다. 화면은 `idea.guideLabel + " 보기"`를 표시하며 `supportsRecipe`를 버튼 표시 여부로 사용하지 않는다.

| guideType | 버튼 | 제공 내용 |
| --- | --- | --- |
| food | 만드는 법 보기 | 재료·구체적인 조리 순서 |
| software | 구현 가이드 보기 | MVP 기능·화면/데이터 흐름·기술 선택·구현 순서·테스트 |
| contest | 공모전 기획안 보기 | 문제/대상·해결안·차별점·시제품/검증·효과·발표 구성 |
| general | 실행 계획 보기 | 목표·준비·작은 첫 실험·실행 단계·성공 기준·개선 |

```json
{
  "guide": {
    "ideaId": "55555555-5555-4555-8555-555555555555",
    "kind": "contest",
    "label": "공모전 기획안",
    "requirementsLabel": "기획 준비",
    "feasible": true,
    "summary": "스마트시티 에너지 절감 아이디어의 검증과 제안서 구성",
    "requirements": [{"name":"사용자 문제 자료","detail":"에너지 낭비 상황을 확인한 관찰 기록"}],
    "steps": [{"order":1,"title":"문제와 대상 정의","description":"해결할 문제와 영향을 받는 대상을 정하고 인터뷰로 확인합니다."}],
    "source": "gemini"
  },
  "cached": false
}
```

신규 생성 201, 동일 아이디어와 유형의 저장 결과 재사용은 200/cached:true다. 종류가 맞지 않는 과거 `plan` 결과는 재사용하지 않고 요청 시 올바른 유형으로 새로 생성한다. 공유 페이지 방문은 AI를 새로 호출하지 않는다.
`feasible:false`는 이유와 빈 requirements/steps를 갖는 정상 결과다. AI 실패는 5xx이며 저장하지 않는다. AI 시간 제한은 기존 20초를 사용한다.

### 사용자 카테고리의 유형

`POST /app/categories/preview`와 `POST /app/categories`에 선택 값 `guideType`을 보낸다. 생략 시 `general`이다. 예: `{"name":"건강 관리","guideType":"general"}`.
허용 값은 위 네 가지다. 카테고리 생성 시 DB에 저장하고 이름만 보고 다른 분야로 바꾸지 않는다. 같은 이름이 이미 있으면 기존 카테고리의 유형을 유지하고 그 `guideType`/`guideLabel`을 반환한다.
기본 카테고리는 음식=food, 프로그램=software, 공모전=contest다. 기존 사용자 카테고리는 general로 이관된다.

카테고리 응답은 `guideType`, `guideLabel`을 포함한다. 아이디어 응답도 두 필드와 `hasGuide`를 포함한다. 로컬 기록에는 guide 전체를 저장하며 예전 음식 recipe 기록은 변환해서 읽는다.

### 기존 POST /app/ideas/{id}/recipe

호환 경로도 같은 분야별 가이드를 생성·재사용한다. 기존 `recipe` 래퍼와 `ingredients: [{name,amount}]` 구조를 유지하며, kind는 food/software/contest/general이다. 신규 화면은 의미가 명확한 `/guide`의 `requirements: [{name,detail}]`를 사용한다.
`supportsRecipe`와 `hasRecipe`는 기존 클라이언트를 위한 음식 여부·음식 결과 여부로 유지한다. 비음식 후속 기능을 숨기는 조건으로 사용하면 안 된다. 종전 422/1111 음식 전용 거절 규칙은 폐기됐다.

## 7. 공유·피드·반응 — 구현됨

### POST /app/shares

```json
{"ideaId":"55555555-5555-4555-8555-555555555555"}
```

```json
{
  "postId":"66666666-6666-4666-8666-666666666666",
  "scope":"link",
  "shareCode":"RANDOM_SHARE_CODE",
  "shareUrl":"http://localhost:8080/s/RANDOM_SHARE_CODE",
  "created":true
}
```

scope=link는 피드에 나타나지 않는다. 같은 회원·아이디어·scope면 기존 값·created:false를 반환한다.
공유 URL은 SERVER_PUBLIC_URL로 구성한다. localhost URL은 다른 사람 PC에서 열 수 없으므로 PC 시연용이다.
외부 공유를 실제 운영하려면 별도 서버 공개 주소와 실행 환경을 마련해야 한다.

### POST /app/posts

요청은 shares와 같다. 응답도 같지만 scope=feed다. 최초 게시 201, 중복 게시 200이다.
새 자유 입력 게시글을 작성하는 API는 아니며, 서버에 존재하는 아이디어를 게시한다.

### GET /app/posts?sort=latest&limit=20&cursor=...

- sort: latest(기본) 또는 popular.
- limit: 기본 20, 1~50. cursor: 최초 생략, 이후 응답의 nextCursor 사용.
- 필터는 scope=feed, 글과 아이디어 모두 hidden=false.
- latest는 createdAt DESC, id DESC. popular는 score DESC, createdAt DESC, id DESC.
- 인기 점수는 like-dislike. 커서는 해당 정렬 키·sort를 포함하고 서버에서 형식을 검증한다.
- 인기순은 반응에 따라 순위가 바뀔 수 있다. 프론트는 ID로 중복을 제거하고 새로고침하면 첫 페이지부터 받는다.

```json
{
  "items":[
    {
      "id":"66666666-6666-4666-8666-666666666666",
      "ideaId":"55555555-5555-4555-8555-555555555555",
      "title":"매콤한 치즈 감자 구이",
      "content":"간편한 캠핑 요리 아이디어입니다.",
      "words":["감자","치즈","고추"],
      "authorName":"학생1",
      "isMine":false,
      "myReaction":null,
      "reactions":{"like":3,"meh":1,"dislike":0},
      "createdAt":"2026-10-27T01:00:00Z"
    }
  ],
  "nextCursor":null
}
```

공개 작성자 표시는 기존 회원 name을 기준으로 제안하며 이메일·로그인 id를 노출하지 않는다.
별도 닉네임이 필요하면 기존 데이터에 저장되지 않는 nickName 입력값과 구분하여 추가 설계한다.

### PUT /app/posts/{id}/reaction

```json
{"kind":"like"}
```

kind는 `like | meh | dislike | null`. null이면 취소한다. 자기 글은 403이다.

```json
{"postId":"66666666-6666-4666-8666-666666666666","myReaction":"like","reactions":{"like":4,"meh":1,"dislike":0}}
```

취소 시 myReaction:null. 같은 반응 재요청은 수를 늘리지 않는다. 반응 변경은 기존 한 행을 갱신한다.

### GET /s/{code}

로그인 없이 읽는 HTML이다. 제목·설명·단어와 저장된 분야별 후속 가이드를 제공한다. 섹션 제목은 만드는 법·구현 가이드·공모전 기획안·실행 계획으로 구분한다. 유형이 지정되지 않은 과거 plan은 재사용하지 않고 가이드 버튼 요청 시 새로 생성한다.
방문 자체로 만드는 법 AI를 새로 호출하지 않는다. 없는 코드·숨김 글·숨김 아이디어는 404다.
서버에서 모든 텍스트를 HTML 이스케이프한다.

## 8. 상태 확인

GET /common/health는 아래 JSON을 반환한다. DB와 기본 카테고리별 단어 100개 이상 준비 여부를 확인한다.

```json
{"status":"ok","database":"up","aiMode":"mock","seedReady":true}
```

AI 확인을 위해 매 health 요청마다 유료 API를 호출하지 않는다.
DB 또는 기본 단어가 준비되지 않으면 503과 status:degraded를 반환한다. 상세 키·DB 주소·회원 정보는 상태 응답에 노출하지 않는다.

## 9. 프론트엔드 연동 순서

1. 기존 로그인과 토큰 갱신을 연결한다. 학생 화면에 로그인 동선이 없으면 먼저 시연 계정 진입 방식을 정한다.
2. API 기본 주소와 공통 JSON·Bearer 처리, 한 번만 갱신 재시도하는 HTTP 클라이언트를 둔다.
3. 카테고리 → 단어 추출·고정 → 창조·취소 → 결과·재조합을 연결한다.
4. 만드는 법, 로컬 기록 저장, 링크 복사, 피드·반응을 연결한다.
5. 로딩·빈 목록·401·422·429·AI 오류와 제한 시간 초과 화면을 확인한다.

서버 API 변경 시 이 문서와 Swagger를, 프론트에서는 기본 주소·DTO·오류 매핑을 함께 업데이트한다.


## 구현·검증 범위

- 기본 단어: 음식 149, 프로그램 152, 공모전 148. 공통 단어 총 441개.
- 현재 resources/.env는 실제 Gemini 모드로 설정되어 있다. 아이디어·재조합·만드는 법·연관 단어 매칭을 실제 API로 검증했다.
- 요청 취소로 화면 대기는 중단할 수 있다. 현재 동기 HTTP 서버에서 연결 종료 즉시 외부 생성·과금 취소는 보장하지 않는다.
- 동일 작업 공유·호출 제한은 단일 서버 메모리 기준이다. 여러 서버로 확장할 때 공유 조정 저장소가 필요하다.
- 자유 입력 안전성 검사는 기본 금칙어 규칙과 AI 응답 구조 검증이다. 완전한 콘텐츠 심사 기능은 아니다.

실제 연결·환경 파일 위치·측정값은 [Gemini 연결 안내](GEMINI_SETUP.md)를 참고한다.
