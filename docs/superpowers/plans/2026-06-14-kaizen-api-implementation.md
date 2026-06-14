# kaizen_api Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar a REST API completa do kaizen_api para suportar o app React Native kaizen-app, seguindo o padrão arquitetural do alimentar.api.

**Architecture:** Spring Boot 4.1.0 / Java 21 / Gradle / PostgreSQL local. JWT RS256 com refresh token rotativo (7 dias). Prefixo `/api/v1` via context-path. Segue convenções do alimentar.api: records Java com @JsonView, GlobalExceptionHandler, repositórios com JpaRepository.

**Tech Stack:** Spring Boot 4.1.0, Java 21, Gradle, PostgreSQL, JJWT 0.12.6, Lombok, Spring Security, Spring Data JPA, Spring Validation

---

## Task 1: Build Config + Application Properties + RSA Keys

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.properties`
- Modify: `src/main/java/org/rod/kaizen_api/KaizenApiApplication.java`
- Create: `src/main/resources/private.pem`
- Create: `src/main/resources/public.pem`

- [ ] **Step 1: Atualizar build.gradle**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'org.rod'
version = '0.0.1-SNAPSHOT'
description = 'kaizen_api'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Configurar application.properties**

```properties
spring.application.name=kaizen_api
server.port=8080
server.servlet.context-path=/api/v1

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/kaizen_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# JWT
app.jwt.expiration=3600000
app.jwt.refresh-expiration=604800000
```

- [ ] **Step 3: Gerar chaves RSA**

```bash
cd src/main/resources
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

Expected: dois arquivos `.pem` criados em `src/main/resources/`.

- [ ] **Step 4: Atualizar KaizenApiApplication.java**

```java
package org.rod.kaizen_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KaizenApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(KaizenApiApplication.class, args);
    }
}
```

- [ ] **Step 5: Criar banco de dados**

```bash
psql -U postgres -c "CREATE DATABASE kaizen_db;"
```

Expected: `CREATE DATABASE`

- [ ] **Step 6: Compilar projeto**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 2: Enums + StringListConverter

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/enums/VictoryCategory.java`
- Create: `src/main/java/org/rod/kaizen_api/enums/UserType.java`
- Create: `src/main/java/org/rod/kaizen_api/enums/UserStatus.java`
- Create: `src/main/java/org/rod/kaizen_api/util/StringListConverter.java`

- [ ] **Step 1: Criar VictoryCategory**

```java
package org.rod.kaizen_api.enums;

public enum VictoryCategory {
    FISICA, MENTAL, ESPIRITUAL, PESSOAL
}
```

- [ ] **Step 2: Criar UserType**

```java
package org.rod.kaizen_api.enums;

public enum UserType {
    ADMIN, USER
}
```

- [ ] **Step 3: Criar UserStatus**

```java
package org.rod.kaizen_api.enums;

public enum UserStatus {
    ACTIVE, INACTIVE
}
```

- [ ] **Step 4: Criar StringListConverter**

```java
package org.rod.kaizen_api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        try {
            return (list == null || list.isEmpty()) ? "[]" : mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String json) {
        try {
            return (json == null || json.isBlank()) ? new ArrayList<>()
                    : mapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
```

- [ ] **Step 5: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 3: Domain Models (Entities)

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/models/UserModel.java`
- Create: `src/main/java/org/rod/kaizen_api/models/VictoryModel.java`
- Create: `src/main/java/org/rod/kaizen_api/models/CheckinModel.java`
- Create: `src/main/java/org/rod/kaizen_api/models/VictoryCheckinModel.java`
- Create: `src/main/java/org/rod/kaizen_api/models/SubtaskCheckinModel.java`
- Create: `src/main/java/org/rod/kaizen_api/models/DiscardableTaskModel.java`
- Create: `src/main/java/org/rod/kaizen_api/models/RefreshTokenModel.java`

- [ ] **Step 1: Criar UserModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.rod.kaizen_api.enums.UserStatus;
import org.rod.kaizen_api.enums.UserType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TB_USERS")
@Data
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType = UserType.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Column(nullable = false)
    private int streakCurrent = 0;

    @Column(nullable = false)
    private int streakRecord = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(length = 36)
    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiry;
}
```

- [ ] **Step 2: Criar VictoryModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.util.StringListConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_VICTORIES")
@Data
@DynamicUpdate
public class VictoryModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID victoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VictoryCategory category;

    @Column(nullable = false, length = 200)
    private String goal;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> subtasks = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    private int order = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Criar CheckinModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_CHECKINS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Data
public class CheckinModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID checkinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Column(nullable = false)
    private LocalDate date;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VictoryCheckinModel> victoryCheckins = new ArrayList<>();

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscardableTaskModel> discardableTasks = new ArrayList<>();
}
```

- [ ] **Step 4: Criar VictoryCheckinModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_VICTORY_CHECKINS")
@Data
public class VictoryCheckinModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private CheckinModel checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "victory_id", nullable = false)
    private VictoryModel victory;

    @Column(nullable = false)
    private boolean completed = false;

    @OneToMany(mappedBy = "victoryCheckin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SubtaskCheckinModel> subtaskCheckins = new ArrayList<>();
}
```

- [ ] **Step 5: Criar SubtaskCheckinModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "TB_SUBTASK_CHECKINS")
@Data
public class SubtaskCheckinModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vc_id", nullable = false)
    private VictoryCheckinModel victoryCheckin;

    @Column(nullable = false)
    private int subtaskIndex;

    @Column(nullable = false)
    private boolean completed = false;
}
```

- [ ] **Step 6: Criar DiscardableTaskModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "TB_DISCARDABLE_TASKS")
@Data
public class DiscardableTaskModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private CheckinModel checkin;

    @Column(nullable = false, length = 200)
    private String goal;

    @Column(nullable = false)
    private boolean completed = false;
}
```

- [ ] **Step 7: Criar RefreshTokenModel**

```java
package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TB_REFRESH_TOKENS")
@Data
public class RefreshTokenModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 8: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 4: Repositories

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/repositories/UserRepository.java`
- Create: `src/main/java/org/rod/kaizen_api/repositories/VictoryRepository.java`
- Create: `src/main/java/org/rod/kaizen_api/repositories/CheckinRepository.java`
- Create: `src/main/java/org/rod/kaizen_api/repositories/VictoryCheckinRepository.java`
- Create: `src/main/java/org/rod/kaizen_api/repositories/SubtaskCheckinRepository.java`
- Create: `src/main/java/org/rod/kaizen_api/repositories/DiscardableTaskRepository.java`
- Create: `src/main/java/org/rod/kaizen_api/repositories/RefreshTokenRepository.java`

- [ ] **Step 1: UserRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserModel, UUID> {
    boolean existsByEmail(String email);
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByPasswordResetToken(String token);
}
```

