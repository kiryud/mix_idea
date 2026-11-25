# Idea Support Server Convention (Style Guide)

기본 패키지는 `com.ideasupport`, 프로젝트명은 `idea-support-server`입니다.
앱 전용 API와 DB 설계는 [백엔드 구현 계획](docs/BACKEND_PLAN.md)을 우선합니다.
현재 문서의 회원 예제는 템플릿 규칙 설명이며 앱의 회원가입 요구사항이 아닙니다.

## Layout
```sh
  Project
   │  README.md
   │  Convention.md
   ├─ core
   │   ├─  dto
   │   └─  entity
   └─infra
   │   ├─  config
   │   └─  repository
   └─ lib
   │   ├─  annotation
   │   ├─  component
   │   ├─  dto
   │   ├─  enum
   │   ├─  exception
   │   ├─  handler
   │   ├─  interceptor
   │   ├─  object
   │   └─  util
   └─ main
       ├─  Controller
       ├─  Service
       └─  Component
```

## 기능 구현 원칙

- 기능별 `main/app/<feature>` 패키지 안에 Controller, Service와 역할별 Component를 둡니다.
- Controller는 기존 `@Api`, `@Auth`/`@Auths`, `@Valid`, Swagger로 입력·경로·인증·응답을 선언합니다.
- Service는 Controller와 Component를 연결하고 여러 Component의 실행 순서·결과를 조합합니다. Repository를 직접 호출하지 않습니다.
- Component는 조회·저장·검증·AI 생성·추출 등 개별 역할을 담당합니다. 트랜잭션은 DB 작업 Component의 공개 메서드에 둡니다.
- AI 네트워크 호출을 DB 트랜잭션 안에 넣지 않습니다. 동일 키 생성 공유와 DB UNIQUE 제약은 별도 책임입니다.
- DTO는 `core/dto/app/<feature>`, Entity는 `core/entity`, Repository는 `infra/repository`에 둡니다.
- 내부 협업 모델은 외부 JSON 계약과 구분하며 Controller에서는 `core/dto`의 DTO만 반환합니다.
- MySQL과 기존 자체 회원·JWT 인증을 유지합니다. 로컬에서도 실제 서명·회원 검증을 적용합니다.

## Functions

### A/HC/LC Pattern

There is a useful pattern to follow when naming functions:

```
prefix? + action (A) + high context (HC) + low context? (LC)
```

Take a look at how this pattern may be applied in the table below.

| Name                   | Prefix   | Action (A) | High context (HC) | Low context (LC) |
|------------------------|----------|------------|-------------------|------------------|
| `getUser`              |          | `get`      | `User`            |                  |
| `getUserMessages`      |          | `get`      | `User`            | `Messages`       |
| `shouldDisplayMessage` | `should` | `Display`  | `Message`         |                  |
| `isPaymentEnabled`     | `is`     | `Enabled`  | `Payment`         |                  |

---

## variable

- Pick **camelCase** naming convention and follow it.

```kotlin
/* Bad */
val page_count = 5
val active = true
val ShouldUpdate = true

/* Good */
val pageCount = 5
val isActive = true
val shouldUpdate = true
```

- Like a prefix, variable names can be made singular or plural depending on whether they hold a single value or multiple
  values.

```Kotlin
/* Bad */
val friends = 'Bob'
val friend = ['Bob', 'Tony', 'Tanya']

/* Good */
val friend = 'Bob'
val friends = ['Bob', 'Tony', 'Tanya']
```

## Entity

#### Naming

- @Table 어노테이션 name = snake_case
- Class Name - UpperCamelCase

```kotlin
@Entity
@Data
@NoArgsConstructor
@Table(name = "table_name")
data class TableName (
  
)
```

#### Column
```kotlin
@Entity
@Data
@NoArgsConstructor
@Table(name = "table_name")
data class TableName (
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var idx: Long? = null,

  @Column(name = "col1_name")
  var col1_name: number,
)
```

#### Request DTO (Input)

- 동사 + 엔티티 이름 + InDto

```kotlin
data class CreateUserInDto (
  
)
```

#### Response DTO (Output)

- 동사 + 엔티티 이름 + OutDto

```kotlin
data class CreateUserOutDto (
  
)
```

## Git Convention

### tag

| Tag                | Description                                 |
|--------------------|:--------------------------------------------|
| `Feat`             | 새로운 기능을 추가할 경우                              |
| `Fix`              | 버그를 고친 경우                                   |
| `Design`           | CSS 등 사용자 UI 디자인 변경                         |
| `!BREAKING CHANGE` | 커다란 API 변경의 경우                              |
| `!HOTFIX	`         | 급하게 치명적인 버그를 고쳐야하는 경우                       |
| `Style`            | 코드 포맷 변경, 세미 콜론 누락, 코드 수정이 없는 경우            |
| `Refactor`         | 프로덕션 코드 리팩토링                                |
| `Comment`          | 필요한 주석 추가 및 변경                              |
| `Docs`             | 문서를 수정한 경우                                  |
| `Test`             | 테스트 추가, 테스트 리팩토링(프로덕션 코드 변경 X)              |
| `Chore`            | 빌드 태스트 업데이트, 패키지 매니저를 설정하는 경우(프로덕션 코드 변경 X) |
| `Rename`           | 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우                |
| `Remove`           | 파일을 삭제하는 작업만 수행한 경우                         |


### branch
```sh
main -> dev -> feture/jira-issue-number(feature/PF-1)
```

### commit message
```sh
[jira-issue-number] Tag: commit message
[PF-1] feat: add user api
```
