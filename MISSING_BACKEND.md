# Kaizen API — Pendências para integração com o app

Issues encontrados ao remover o mock do app e conectar na API real.
Ordenados por criticidade.

---

## 🔴 Crítico — quebra imediata

### 1. `VictoryCategory` enum serializado em MAIÚSCULAS

**Problema:** O enum Java serializa como `"FISICA"`, `"MENTAL"`, `"ESPIRITUAL"`, `"PESSOAL"`.
O app espera minúsculas: `"fisica"`, `"mental"`, `"espiritual"`, `"pessoal"`.
Isso quebra toda a renderização de cards por categoria e os filtros de check-in.

**Onde corrigir:** `VictoryCategory.java`

**Solução:**
```java
import com.fasterxml.jackson.annotation.JsonValue;

public enum VictoryCategory {
    FISICA, MENTAL, ESPIRITUAL, PESSOAL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }
}
```
Também adicionar `@JsonCreator` para aceitar lowercase no request body:
```java
@JsonCreator
public static VictoryCategory from(String value) {
    return VictoryCategory.valueOf(value.toUpperCase());
}
```

---

### 2. `LocalDate` e `LocalDateTime` serializados como array, não como string ISO

**Problema:** Sem configuração explícita, Jackson serializa:
- `LocalDate` → `[2026, 6, 15]` (array)
- `LocalDateTime` → `[2026, 6, 15, 12, 0, 0]` (array)

O app espera strings ISO: `"2026-06-15"` e `"2026-06-15T12:00:00"`.

Afetados:
- `TodayCheckinDto.date` → quebra a tela de check-in
- `UserProfileDto.createdAt` → pode quebrar a tela de perfil

**Onde corrigir:** `application.properties`

**Solução:**
```properties
spring.jackson.serialization.write-dates-as-timestamps=false
```

Também adicionar a dependência do módulo JavaTime ao Jackson, se ainda não estiver:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```
(Spring Boot inclui via `spring-boot-starter-web`, mas confirme que o `ObjectMapper` registra o módulo automaticamente.)

---

## 🟡 Importante — quebra em fluxo específico

### 3. `GET /victories` retorna `[]` para usuário novo (onboarding não dispara)

**Problema:** O app usa `victories === null` para detectar que o usuário ainda não configurou suas vitórias e redirecionar ao onboarding.
O backend retorna `[]` (lista vazia) quando o usuário não tem vitórias, então o guard nunca redireciona.

**Solução sugerida (backend):** Retornar `204 No Content` quando o usuário não tem vitórias configuradas, ou adicionar um campo no response que indique se o setup foi concluído.

**Alternativa (já aplicada no app):** O store trata `[]` como `null`, disparando o onboarding. Isso funciona, mas perde a semântica de "usuário deletou todas as vitórias" vs "nunca configurou".

---

## 🟢 Informativo — sem impacto imediato

### 4. Endpoint `resetToday` ausente

**Problema:** O checkin service do app tinha um método `resetToday()` hardcoded no mock (helper de desenvolvimento para zerar o check-in do dia).
Não existe endpoint correspondente no backend.

**Status:** Corrigido no app — em modo real, `resetToday()` agora chama `GET /checkin/today` (re-fetch), que é inócuo.
Caso queira expor o reset para fins de QA/teste, criar:
```
DELETE /checkin/today   (ou POST /checkin/today/reset)
```

---

### 5. `ProgressStats.current.total` e `activeDays` são `long` no Java

**Problema potencial:** `ProgressCurrentDto` usa `long total` e `long activeDays`.
Jackson serializa `long` como número JSON sem aspas, o que é compatível com o TypeScript `number`.
Não é um bug hoje, mas se os valores ultrapassarem `Number.MAX_SAFE_INTEGER` haverá perda de precisão.

**Ação:** Nenhuma necessária por ora.

---

### 6. `HeatmapDayDto.victories` e `WeeklyDayDto.victories` são `Map<String, Boolean>`

**Nota:** Ambos serializam como objeto JSON `{ "fisica": true, ... }` que é compatível com o tipo TypeScript `{ [key in VictoryCategory]: boolean }`.
Porém, como as chaves vêm do backend como strings (potencialmente uppercase após o fix do item 1 acima), confirmar que após o fix do enum as chaves do map também estejam em lowercase.

**Ação:** Testar após aplicar o fix do `VictoryCategory`.