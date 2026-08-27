# MigrateCart Virtual Threads Workshop

This is the participant repository for a hands-on Java virtual-thread migration.
It intentionally starts with broken implementations and failing tests. The failures are
the starting line for the workshop.

The repository does not contain solution branches, solution tags, or completed
implementations. Make your changes on your own branch as you progress through the
exercises.

## Requirements

- Java 25
- Maven 3.9+
- Git

Preview features are already enabled by the Maven build.

## Before The Workshop

Check your local environment and download the Maven dependencies:

```bash
make doctor
mvn test
```

`make doctor` should pass. `mvn test` should compile successfully and report 18 tests
run with 10 exercise failures; those failures are expected in the starter repository.

## Start Your Work

Create a branch so your progress is easy to preserve:

```bash
git switch -c my-migration
```

Then work through the exercises in order:

1. [Kill The Thread Pool](exercises/01-kill-the-thread-pool.md)
2. [Structured Fan-Out](exercises/02-structured-fan-out.md)
3. [Context Migration](exercises/03-context-migration.md)
4. [Diagnosing Pinned Carriers](exercises/04-diagnosing-pinned-carriers.md)
5. [The Bottleneck Moves](exercises/05-the-bottleneck-moves.md) — optional/take-home

Run only the exercise you are currently working on:

```bash
make exercise1
make exercise2
make exercise3
make exercise4
make exercise5
```

Run every application scenario and observe the current behavior:

```bash
make run
```

Run the scenario for one exercise before and after your change:

```bash
make run ARGS=exercise3
```

The runner reports behavior rather than asserting it, so it works in both the broken
starter and your migrated implementation.

Commit after each completed exercise. Earlier fixes remain in place while you move to
the next exercise.

## Exercise Map

| Exercise | Main production area | Lesson |
| --- | --- | --- |
| 1 | `config/ExecutorConfig.java` | Replace a fixed pool with virtual-thread-per-task execution. |
| 2 | `cart/CartAggregationService.java` | Give fan-out work a bounded lifetime with structured concurrency. |
| 3 | `cart/CartFacade.java`, `context/` | Bind request data to an operation with `ScopedValue`. |
| 4 | `diagnostics/HotPathInventoryCache.java` | Use runtime evidence and remove blocking from a serialized hot path. |
| 5 | `reporting/`, `downstream/` | Audit per-thread caches and explicitly bound scarce dependencies. |

## JFR Workflow

Exercise 4 includes a fast behavioral test and an optional JFR recording:

```bash
make exercise4
make pinning-jfr
```

The recording is written to `target/pinning.jfr`. If the JDK reports
`jdk.VirtualThreadPinned` events, they are printed and written to
`target/pinning-events.txt`. On Java 25, monitor-related carrier pinning may not appear;
the timing evidence still demonstrates hot-path serialization.

## Useful Files

- `exercises/` — participant instructions
- `src/main/java/` — code to migrate
- `src/test/java/` — executable success criteria
- `migration-checklist.txt` — production migration checklist
- `scripts/doctor.sh` — local setup verification
- `scripts/record-pinning.sh` — JFR recording helper

The tests are intentionally visible: they describe the required behavior. The completed
production implementation is not included in this repository.