- [ ] **Step 2: VictoryRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.models.VictoryModel;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VictoryRepository extends JpaRepository<VictoryModel, UUID> {
    List<VictoryModel> findByUserOrderByOrderAsc(UserModel user);
    void deleteByUser(UserModel user);
    Optional<VictoryModel> findByVictoryIdAndUser(UUID id, UserModel user);
    boolean existsByUserAndCategory(UserModel user, VictoryCategory category);
}
```

- [ ] **Step 3: CheckinRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.CheckinModel;
import org.rod.kaizen_api.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<CheckinModel, UUID> {
    Optional<CheckinModel> findByUserAndDate(UserModel user, LocalDate date);
    List<CheckinModel> findByUserAndDateBetweenOrderByDateAsc(UserModel user, LocalDate start, LocalDate end);
    List<CheckinModel> findByUserOrderByDateDesc(UserModel user);

    @Query("SELECT COUNT(c) FROM CheckinModel c JOIN c.victoryCheckins vc WHERE c.user = :user AND vc.completed = true")
    long countActiveDays(UserModel user);
}
```

- [ ] **Step 4: VictoryCheckinRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.CheckinModel;
import org.rod.kaizen_api.models.VictoryCheckinModel;
import org.rod.kaizen_api.models.VictoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VictoryCheckinRepository extends JpaRepository<VictoryCheckinModel, UUID> {
    Optional<VictoryCheckinModel> findByCheckinAndVictory(CheckinModel checkin, VictoryModel victory);
}
```

- [ ] **Step 5: SubtaskCheckinRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.SubtaskCheckinModel;
import org.rod.kaizen_api.models.VictoryCheckinModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubtaskCheckinRepository extends JpaRepository<SubtaskCheckinModel, UUID> {
    Optional<SubtaskCheckinModel> findByVictoryCheckinAndSubtaskIndex(VictoryCheckinModel vc, int index);
}
```

- [ ] **Step 6: DiscardableTaskRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.DiscardableTaskModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscardableTaskRepository extends JpaRepository<DiscardableTaskModel, UUID> {
}
```

- [ ] **Step 7: RefreshTokenRepository**

```java
package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.RefreshTokenModel;
import org.rod.kaizen_api.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenModel, UUID> {
    Optional<RefreshTokenModel> findByToken(String token);
    void deleteByUser(UserModel user);
}
```

- [ ] **Step 8: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 5: Exceptions + Global Error Handler

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/exceptions/NotFoundException.java`
- Create: `src/main/java/org/rod/kaizen_api/exceptions/ConflictException.java`
- Create: `src/main/java/org/rod/kaizen_api/exceptions/BusinessRuleException.java`
- Create: `src/main/java/org/rod/kaizen_api/exceptions/InvalidCredentialsException.java`
- Create: `src/main/java/org/rod/kaizen_api/exceptions/UnauthorizedException.java`
- Create: `src/main/java/org/rod/kaizen_api/exceptions/ErrorResponse.java`
- Create: `src/main/java/org/rod/kaizen_api/exceptions/GlobalExceptionHandler.java`

- [ ] **Step 1: Criar exceções customizadas**

```java
// NotFoundException.java
package org.rod.kaizen_api.exceptions;
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
```

```java
// ConflictException.java
package org.rod.kaizen_api.exceptions;
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
```

```java
// BusinessRuleException.java
package org.rod.kaizen_api.exceptions;
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
```

```java
// InvalidCredentialsException.java
package org.rod.kaizen_api.exceptions;
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) { super(message); }
}
```

```java
// UnauthorizedException.java
package org.rod.kaizen_api.exceptions;
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
```

- [ ] **Step 2: Criar ErrorResponse**

```java
package org.rod.kaizen_api.exceptions;

import java.util.Map;

public record ErrorResponse(
        int code,
        String errorMessage,
        Map<String, String> errorDetails
) {}
```

- [ ] **Step 3: Criar GlobalExceptionHandler**

```java
package org.rod.kaizen_api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), null));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), null));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, ex.getMessage(), null));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal server error", null));
    }
}
```

- [ ] **Step 4: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 6: DTOs

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/RegisterRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/LoginRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/AuthResponseDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/RefreshRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/ForgotPasswordRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/ResetPasswordRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/auth/ChangePasswordRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/UserProfileDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/victory/VictoryRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/victory/VictoryGoalDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/checkin/TodayCheckinDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/checkin/VictoryCheckinDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/checkin/SubtaskCheckinDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/checkin/DiscardableTaskDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/checkin/DiscardableRecordDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/stats/StreakDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/stats/WeeklyDayDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/stats/MonthlyDataDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/stats/HeatmapDayDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/stats/HeatmapDataDto.java`
- Create: `src/main/java/org/rod/kaizen_api/dtos/stats/ProgressStatsDto.java`

- [ ] **Step 1: DTOs de Auth**

```java
// RegisterRecordDto.java
package org.rod.kaizen_api.dtos.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRecordDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 6, max = 100) String password
) {}
```

```java
// LoginRecordDto.java
package org.rod.kaizen_api.dtos.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRecordDto(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
```

```java
// RefreshRecordDto.java
package org.rod.kaizen_api.dtos.auth;
import jakarta.validation.constraints.NotBlank;

public record RefreshRecordDto(@NotBlank String refreshToken) {}
```

```java
// ForgotPasswordRecordDto.java
package org.rod.kaizen_api.dtos.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRecordDto(@NotBlank @Email String email) {}
```

```java
// ResetPasswordRecordDto.java
package org.rod.kaizen_api.dtos.auth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRecordDto(
        @NotBlank String token,
        @NotBlank @Size(min = 6) String newPassword
) {}
```

```java
// ChangePasswordRecordDto.java
package org.rod.kaizen_api.dtos.auth;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRecordDto(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 6) String newPassword
) {}
```

- [ ] **Step 2: UserProfileDto + AuthResponseDto**

```java
// UserProfileDto.java
package org.rod.kaizen_api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String name,
        String email,
        LocalDateTime createdAt,
        int streak,
        int record
) {}
```

```java
// AuthResponseDto.java
package org.rod.kaizen_api.dtos.auth;
import org.rod.kaizen_api.dtos.UserProfileDto;

public record AuthResponseDto(
        String accessToken,
        String refreshToken,
        UserProfileDto user
) {}
```

- [ ] **Step 3: DTOs de Victory**

```java
// VictoryGoalDto.java
package org.rod.kaizen_api.dtos.victory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.rod.kaizen_api.enums.VictoryCategory;

