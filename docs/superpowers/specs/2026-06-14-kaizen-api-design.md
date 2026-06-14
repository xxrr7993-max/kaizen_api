# kaizen_api — Design Spec
**Data:** 2026-06-14

## Contexto

API REST para o app React Native **kaizen-app** (rastreador de hábitos diários em 4 categorias de vida). Segue o padrão arquitetural do **alimentar.api** 1:1. A app já consome `/api/v1` como prefixo base.

---

## Stack

- Spring Boot 4.1.0 / Java 21 / Gradle
- PostgreSQL local (`localhost:5432/kaizen_db`)
- JWT RS256 + refresh token rotativo (7 dias)
- HikariCP, Spring Data JPA (Hibernate DDL update)
- Lombok, Validation, HATEOAS (apenas UserModel)
- Base package: `org.rod.kaizen_api`

---

## Entidades

### UserModel (TB_USERS)
| Campo | Tipo | Restrições |
|---|---|---|
| userId | UUID | PK |
| name | String | 100 chars, not null |
| email | String | 150 chars, unique, not null |
| password | String | @JsonIgnore |
| userType | UserType (enum) | ADMIN / USER |
| userStatus | UserStatus (enum) | ACTIVE / INACTIVE |
| streakCurrent | int | default 0 |
| streakRecord | int | default 0 |
| createdAt | LocalDateTime | @CreationTimestamp |
| updatedAt | LocalDateTime | @UpdateTimestamp |

### VictoryModel (TB_VICTORIES)
| Campo | Tipo | Restrições |
|---|---|---|
| victoryId | UUID | PK |
| user | UserModel | ManyToOne lazy |
| category | VictoryCategory (enum) | FISICA / MENTAL / ESPIRITUAL / PESSOAL |
| goal | String | 200 chars, not null |
| subtasks | List\<String\> | jsonb column via @Convert |
| order | int | default 0 |
| createdAt | LocalDateTime | @CreationTimestamp |
| updatedAt | LocalDateTime | @UpdateTimestamp |

### CheckinModel (TB_CHECKINS)
| Campo | Tipo | Restrições |
|---|---|---|
| checkinId | UUID | PK |
| user | UserModel | ManyToOne lazy |
| date | LocalDate | unique constraint (user + date) |
| createdAt | LocalDateTime | @CreationTimestamp |

OneToMany → VictoryCheckinModel (cascade ALL, orphanRemoval)
OneToMany → DiscardableTaskModel (cascade ALL, orphanRemoval)

### VictoryCheckinModel (TB_VICTORY_CHECKINS)
| Campo | Tipo |
|---|---|
| vcId | UUID PK |
| checkin | CheckinModel ManyToOne lazy |
| victory | VictoryModel ManyToOne lazy |
| completed | boolean default false |

OneToMany → SubtaskCheckinModel (cascade ALL, orphanRemoval)

### SubtaskCheckinModel (TB_SUBTASK_CHECKINS)
| Campo | Tipo |
|---|---|
| scId | UUID PK |
| victoryCheckin | VictoryCheckinModel ManyToOne lazy |
| subtaskIndex | int |
| completed | boolean default false |

### DiscardableTaskModel (TB_DISCARDABLE_TASKS)
| Campo | Tipo |
|---|---|
| taskId | UUID PK |
| checkin | CheckinModel ManyToOne lazy |
| goal | String 200 chars |
| completed | boolean default false |

### RefreshTokenModel (TB_REFRESH_TOKENS)
| Campo | Tipo |
|---|---|
| tokenId | UUID PK |
| user | UserModel ManyToOne lazy |
| token | String unique |
| expiresAt | LocalDateTime |
| createdAt | LocalDateTime @CreationTimestamp |

---

## Enums

- `VictoryCategory`: FISICA, MENTAL, ESPIRITUAL, PESSOAL
- `UserType`: ADMIN, USER
- `UserStatus`: ACTIVE, INACTIVE

---

## DTOs (Java Records com @JsonView)

### Auth
- `RegisterRecordDto`: name, email, password
- `LoginRecordDto`: email, password
- `AuthResponseDto`: accessToken, refreshToken, user (UserProfileDto)
- `RefreshRecordDto`: refreshToken
- `ForgotPasswordRecordDto`: email
- `ResetPasswordRecordDto`: token, newPassword
- `ChangePasswordRecordDto`: currentPassword, newPassword

### User
- `UserProfileDto`: id, name, email, createdAt, streak, record

### Victory
- `VictoryRecordDto` (views: Post, Put, Response): id, category, goal, subtasks, order

