# AGENTS.md — XTBot

## Project Overview

XTBot (eXtensible Telegram Bot) is a **configuration-driven** Telegram bot framework. Bots are defined via YAML config files (stages, acceptors, actions), not imperative code. Java 21, Maven multi-module, Lombok throughout.

## Architecture

Five Maven modules with strict dependency flow:

```
xtbot-api          — Public API: annotations (@BotComponent, @Acceptor, @Executor, @Name, @Default) and DTOs (Update, Client, Message, Callback)
xtbot-internal-lib — Built-in acceptors/executors (prefixed `x.accept.*`, `x.exec.*`), depends on xtbot-api
xtbot-core         — Engine: config loading, stage lifecycle, Jinja rendering, event handling, Gateway interface; depends on xtbot-api + xtbot-internal-lib
xtbot-telegram     — Telegram adapter: implements Gateway, wires everything, contains main(); depends on xtbot-core
xtbot-test         — Test-only @BotComponent jar; copied to xtbot-core/src/test/resources/bot-test/ at package phase
```

**Key data flow:** Telegram SDK Update → `TgSdkUpdateToDataMapper` → `UpdateData` → `Gateway.consume()` → `EventHandler.run()` → stage lifecycle (initiate → complete → bind next) → `Gateway.produce(OutputData)` → Telegram API.

## Core Concepts

- **Stage lifecycle** (`EventHandler`): Each stage is *initiated* (sends message/buttons), then *completed* (runs acceptors → actions → save), then binds to *next* stage. On failure, `failStage` is bound. Both `initial` and `fail` stages are required and must be unique.
- **Acceptors** are `static boolean` methods annotated `@Acceptor("name")` on `@BotComponent` classes. They receive `(Update, String val)`.
- **Executors** are `static` methods annotated `@Executor("name")`. Parameters are matched by name (`@Name`) and support `@Default` values.
- **Jinja templates** (`JinjaRender` via Jinjava) are used in YAML config for `next`, `text`, button display/data, action args, and `save` values. Context map is built from `UpdateData`.
- **Internal components** are registered in `internal-components.txt` (classpath resource). External extensions are loaded from a JAR via ASM-based `@BotComponent` scanning (`ExternalJarClassLoader`).

## Build & Run

```bash
# Build all modules (requires Java 21)
mvn clean package -DskipTests

# Run tests
mvn test

# Build Docker image
docker build -t xtbot .

# Run (token via env var)
X_TELEGRAM_TOKEN=<token> java -jar xtbot-telegram/target/xtbot-telegram-0.4.0-SNAPSHOT.jar <config.yml>
```

**Important:** `xtbot-test` jar is copied to `xtbot-core/src/test/resources/bot-test/bot-test.jar` during `package` phase. If `xtbot-core` tests fail with missing test jar, run `mvn package -pl xtbot-test` first.

## Conventions & Patterns

- **Lombok `@Accessors(fluent = true)`** is used extensively — getters have no `get` prefix (e.g., `stage.name()` not `stage.getName()`).
- **Records** for immutable data: `Properties`, `StageProps`, `AcceptorProps`, `ActionProps`, all DTO types in `xtbot-api`.
- **`Mapper<S, T>` functional interface** — all transformations implement `S map(T t, Object... options)`.
- Acceptor/Executor methods must be **`public static`** on `@BotComponent @UtilityClass` classes. See `CallbackAcceptor`, `TextExecutors` for examples.
- Built-in names use `x.` prefix: `x.accept.callback`, `x.accept.command`, `x.accept.message`, `x.exec.text.match`, `x.exec.text.split`, `x.exec.system.pause`, `x.exec.ssh.*`.
- YAML config uses **snake_case** (`remove_buttons`, `send_typing`, `parse_mode`). Jackson `PropertyNamingStrategies.SNAKE_CASE` handles mapping.
- Tests use JUnit 5 + Mockito. Config/properties tests load YAML from `xtbot-core/src/test/resources/properties/`.

## Key Files

| Purpose | Path |
|---|---|
| Entry point | `xtbot-telegram/.../XTelegramBot.java` |
| Gateway impl | `xtbot-telegram/.../TelegramSdkApiBot.java` |
| Event processing | `xtbot-core/.../handler/EventHandler.java` |
| Stage/Config model | `xtbot-core/.../config/Config.java`, `Stage.java`, `Action.java`, `Acceptor.java` |
| YAML config schema | `xtbot-core/.../properties/data/Properties.java`, `StageProps.java` |
| Extension loading | `xtbot-core/.../extension/CommonMethodsLoader.java` |
| Internal component registry | `internal-components.txt` (classpath) |
| Example YAML config | `xtbot-telegram/src/test/resources/stress-props.yml` |