import java.util.List;

public record VictoryGoalDto(
        @NotNull VictoryCategory category,
        @NotBlank @Size(max = 200) String goal,
        List<String> subtasks
) {}
```

```java
// VictoryRecordDto.java
package org.rod.kaizen_api.dtos.victory;
import org.rod.kaizen_api.enums.VictoryCategory;

import java.util.List;
import java.util.UUID;

public record VictoryRecordDto(
        UUID id,
        VictoryCategory category,
        String goal,
        List<String> subtasks,
        int order
) {}
```

- [ ] **Step 4: DTOs de Checkin**

```java
// SubtaskCheckinDto.java
package org.rod.kaizen_api.dtos.checkin;
public record SubtaskCheckinDto(int index, boolean completed) {}
```

```java
// VictoryCheckinDto.java
package org.rod.kaizen_api.dtos.checkin;
import org.rod.kaizen_api.enums.VictoryCategory;
import java.util.List;
import java.util.UUID;

public record VictoryCheckinDto(
        UUID victoryId,
        VictoryCategory category,
        boolean completed,
        List<SubtaskCheckinDto> subtasks
) {}
```

```java
// DiscardableTaskDto.java
package org.rod.kaizen_api.dtos.checkin;
import java.util.UUID;
public record DiscardableTaskDto(UUID id, String goal, boolean completed) {}
```

```java
// DiscardableRecordDto.java
package org.rod.kaizen_api.dtos.checkin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record DiscardableRecordDto(@NotBlank @Size(max = 200) String goal) {}
```

```java
// TodayCheckinDto.java
package org.rod.kaizen_api.dtos.checkin;
import java.time.LocalDate;
import java.util.List;

public record TodayCheckinDto(
        LocalDate date,
        List<VictoryCheckinDto> victories,
        int completedCount,
        List<DiscardableTaskDto> discardable
) {}
```

- [ ] **Step 5: DTOs de Stats**

```java
// StreakDto.java
package org.rod.kaizen_api.dtos.stats;
public record StreakDto(int current, int record) {}
```

```java
// WeeklyDayDto.java
package org.rod.kaizen_api.dtos.stats;
import java.util.Map;
public record WeeklyDayDto(String date, Map<String, Boolean> victories, int completedCount) {}
```

```java
// MonthlyDataDto.java
package org.rod.kaizen_api.dtos.stats;
import java.util.Map;
public record MonthlyDataDto(String month, Map<String, Map<String, Boolean>> days) {}
```

```java
// HeatmapDayDto.java
package org.rod.kaizen_api.dtos.stats;
import java.util.Map;
public record HeatmapDayDto(String date, Map<String, Boolean> victories) {}
```

```java
// HeatmapDataDto.java
package org.rod.kaizen_api.dtos.stats;
import java.util.List;
public record HeatmapDataDto(List<HeatmapDayDto> days) {}
```

```java
// ProgressStatsDto.java
package org.rod.kaizen_api.dtos.stats;
import java.util.List;
import java.util.Map;

public record ProgressStatsDto(
        List<ProgressMonthDto> months,
        ProgressCurrentDto current,
        Map<String, Integer> completion
) {
    public record ProgressMonthDto(
            String label,
            int fisica,
            int mental,
            int espiritual,
            int pessoal,
            int overall
    ) {}

    public record ProgressCurrentDto(
            int streak,
            int record,
            long total,
            long activeDays
    ) {}
}
```

- [ ] **Step 6: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 7: Security — JWT + SecurityConfig + Filter

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/configs/JwtUtil.java`
- Create: `src/main/java/org/rod/kaizen_api/filters/JwtAuthenticationFilter.java`
- Create: `src/main/java/org/rod/kaizen_api/configs/SecurityConfig.java`

- [ ] **Step 1: Criar JwtUtil**