### Checkin
- `TodayCheckinDto`: date, victories (List\<VictoryCheckinDto\>), completedCount, discardable (List\<DiscardableTaskDto\>)
- `VictoryCheckinDto`: victoryId, category, completed, subtasks (List\<SubtaskCheckinDto\>)
- `SubtaskCheckinDto`: index, completed
- `DiscardableTaskDto`: id, goal, completed
- `DiscardableRecordDto`: goal (input)

### Stats
- `StreakDto`: current, record
- `WeeklyDayDto`: date, victories (Map\<String,Boolean\>), completedCount
- `MonthlyDataDto`: month, days (Map\<String, Map\<String,Boolean\>\>)
- `HeatmapDayDto`: date, victories (Map\<String,Boolean\>)
- `HeatmapDataDto`: days (List\<HeatmapDayDto\>)
- `ProgressMonthDto`: label, fisica, mental, espiritual, pessoal, overall
- `ProgressCurrentDto`: streak, record, total, activeDays
- `ProgressCompletionDto`: fisica, mental, espiritual, pessoal (%)
- `ProgressStatsDto`: months (List\<ProgressMonthDto\>), current, completion

---

## Endpoints (prefixo `/api/v1`)

### AuthController — público
| Método | Path | Request | Response |
|---|---|---|---|
| POST | /auth/register | RegisterRecordDto | AuthResponseDto 201 |
| POST | /auth/login | LoginRecordDto | AuthResponseDto 200 |
| POST | /auth/refresh | RefreshRecordDto | AuthResponseDto 200 |
| POST | /auth/forgot-password | ForgotPasswordRecordDto | 200 |
| POST | /auth/reset-password | ResetPasswordRecordDto | 200 |
| POST | /auth/change-password | ChangePasswordRecordDto | 200 |

### UserController — autenticado
| Método | Path | Response |
|---|---|---|
| GET | /users/me | UserProfileDto 200 |
| PATCH | /users/me | UserProfileDto 200 |
| DELETE | /users/me | 204 |

### VictoryController — autenticado
| Método | Path | Request | Response |
|---|---|---|---|
| GET | /victories | — | Victory[] 200 |
| PUT | /victories | List\<VictoryGoalDto\> | Victory[] 200 |
| POST | /victories | VictoryRecordDto | Victory 201 |
| PATCH | /victories/{id} | VictoryRecordDto | Victory 200 |
| DELETE | /victories/{id} | — | 204 |
| DELETE | /victories | — | 204 (reset all) |

### CheckinController — autenticado
| Método | Path | Response |
|---|---|---|
| GET | /checkin/today | TodayCheckinDto |
| POST | /checkin/victory/{victoryId}/toggle | TodayCheckinDto |
| POST | /checkin/victory/{victoryId}/subtask/{idx}/toggle | TodayCheckinDto |
| GET | /checkin/streak | StreakDto |
| GET | /checkin/week | WeeklyDay[] |
| POST | /checkin/discardable | TodayCheckinDto |
| POST | /checkin/discardable/{id}/toggle | TodayCheckinDto |
| DELETE | /checkin/discardable/{id} | TodayCheckinDto |
| GET | /checkin/month?m=YYYY-MM | MonthlyDataDto |

### StatsController — autenticado
| Método | Path | Params | Response |
|---|---|---|---|
| GET | /stats/monthly | months=6 | ProgressStatsDto |
| GET | /stats/heatmap | — | HeatmapDataDto |

---

## Segurança

- JWT RS256: geração por `JwtUtil` com chaves RSA em `resources/private.pem` e `public.pem`
- Expiração do access token: 1 hora
- Refresh token: 7 dias, armazenado em `TB_REFRESH_TOKENS`, rotacionado a cada uso
- `JwtAuthenticationFilter` extrai Bearer do header e popula `SecurityContext`
- CORS: aceita qualquer origin (app mobile)
- Rotas públicas: `/api/v1/auth/**`
- Rotas protegidas: todo o resto

---

## Lógica de Streak

- `CheckinModel` criado no primeiro toggle de uma victory no dia
- Ao fechar o dia (quando todas as 4 victories estão completas OU qualquer toggle): recomputa streak consultando dias consecutivos até hoje
- `UserModel.streakCurrent` e `streakRecord` atualizados e retornados via `StreakDto`

---

## Estrutura de Pacotes

```
org.rod.kaizen_api/
├── controllers/
├── services/ (interfaces) + services/impl/
├── models/
├── dtos/
├── repositories/
├── configs/        # SecurityConfig, JwtUtil
├── filters/        # JwtAuthenticationFilter
├── enums/          # VictoryCategory, UserType, UserStatus
├── exceptions/     # NotFoundException, ConflictException, BusinessRuleException,
│                   # InvalidCredentialsException, GlobalExceptionHandler
└── util/           # StringListConverter (jsonb ↔ List<String>)
```
