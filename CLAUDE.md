# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Ivy Wallet is an open-source Android money manager (manual expense tracker): 100% Kotlin + Jetpack Compose, Hilt, Coroutines/Flow, ArrowKt, Room, DataStore, Ktor. Requires Java 17+. Upstream (Ivy-Apps/ivy-wallet) has been unmaintained since Nov 2024; this repo is a fork, so development targets this fork's `main`.

## Commands

Run from the repo root (the `scripts/*.sh` wrappers are thin shells around these):

```bash
./gradlew assembleDebug                      # build (applicationId com.ivy.wallet.debug)
./gradlew testDebugUnitTest                  # all JVM unit tests
./gradlew :shared:domain:testDebugUnitTest --tests "com.ivy.domain.usecase.balance.BalanceBuilderTest"   # single test class
./gradlew :shared:domain:testDebugUnitTest --tests "*BalanceBuilderTest.some test name*"                  # single test method
./gradlew verifyPaparazziDebug               # screenshot tests (recordPaparazziDebug to update golden images)
./gradlew :shared:data:core:connectedDebugAndroidTest   # integration tests (need device/emulator); also :shared:domain
./gradlew detekt                             # lint; baseline at config/detekt/baseline.yml (run it on its own — combined with test tasks the root task fails on build-output inputs; judge by the exit code, compose-lint messages span multiple lines)
./gradlew detektFormat                       # auto-fix formatting
./gradlew lintR                              # Android lint (report: build/reports/lint/lint.html)
./gradlew assembleDemo -PcomposeCompilerReports=true && ./gradlew :ci-actions:compose-stability:run   # Compose stability check
```

Build types: `debug` (default), `release`, `demo` (minified, debug-signed). CI in `.github/workflows` runs unit tests, detekt, lint, Paparazzi, compose stability, and integration tests.

## Module layout

Gradle multi-module with typesafe project accessors (`projects.shared.data.core`) and a version catalog (`gradle/libs.versions.toml`). Module build logic lives in convention plugins in `buildSrc/src/main/kotlin` — a feature module's `build.gradle.kts` is usually just `id("ivy.feature")` + namespace + dependencies. Other plugins: `ivy.module`, `ivy.compose`, `ivy.hilt`, `ivy.room`, `ivy.paparazzi`, `ivy.integration.testing`, `ivy.widget`.

- `app/` — `RootActivity`, `IvyAndroidApp`, and `IvyNavGraph.kt`.
- `feature/*` — one module per screen/area (home, accounts, budgets, loans, reports, edit-transaction, import-data, settings, …).
- `shared/data/model` — the domain model: sealed ADTs + validated value classes (`Transaction` = `Income | Expense | Transfer`, `PositiveValue`, `NotBlankTrimmedString`, typed IDs like `TransactionId`). `*-testing` sibling modules hold fixtures/fakes.
- `shared/data/core` — Room DB (`IvyRoomDatabase`), entities, read/write DAOs, mappers (entity → domain, return `Either`), repositories, DataStore, Ktor exchange-rate source, backup/CSV.
- `shared/domain` — use cases (balance, stats, exchange, CSV) and feature flags (`domain/features`).
- `shared/ui/core` — design system and `ComposeViewModel`; `shared/ui/navigation` — `Screen` definitions and `Navigation`.
- `temp/legacy-code`, `temp/old-design` — pre-refactor code still used by most features.
- `widget/*` — Glance home-screen widgets. `ci-actions/*` — Kotlin programs run by GitHub Actions.

## Architecture

Target architecture (see `docs/guidelines/`): **data layer → optional domain layer (UseCases) → UI layer**, with data mapped raw model → domain model → view-state.

- **Screens use MVI with the Compose runtime inside the ViewModel.** ViewModels extend `ComposeViewModel<UiState, UiEvent>` (`shared/ui/core/.../ComposeViewModel.kt`) and implement `@Composable fun uiState()` (using `mutableStateOf`, `LaunchedEffect`, `remember`, `collectAsState`) plus `onEvent(event)`. Don't convert these to StateFlow pipelines. A feature typically has `XScreen.kt`, `XViewModel.kt`, `XScreenState.kt`, `XScreenEvent.kt`.
- View-state holds only primitives and `@Immutable`/`kotlinx.collections.immutable` types (`ImmutableList`, `persistentListOf`) so Compose stability checks pass. Composables stay dumb: render state, emit events.
- **No throwing for expected failures**: fallible operations return Arrow `Either` (`either {}`, `bind()`, `raise()`, `ensure()`). Data sources wrap IO into `Either`; repositories are main-safe (`withContext` on IO dispatcher).
- **Model with ADTs and exact types**: sealed interfaces for states/variants, value classes with validating `from()` / `unsafe()` constructors in the domain. DTOs/entities may use primitives.
- **Navigation**: screens are sealed `Screen` objects in `shared/ui/navigation/.../Screens.kt`, dispatched by the `when` in `app/src/main/java/com/ivy/IvyNavGraph.kt`. A new screen needs an entry in both.
- **Room**: schema changes require bumping the version in `IvyRoomDatabase.kt` and adding a `MigrationXtoY_Description` in `shared/data/core/.../db/migration`.

### Legacy migration (important)

The codebase is mid-migration and the guidelines describe the target, not the majority of the code. `temp/legacy-code` contains the old "FRP" action classes (`*Act`, e.g. `AccountsAct`, `HistoryTrnsAct` under `com.ivy.wallet.domain.action`), legacy models (`com.ivy.legacy.datamodel.*`, `com.ivy.base.legacy.Transaction`), and `*Creator` logic; `com.ivy.data.temp.migration` (in `shared/data/core`) has bridging helpers. Many ViewModels mix new repositories with legacy Acts, and most `Screen`s are `isLegacy = true`. There are two parallel `Transaction` types — check imports carefully. Prefer the new data model/repositories for new code, but match the surrounding idiom when making small edits to legacy-heavy screens.

## Testing conventions

- JUnit4 + Kotest assertions (`shouldBe`) + MockK; coroutine tests use `runTest`. Property-based tests use Kotest property/Arrow extensions (e.g. `AccountStatsUseCasePropertyTest`).
- Test names are backtick sentences; bodies follow `// Given`, `// When`, `// Then`. Put shared fixtures in a companion object or a `SomethingFixtures` object; prefer fakes when they make the test simpler than mocks.
- Paparazzi tests extend `PaparazziScreenshotTest` (`shared/ui/testing`), run with `TestParameterInjector` over `PaparazziTheme` (light/dark), and call `snapshot(theme) { ... }`.

## Guidelines

The maintainers' main rule (`docs/Guidelines.md`): keep changes simple and pragmatic, don't add complexity. Detekt and Slack compose-lints are enforced in CI.