```java
package org.rod.kaizen_api.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    public JwtUtil() throws Exception {
        this.privateKey = loadPrivateKey();
        this.publicKey = loadPublicKey();
    }

    private PrivateKey loadPrivateKey() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("private.pem");
        String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("public.pem");
        String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    public String generateToken(String email, UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 2: Criar JwtAuthenticationFilter**

```java
package org.rod.kaizen_api.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rod.kaizen_api.configs.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                var userId = jwtUtil.getUserIdFromToken(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Criar SecurityConfig**

```java
package org.rod.kaizen_api.configs;

import org.rod.kaizen_api.filters.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 4: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

---

## Task 8: Auth Service + Controller

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/services/AuthService.java`
- Create: `src/main/java/org/rod/kaizen_api/services/impl/AuthServiceImpl.java`
- Create: `src/main/java/org/rod/kaizen_api/controllers/AuthController.java`
- Create: `src/test/java/org/rod/kaizen_api/services/AuthServiceImplTest.java`

- [ ] **Step 1: Criar interface AuthService**

```java
package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.auth.*;

public interface AuthService {
    AuthResponseDto register(RegisterRecordDto dto);
    AuthResponseDto login(LoginRecordDto dto);
    AuthResponseDto refresh(RefreshRecordDto dto);
    void forgotPassword(ForgotPasswordRecordDto dto);
    void resetPassword(ResetPasswordRecordDto dto);
    void changePassword(ChangePasswordRecordDto dto, String userId);
}
```

- [ ] **Step 2: Criar AuthServiceImpl**

```java
package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.configs.JwtUtil;
import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.dtos.auth.*;
import org.rod.kaizen_api.exceptions.*;
import org.rod.kaizen_api.models.RefreshTokenModel;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.repositories.RefreshTokenRepository;
import org.rod.kaizen_api.repositories.UserRepository;
import org.rod.kaizen_api.services.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRecordDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ConflictException("Email already registered");
        }
        UserModel user = new UserModel();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponseDto login(LoginRecordDto dto) {
        UserModel user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto refresh(RefreshRecordDto dto) {
        RefreshTokenModel token = refreshTokenRepository.findByToken(dto.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token expired");
        }
        refreshTokenRepository.delete(token);
        return buildAuthResponse(token.getUser());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRecordDto dto) {
        userRepository.findByEmail(dto.email()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            // In production: send email with resetToken
        });
        // Always return 200 to avoid user enumeration
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRecordDto dto) {
        UserModel user = userRepository.findByPasswordResetToken(dto.token())
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired reset token"));
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Reset token expired");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRecordDto dto, String userId) {
        UserModel user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);
    }

    private AuthResponseDto buildAuthResponse(UserModel user) {
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getUserId());
        String refreshToken = createRefreshToken(user);
        UserProfileDto profile = new UserProfileDto(
                user.getUserId(), user.getName(), user.getEmail(),
                user.getCreatedAt(), user.getStreakCurrent(), user.getStreakRecord()
        );
        return new AuthResponseDto(accessToken, refreshToken, profile);
    }

    private String createRefreshToken(UserModel user) {
        RefreshTokenModel token = new RefreshTokenModel();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        return refreshTokenRepository.save(token).getToken();
    }
}
```

- [ ] **Step 3: Criar AuthController**

```java
package org.rod.kaizen_api.controllers;

import jakarta.validation.Valid;
import org.rod.kaizen_api.dtos.auth.*;
import org.rod.kaizen_api.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRecordDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRecordDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshRecordDto dto) {
        return ResponseEntity.ok(authService.refresh(dto));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRecordDto dto) {
        authService.forgotPassword(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRecordDto dto) {
        authService.resetPassword(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRecordDto dto,
                                               @AuthenticationPrincipal String userId) {
        authService.changePassword(dto, userId);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 4: Escrever teste unitário**

```java
// src/test/java/org/rod/kaizen_api/services/AuthServiceImplTest.java
package org.rod.kaizen_api.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rod.kaizen_api.configs.JwtUtil;
import org.rod.kaizen_api.dtos.auth.LoginRecordDto;
import org.rod.kaizen_api.dtos.auth.RegisterRecordDto;
import org.rod.kaizen_api.exceptions.ConflictException;
import org.rod.kaizen_api.exceptions.InvalidCredentialsException;
import org.rod.kaizen_api.models.RefreshTokenModel;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.repositories.RefreshTokenRepository;
import org.rod.kaizen_api.repositories.UserRepository;
import org.rod.kaizen_api.services.impl.AuthServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtUtil jwtUtil;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthServiceImpl authService;

    @Test
    void register_newEmail_returnsAuthResponse() {
        var dto = new RegisterRecordDto("Test", "test@test.com", "password");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        var user = new UserModel();
        user.setUserId(UUID.randomUUID());
        user.setName("Test");
        user.setEmail("test@test.com");
        user.setCreatedAt(LocalDateTime.now());
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtil.generateToken(any(), any())).thenReturn("token");
        var rt = new RefreshTokenModel();
        rt.setToken("refresh");
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.save(any())).thenReturn(rt);

        var result = authService.register(dto);

        assertNotNull(result);
        assertEquals("token", result.accessToken());
        assertEquals("refresh", result.refreshToken());
    }

    @Test
    void register_existingEmail_throwsConflict() {
        var dto = new RegisterRecordDto("Test", "existing@test.com", "pass");
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);
        assertThrows(ConflictException.class, () -> authService.register(dto));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        var dto = new LoginRecordDto("test@test.com", "wrong");
        var user = new UserModel();
        user.setPassword("hashed");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> authService.login(dto));
    }
}
```

- [ ] **Step 5: Rodar testes**

```bash
./gradlew test --tests "org.rod.kaizen_api.services.AuthServiceImplTest"
```

Expected: `3 tests PASSED`

- [ ] **Step 6: Compilar e subir app**

```bash
./gradlew bootRun
```

Expected: `Started KaizenApiApplication`

- [ ] **Step 7: Testar registro via curl**

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test@kaizen.com","password":"senha123"}' | python3 -m json.tool
```

Expected: JSON com `accessToken`, `refreshToken`, `user.id`

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: auth service and controller (register, login, refresh, password)"
```

---

## Task 9: User Service + Controller

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/services/UserService.java`
- Create: `src/main/java/org/rod/kaizen_api/services/impl/UserServiceImpl.java`
- Create: `src/main/java/org/rod/kaizen_api/controllers/UserController.java`

- [ ] **Step 1: Criar interface UserService**

```java
package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.dtos.auth.RegisterRecordDto;
import org.rod.kaizen_api.models.UserModel;

import java.util.UUID;

public interface UserService {
    UserProfileDto getProfile(String userId);
    UserProfileDto updateProfile(String userId, String name, String email);
    void deleteAccount(String userId);
    UserModel findById(UUID id);
}
```

- [ ] **Step 2: Criar UserServiceImpl**

```java
package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.exceptions.ConflictException;
import org.rod.kaizen_api.exceptions.NotFoundException;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.repositories.RefreshTokenRepository;
import org.rod.kaizen_api.repositories.UserRepository;
import org.rod.kaizen_api.repositories.VictoryRepository;
import org.rod.kaizen_api.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VictoryRepository victoryRepository;

    public UserServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           VictoryRepository victoryRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.victoryRepository = victoryRepository;
    }

    @Override
    public UserProfileDto getProfile(String userId) {
        UserModel user = findById(UUID.fromString(userId));
        return toProfileDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String userId, String name, String email) {
        UserModel user = findById(UUID.fromString(userId));
        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) {
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(email);
        }
        return toProfileDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteAccount(String userId) {
        UserModel user = findById(UUID.fromString(userId));
        refreshTokenRepository.deleteByUser(user);
        victoryRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    @Override
    public UserModel findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UserProfileDto toProfileDto(UserModel user) {
        return new UserProfileDto(
                user.getUserId(), user.getName(), user.getEmail(),
                user.getCreatedAt(), user.getStreakCurrent(), user.getStreakRecord()
        );
    }
}
```

- [ ] **Step 3: Criar UserController**

```java
package org.rod.kaizen_api.controllers;

import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(@AuthenticationPrincipal String userId,
                                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.updateProfile(userId, body.get("name"), body.get("email")));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Compilar e testar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: user service and controller (me endpoint)"
```

---

## Task 10: Victory Service + Controller

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/services/VictoryService.java`
- Create: `src/main/java/org/rod/kaizen_api/services/impl/VictoryServiceImpl.java`
- Create: `src/main/java/org/rod/kaizen_api/controllers/VictoryController.java`

- [ ] **Step 1: Criar interface VictoryService**

```java
package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.victory.VictoryGoalDto;
import org.rod.kaizen_api.dtos.victory.VictoryRecordDto;
import org.rod.kaizen_api.models.UserModel;

import java.util.List;
import java.util.UUID;

public interface VictoryService {
    List<VictoryRecordDto> findAll(UserModel user);
    List<VictoryRecordDto> saveAll(UserModel user, List<VictoryGoalDto> goals);
    VictoryRecordDto create(UserModel user, VictoryGoalDto goal);
    VictoryRecordDto update(UserModel user, UUID id, VictoryGoalDto goal);
    void delete(UserModel user, UUID id);
    void deleteAll(UserModel user);
}
```

- [ ] **Step 2: Criar VictoryServiceImpl**

```java
package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.victory.VictoryGoalDto;
import org.rod.kaizen_api.dtos.victory.VictoryRecordDto;
import org.rod.kaizen_api.exceptions.NotFoundException;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.models.VictoryModel;
import org.rod.kaizen_api.repositories.VictoryRepository;
import org.rod.kaizen_api.services.VictoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VictoryServiceImpl implements VictoryService {

    private final VictoryRepository victoryRepository;

    public VictoryServiceImpl(VictoryRepository victoryRepository) {
        this.victoryRepository = victoryRepository;
    }

    @Override
    public List<VictoryRecordDto> findAll(UserModel user) {
        return victoryRepository.findByUserOrderByOrderAsc(user)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<VictoryRecordDto> saveAll(UserModel user, List<VictoryGoalDto> goals) {
        victoryRepository.deleteByUser(user);
        List<VictoryModel> saved = new ArrayList<>();
        for (int i = 0; i < goals.size(); i++) {
            VictoryGoalDto g = goals.get(i);
            VictoryModel v = new VictoryModel();
            v.setUser(user);
            v.setCategory(g.category());
            v.setGoal(g.goal());
            v.setSubtasks(g.subtasks() != null ? g.subtasks() : new ArrayList<>());
            v.setOrder(i);
            saved.add(victoryRepository.save(v));
        }
        return saved.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public VictoryRecordDto create(UserModel user, VictoryGoalDto goal) {
        VictoryModel v = new VictoryModel();
        v.setUser(user);
        v.setCategory(goal.category());
        v.setGoal(goal.goal());
        v.setSubtasks(goal.subtasks() != null ? goal.subtasks() : new ArrayList<>());
        int currentMax = victoryRepository.findByUserOrderByOrderAsc(user).size();
        v.setOrder(currentMax);
        return toDto(victoryRepository.save(v));
    }

    @Override
    @Transactional
    public VictoryRecordDto update(UserModel user, UUID id, VictoryGoalDto goal) {
        VictoryModel v = victoryRepository.findByVictoryIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        v.setGoal(goal.goal());
        if (goal.subtasks() != null) v.setSubtasks(goal.subtasks());
        return toDto(victoryRepository.save(v));
    }

    @Override
    @Transactional
    public void delete(UserModel user, UUID id) {
        VictoryModel v = victoryRepository.findByVictoryIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        victoryRepository.delete(v);
    }

    @Override
    @Transactional
    public void deleteAll(UserModel user) {
        victoryRepository.deleteByUser(user);
    }

    private VictoryRecordDto toDto(VictoryModel v) {
        return new VictoryRecordDto(
                v.getVictoryId(), v.getCategory(), v.getGoal(), v.getSubtasks(), v.getOrder()
        );
    }
}
```

- [ ] **Step 3: Criar VictoryController**

```java
package org.rod.kaizen_api.controllers;

import jakarta.validation.Valid;
import org.rod.kaizen_api.dtos.victory.VictoryGoalDto;
import org.rod.kaizen_api.dtos.victory.VictoryRecordDto;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.services.UserService;
import org.rod.kaizen_api.services.VictoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/victories")
public class VictoryController {

    private final VictoryService victoryService;
    private final UserService userService;

    public VictoryController(VictoryService victoryService, UserService userService) {
        this.victoryService = victoryService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<VictoryRecordDto>> findAll(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(victoryService.findAll(user));
    }

    @PutMapping
    public ResponseEntity<List<VictoryRecordDto>> saveAll(@AuthenticationPrincipal String userId,
                                                          @Valid @RequestBody List<VictoryGoalDto> goals) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(victoryService.saveAll(user, goals));
    }

    @PostMapping
    public ResponseEntity<VictoryRecordDto> create(@AuthenticationPrincipal String userId,
                                                   @Valid @RequestBody VictoryGoalDto goal) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(victoryService.create(user, goal));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VictoryRecordDto> update(@AuthenticationPrincipal String userId,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody VictoryGoalDto goal) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(victoryService.update(user, id, goal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        UserModel user = userService.findById(UUID.fromString(userId));
        victoryService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        victoryService.deleteAll(user);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: victory service and controller"
```

---

## Task 11: Checkin Service + Controller

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/services/CheckinService.java`
- Create: `src/main/java/org/rod/kaizen_api/services/impl/CheckinServiceImpl.java`
- Create: `src/main/java/org/rod/kaizen_api/controllers/CheckinController.java`

- [ ] **Step 1: Criar interface CheckinService**

```java
package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.checkin.*;
import org.rod.kaizen_api.dtos.stats.StreakDto;
import org.rod.kaizen_api.dtos.stats.WeeklyDayDto;
import org.rod.kaizen_api.dtos.stats.MonthlyDataDto;
import org.rod.kaizen_api.models.UserModel;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CheckinService {
    TodayCheckinDto getToday(UserModel user);
    TodayCheckinDto toggleVictory(UserModel user, UUID victoryId);
    TodayCheckinDto toggleSubtask(UserModel user, UUID victoryId, int subtaskIndex);
    StreakDto getStreak(UserModel user);
    List<WeeklyDayDto> getWeekly(UserModel user);
    MonthlyDataDto getMonthly(UserModel user, String month);
    TodayCheckinDto addDiscardable(UserModel user, DiscardableRecordDto dto);
    TodayCheckinDto toggleDiscardable(UserModel user, UUID taskId);
    TodayCheckinDto deleteDiscardable(UserModel user, UUID taskId);
}
```

- [ ] **Step 2: Criar CheckinServiceImpl**

```java
package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.checkin.*;
import org.rod.kaizen_api.dtos.stats.MonthlyDataDto;
import org.rod.kaizen_api.dtos.stats.StreakDto;
import org.rod.kaizen_api.dtos.stats.WeeklyDayDto;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.exceptions.NotFoundException;
import org.rod.kaizen_api.models.*;
import org.rod.kaizen_api.repositories.*;
import org.rod.kaizen_api.services.CheckinService;
import org.rod.kaizen_api.services.VictoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final VictoryCheckinRepository victoryCheckinRepository;
    private final SubtaskCheckinRepository subtaskCheckinRepository;
    private final DiscardableTaskRepository discardableTaskRepository;
    private final VictoryRepository victoryRepository;
    private final UserRepository userRepository;

    public CheckinServiceImpl(CheckinRepository checkinRepository,
                              VictoryCheckinRepository victoryCheckinRepository,
                              SubtaskCheckinRepository subtaskCheckinRepository,
                              DiscardableTaskRepository discardableTaskRepository,
                              VictoryRepository victoryRepository,
                              UserRepository userRepository) {
        this.checkinRepository = checkinRepository;
        this.victoryCheckinRepository = victoryCheckinRepository;
        this.subtaskCheckinRepository = subtaskCheckinRepository;
        this.discardableTaskRepository = discardableTaskRepository;
        this.victoryRepository = victoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TodayCheckinDto getToday(UserModel user) {
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto toggleVictory(UserModel user, UUID victoryId) {
        VictoryModel victory = victoryRepository.findByVictoryIdAndUser(victoryId, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        ensureVictoryCheckins(user, checkin);
        VictoryCheckinModel vc = victoryCheckinRepository.findByCheckinAndVictory(checkin, victory)
                .orElseThrow(() -> new NotFoundException("Victory checkin not found"));
        vc.setCompleted(!vc.isCompleted());
        victoryCheckinRepository.save(vc);
        recomputeStreak(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto toggleSubtask(UserModel user, UUID victoryId, int subtaskIndex) {
        VictoryModel victory = victoryRepository.findByVictoryIdAndUser(victoryId, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        ensureVictoryCheckins(user, checkin);
        VictoryCheckinModel vc = victoryCheckinRepository.findByCheckinAndVictory(checkin, victory)
                .orElseThrow(() -> new NotFoundException("Victory checkin not found"));
        SubtaskCheckinModel sc = subtaskCheckinRepository
                .findByVictoryCheckinAndSubtaskIndex(vc, subtaskIndex)
                .orElseThrow(() -> new NotFoundException("Subtask not found"));
        sc.setCompleted(!sc.isCompleted());
        subtaskCheckinRepository.save(sc);
        return toTodayDto(checkin, user);
    }

    @Override
    public StreakDto getStreak(UserModel user) {
        return new StreakDto(user.getStreakCurrent(), user.getStreakRecord());
    }

    @Override
    public List<WeeklyDayDto> getWeekly(UserModel user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<CheckinModel> checkins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, weekStart, today);
        Map<LocalDate, CheckinModel> byDate = checkins.stream()
                .collect(Collectors.toMap(CheckinModel::getDate, c -> c));

        List<WeeklyDayDto> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            CheckinModel c = byDate.get(date);
            result.add(buildWeeklyDay(date, c));
        }
        return result;
    }

    @Override
    public MonthlyDataDto getMonthly(UserModel user, String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<CheckinModel> checkins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, start, end);

        Map<String, Map<String, Boolean>> days = new LinkedHashMap<>();
        for (CheckinModel c : checkins) {
            Map<String, Boolean> cats = buildCategoryMap(c);
            days.put(c.getDate().toString(), cats);
        }
        return new MonthlyDataDto(month, days);
    }

    @Override
    @Transactional
    public TodayCheckinDto addDiscardable(UserModel user, DiscardableRecordDto dto) {
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        DiscardableTaskModel task = new DiscardableTaskModel();
        task.setCheckin(checkin);
        task.setGoal(dto.goal());
        discardableTaskRepository.save(task);
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto toggleDiscardable(UserModel user, UUID taskId) {
        DiscardableTaskModel task = discardableTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Discardable task not found"));
        task.setCompleted(!task.isCompleted());
        discardableTaskRepository.save(task);
        CheckinModel checkin = task.getCheckin();
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto deleteDiscardable(UserModel user, UUID taskId) {
        DiscardableTaskModel task = discardableTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Discardable task not found"));
        CheckinModel checkin = task.getCheckin();
        discardableTaskRepository.delete(task);
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CheckinModel getOrCreateCheckin(UserModel user, LocalDate date) {
        return checkinRepository.findByUserAndDate(user, date).orElseGet(() -> {
            CheckinModel c = new CheckinModel();
            c.setUser(user);
            c.setDate(date);
            return checkinRepository.save(c);
        });
    }

    private void ensureVictoryCheckins(UserModel user, CheckinModel checkin) {
        List<VictoryModel> victories = victoryRepository.findByUserOrderByOrderAsc(user);
        // Reload checkin to get current victoryCheckins
        Set<UUID> existing = victoryCheckinRepository
                .findAll().stream()
                .filter(vc -> vc.getCheckin().getCheckinId().equals(checkin.getCheckinId()))
                .map(vc -> vc.getVictory().getVictoryId())
                .collect(Collectors.toSet());

        for (VictoryModel v : victories) {
            if (!existing.contains(v.getVictoryId())) {
                VictoryCheckinModel vc = new VictoryCheckinModel();
                vc.setCheckin(checkin);
                vc.setVictory(v);
                vc = victoryCheckinRepository.save(vc);
                for (int i = 0; i < v.getSubtasks().size(); i++) {
                    SubtaskCheckinModel sc = new SubtaskCheckinModel();
                    sc.setVictoryCheckin(vc);
                    sc.setSubtaskIndex(i);
                    subtaskCheckinRepository.save(sc);
                }
            }
        }
    }

    private void recomputeStreak(UserModel user, CheckinModel todayCheckin) {
        List<CheckinModel> allCheckins = checkinRepository.findByUserOrderByDateDesc(user);
        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (CheckinModel c : allCheckins) {
            boolean hasCompleted = c.getVictoryCheckins().stream().anyMatch(VictoryCheckinModel::isCompleted);
            if (c.getDate().equals(expected) && hasCompleted) {
                streak++;
                expected = expected.minusDays(1);
            } else if (c.getDate().isBefore(expected)) {
                break;
            }
        }
        user.setStreakCurrent(streak);
        if (streak > user.getStreakRecord()) user.setStreakRecord(streak);
        userRepository.save(user);
    }

    private TodayCheckinDto toTodayDto(CheckinModel checkin, UserModel user) {
        List<VictoryModel> victories = victoryRepository.findByUserOrderByOrderAsc(user);
        List<VictoryCheckinDto> vcDtos = new ArrayList<>();
        int completedCount = 0;

        for (VictoryModel v : victories) {
            Optional<VictoryCheckinModel> vcOpt = victoryCheckinRepository
                    .findByCheckinAndVictory(checkin, v);
            boolean completed = vcOpt.map(VictoryCheckinModel::isCompleted).orElse(false);
            if (completed) completedCount++;

            List<SubtaskCheckinDto> subtaskDtos = new ArrayList<>();
            if (vcOpt.isPresent()) {
                for (int i = 0; i < v.getSubtasks().size(); i++) {
                    final int idx = i;
                    boolean sc = subtaskCheckinRepository
                            .findByVictoryCheckinAndSubtaskIndex(vcOpt.get(), idx)
                            .map(SubtaskCheckinModel::isCompleted).orElse(false);
                    subtaskDtos.add(new SubtaskCheckinDto(idx, sc));
                }
            }
            vcDtos.add(new VictoryCheckinDto(v.getVictoryId(), v.getCategory(), completed, subtaskDtos));
        }

        List<DiscardableTaskDto> discardable = discardableTaskRepository.findAll().stream()
                .filter(t -> t.getCheckin().getCheckinId().equals(checkin.getCheckinId()))
                .map(t -> new DiscardableTaskDto(t.getTaskId(), t.getGoal(), t.isCompleted()))
                .toList();

        return new TodayCheckinDto(checkin.getDate(), vcDtos, completedCount, discardable);
    }

    private WeeklyDayDto buildWeeklyDay(LocalDate date, CheckinModel checkin) {
        Map<String, Boolean> victories = new LinkedHashMap<>();
        for (VictoryCategory cat : VictoryCategory.values()) {
            victories.put(cat.name().toLowerCase(), false);
        }
        int completedCount = 0;
        if (checkin != null) {
            for (VictoryCheckinModel vc : checkin.getVictoryCheckins()) {
                if (vc.isCompleted()) {
                    String cat = vc.getVictory().getCategory().name().toLowerCase();
                    victories.put(cat, true);
                    completedCount++;
                }
            }
        }
        return new WeeklyDayDto(date.toString(), victories, completedCount);
    }

    private Map<String, Boolean> buildCategoryMap(CheckinModel checkin) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (VictoryCategory cat : VictoryCategory.values()) {
            map.put(cat.name().toLowerCase(), false);
        }
        for (VictoryCheckinModel vc : checkin.getVictoryCheckins()) {
            if (vc.isCompleted()) {
                map.put(vc.getVictory().getCategory().name().toLowerCase(), true);
            }
        }
        return map;
    }
}
```

- [ ] **Step 3: Criar CheckinController**

```java
package org.rod.kaizen_api.controllers;

import jakarta.validation.Valid;
import org.rod.kaizen_api.dtos.checkin.*;
import org.rod.kaizen_api.dtos.stats.MonthlyDataDto;
import org.rod.kaizen_api.dtos.stats.StreakDto;
import org.rod.kaizen_api.dtos.stats.WeeklyDayDto;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.services.CheckinService;
import org.rod.kaizen_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/checkin")
public class CheckinController {

    private final CheckinService checkinService;
    private final UserService userService;

    public CheckinController(CheckinService checkinService, UserService userService) {
        this.checkinService = checkinService;
        this.userService = userService;
    }

    @GetMapping("/today")
    public ResponseEntity<TodayCheckinDto> getToday(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getToday(user));
    }

    @PostMapping("/victory/{victoryId}/toggle")
    public ResponseEntity<TodayCheckinDto> toggleVictory(@AuthenticationPrincipal String userId,
                                                         @PathVariable UUID victoryId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.toggleVictory(user, victoryId));
    }

    @PostMapping("/victory/{victoryId}/subtask/{subtaskIndex}/toggle")
    public ResponseEntity<TodayCheckinDto> toggleSubtask(@AuthenticationPrincipal String userId,
                                                         @PathVariable UUID victoryId,
                                                         @PathVariable int subtaskIndex) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.toggleSubtask(user, victoryId, subtaskIndex));
    }

    @GetMapping("/streak")
    public ResponseEntity<StreakDto> getStreak(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getStreak(user));
    }

    @GetMapping("/week")
    public ResponseEntity<List<WeeklyDayDto>> getWeekly(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getWeekly(user));
    }

    @GetMapping("/month")
    public ResponseEntity<MonthlyDataDto> getMonthly(@AuthenticationPrincipal String userId,
                                                     @RequestParam String m) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.getMonthly(user, m));
    }

    @PostMapping("/discardable")
    public ResponseEntity<TodayCheckinDto> addDiscardable(@AuthenticationPrincipal String userId,
                                                          @Valid @RequestBody DiscardableRecordDto dto) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.addDiscardable(user, dto));
    }

    @PostMapping("/discardable/{id}/toggle")
    public ResponseEntity<TodayCheckinDto> toggleDiscardable(@AuthenticationPrincipal String userId,
                                                             @PathVariable UUID id) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.toggleDiscardable(user, id));
    }

    @DeleteMapping("/discardable/{id}")
    public ResponseEntity<TodayCheckinDto> deleteDiscardable(@AuthenticationPrincipal String userId,
                                                             @PathVariable UUID id) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(checkinService.deleteDiscardable(user, id));
    }
}
```

- [ ] **Step 4: Compilar**

```bash
./gradlew compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: checkin service and controller"
```

---

## Task 12: Stats Service + Controller

**Files:**
- Create: `src/main/java/org/rod/kaizen_api/services/StatsService.java`
- Create: `src/main/java/org/rod/kaizen_api/services/impl/StatsServiceImpl.java`
- Create: `src/main/java/org/rod/kaizen_api/controllers/StatsController.java`

- [ ] **Step 1: Criar interface StatsService**

```java
package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.stats.HeatmapDataDto;
import org.rod.kaizen_api.dtos.stats.ProgressStatsDto;
import org.rod.kaizen_api.models.UserModel;

public interface StatsService {
    ProgressStatsDto getProgress(UserModel user, int months);
    HeatmapDataDto getHeatmap(UserModel user);
}
```

- [ ] **Step 2: Criar StatsServiceImpl**

```java
package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.stats.*;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.models.*;
import org.rod.kaizen_api.repositories.CheckinRepository;
import org.rod.kaizen_api.services.StatsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class StatsServiceImpl implements StatsService {

    private static final Map<Integer, String> PT_MONTHS = Map.ofEntries(
            Map.entry(1, "Jan"), Map.entry(2, "Fev"), Map.entry(3, "Mar"),
            Map.entry(4, "Abr"), Map.entry(5, "Mai"), Map.entry(6, "Jun"),
            Map.entry(7, "Jul"), Map.entry(8, "Ago"), Map.entry(9, "Set"),
            Map.entry(10, "Out"), Map.entry(11, "Nov"), Map.entry(12, "Dez")
    );

    private final CheckinRepository checkinRepository;

    public StatsServiceImpl(CheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }

    @Override
    public ProgressStatsDto getProgress(UserModel user, int months) {
        List<ProgressStatsDto.ProgressMonthDto> monthDtos = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            List<CheckinModel> checkins = checkinRepository
                    .findByUserAndDateBetweenOrderByDateAsc(user, ym.atDay(1), ym.atEndOfMonth());

            int totalDays = ym.lengthOfMonth();
            int fisica = 0, mental = 0, espiritual = 0, pessoal = 0;
            for (CheckinModel c : checkins) {
                for (VictoryCheckinModel vc : c.getVictoryCheckins()) {
                    if (!vc.isCompleted()) continue;
                    switch (vc.getVictory().getCategory()) {
                        case FISICA -> fisica++;
                        case MENTAL -> mental++;
                        case ESPIRITUAL -> espiritual++;
                        case PESSOAL -> pessoal++;
                    }
                }
            }
            int fp = pct(fisica, totalDays), mp = pct(mental, totalDays),
                ep = pct(espiritual, totalDays), pp = pct(pessoal, totalDays);
            int overall = (fp + mp + ep + pp) / 4;
            monthDtos.add(new ProgressStatsDto.ProgressMonthDto(
                    PT_MONTHS.get(ym.getMonthValue()), fp, mp, ep, pp, overall
            ));
        }

        // Current month completion %
        YearMonth thisMonth = YearMonth.now();
        List<CheckinModel> thisCheckins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, thisMonth.atDay(1), LocalDate.now());
        int days = LocalDate.now().getDayOfMonth();
        int cf = 0, cm = 0, ce = 0, cp = 0;
        for (CheckinModel c : thisCheckins) {
            for (VictoryCheckinModel vc : c.getVictoryCheckins()) {
                if (!vc.isCompleted()) continue;
                switch (vc.getVictory().getCategory()) {
                    case FISICA -> cf++;
                    case MENTAL -> cm++;
                    case ESPIRITUAL -> ce++;
                    case PESSOAL -> cp++;
                }
            }
        }
        Map<String, Integer> completion = new LinkedHashMap<>();
        completion.put("fisica", pct(cf, days));
        completion.put("mental", pct(cm, days));
        completion.put("espiritual", pct(ce, days));
        completion.put("pessoal", pct(cp, days));

        long totalVictories = checkinRepository.findByUserOrderByDateDesc(user).stream()
                .flatMap(c -> c.getVictoryCheckins().stream())
                .filter(VictoryCheckinModel::isCompleted).count();
        long activeDays = checkinRepository.countActiveDays(user);

        var progressCurrent = new ProgressStatsDto.ProgressCurrentDto(
                user.getStreakCurrent(), user.getStreakRecord(), totalVictories, activeDays
        );

        return new ProgressStatsDto(monthDtos, progressCurrent, completion);
    }

    @Override
    public HeatmapDataDto getHeatmap(UserModel user) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(27);
        List<CheckinModel> checkins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, start, today);
        Map<LocalDate, CheckinModel> byDate = new HashMap<>();
        checkins.forEach(c -> byDate.put(c.getDate(), c));

        List<HeatmapDayDto> days = new ArrayList<>();
        for (int i = 27; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            CheckinModel c = byDate.get(date);
            Map<String, Boolean> victories = new LinkedHashMap<>();
            for (VictoryCategory cat : VictoryCategory.values()) victories.put(cat.name().toLowerCase(), false);
            if (c != null) {
                c.getVictoryCheckins().stream()
                        .filter(VictoryCheckinModel::isCompleted)
                        .forEach(vc -> victories.put(vc.getVictory().getCategory().name().toLowerCase(), true));
            }
            days.add(new HeatmapDayDto(date.toString(), victories));
        }
        return new HeatmapDataDto(days);
    }

    private int pct(int count, int total) {
        return total == 0 ? 0 : (int) Math.round((double) count / total * 100);
    }
}
```

- [ ] **Step 3: Criar StatsController**

```java
package org.rod.kaizen_api.controllers;

import org.rod.kaizen_api.dtos.stats.HeatmapDataDto;
import org.rod.kaizen_api.dtos.stats.ProgressStatsDto;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.services.StatsService;
import org.rod.kaizen_api.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsService statsService;
    private final UserService userService;

    public StatsController(StatsService statsService, UserService userService) {
        this.statsService = statsService;
        this.userService = userService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<ProgressStatsDto> getProgress(@AuthenticationPrincipal String userId,
                                                        @RequestParam(defaultValue = "6") int months) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(statsService.getProgress(user, months));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapDataDto> getHeatmap(@AuthenticationPrincipal String userId) {
        UserModel user = userService.findById(UUID.fromString(userId));
        return ResponseEntity.ok(statsService.getHeatmap(user));
    }
}
```

- [ ] **Step 4: Compilar tudo**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: stats service and controller"
```

---

## Task 13: Smoke Test Final

- [ ] **Step 1: Subir a API**

```bash
./gradlew bootRun
```

Expected: `Started KaizenApiApplication on port 8080`

- [ ] **Step 2: Registrar usuário**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Rodger","email":"rodger@kaizen.com","password":"senha123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
echo "TOKEN=$TOKEN"
```

Expected: variável TOKEN preenchida com o JWT

- [ ] **Step 3: Criar victories (onboarding)**

```bash
curl -s -X PUT http://localhost:8080/api/v1/victories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"category":"FISICA","goal":"Malhar 30 min","subtasks":["Aquecimento","Treino"]},
    {"category":"MENTAL","goal":"Ler 20 páginas","subtasks":[]},
    {"category":"ESPIRITUAL","goal":"Meditar 10 min","subtasks":[]},
    {"category":"PESSOAL","goal":"Estudar inglês","subtasks":["Duolingo"]}
  ]' | python3 -m json.tool
```

Expected: JSON com array de 4 victories com IDs

- [ ] **Step 4: Testar checkin de hoje**

```bash
curl -s http://localhost:8080/api/v1/checkin/today \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Expected: JSON com `date`, `victories` (4 itens), `completedCount: 0`

- [ ] **Step 5: Togglear uma victory e verificar streak**

```bash
# Pegar o victoryId da primeira victory
VID=$(curl -s http://localhost:8080/api/v1/victories \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")

curl -s -X POST "http://localhost:8080/api/v1/checkin/victory/$VID/toggle" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

curl -s http://localhost:8080/api/v1/checkin/streak \
  -H "Authorization: Bearer $TOKEN"
```

Expected: checkin com `completedCount: 1`; streak `{"current":1,"record":1}`

- [ ] **Step 6: Testar heatmap e stats**

```bash
curl -s "http://localhost:8080/api/v1/stats/heatmap" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

curl -s "http://localhost:8080/api/v1/stats/monthly?months=3" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Expected: heatmap com 28 dias; monthly com 3 meses em labels PT

- [ ] **Step 7: Commit final**

```bash
git add -A
git commit -m "chore: smoke test passed — kaizen_api complete"
```
