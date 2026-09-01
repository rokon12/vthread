# MigrateCart Virtual Threads Workshop

This is the participant repository for a hands-on migration of a blocking Spring Boot
service to virtual threads, structured concurrency, and scoped request context.

> [!IMPORTANT]
> The starter is deliberately broken. A successful setup reports **20 tests run and 10
> failures**. Those failures are the starting line for the workshop.

The happy-path HTTP endpoint already works. The tests expose problems that only become
visible under concurrency, failure, cancellation, context propagation, serialization,
and downstream saturation.

## Requirements

- Java 25
- Optional: Java 21 on the machine presenting the pre–Exercise 4 comparison
- Maven 3.9+
- Git
- `curl` or another HTTP client

Preview features are configured by the Maven build. Use Java 25 for the coding
exercises—the structured concurrency preview API differs from the JDK 21 API.

## Setup

Clone the repository, verify the toolchain, and create a branch for your work:

```bash
git clone https://github.com/rokon12/vithread-workshop-participant.git
cd vithread-workshop-participant
make doctor
git switch -c my-migration
```

Download the dependencies and confirm the expected baseline:

```bash
mvn test
```

Expected summary:

```text
Tests run: 20, Failures: 10, Errors: 0, Skipped: 0
```

`mvn test` exits with a failure status at this point. That is intentional.

### Switch Between Workshop JDKs

Open a temporary shell configured for either installed JDK:

```bash
make jdk21
make jdk25
```

Run `exit` to return to your previous shell. Make cannot change its parent terminal, so
the selected environment lives in a subshell. For a single command, no interactive
shell is needed:

```bash
make jdk21 CMD='java -version'
make jdk25 CMD='mvn test'
```

The helpers use the same automatic discovery as `make pinning-compare`. If needed, set
`JDK21_HOME` or `JDK25_HOME` to an installation directory.

## Run The Spring Boot Application

Start the service:

```bash
make run
```

The command stays attached to the server. Leave it running and use another terminal to
load a cart:

```bash
curl \
  -H 'X-User-Id: user-7' \
  -H 'X-Trace-Id: trace-123' \
  'http://localhost:8080/api/carts/sku-42?quantity=2'
```

Expected response:

```json
{
  "userId": "user-7",
  "traceId": "trace-123",
  "sku": "sku-42",
  "quantity": 2,
  "price": "$42.00",
  "inventoryStatus": "in-stock",
  "shippingEta": "tomorrow"
}
```

Stop the application with `Ctrl+C`.

The request path is:

```text
GET /api/carts/{sku}
          |
          v
    CartController
          |
          v
CartAggregationService
     /      |       \
 pricing inventory shipping
```

## HTTP API

### `GET /api/carts/{sku}`

| Input | Location | Default | Description |
| --- | --- | --- | --- |
| `sku` | path | required | Product identifier. |
| `quantity` | query | `1` | Positive cart quantity. |
| `X-User-Id` | header | `workshop-user` | Request user copied into the response. |
| `X-Trace-Id` | header | `trace-demo` | Trace identifier copied into the response. |

A quantity below one returns HTTP 400.

## Workshop Workflow

For each exercise:

1. Read the exercise sheet.
2. Run only that exercise’s focused tests.
3. Change the indicated production code.
4. Rerun the focused tests until they pass.
5. Commit your progress before moving on.

Earlier exercises remain fixed as you progress. You do not need checkpoint branches or
tags.

| Exercise | Reading | Command | Initial failures | Goal |
| --- | --- | --- | ---: | --- |
| 1. Kill the Thread Pool | [`01-kill-the-thread-pool.md`](exercises/01-kill-the-thread-pool.md) | `make exercise1` | 2 | Replace the fixed application pool with virtual-thread-per-task execution. |
| 2. Structured Fan-Out | [`02-structured-fan-out.md`](exercises/02-structured-fan-out.md) | `make exercise2` | 2 | Give pricing, inventory, and shipping one failure and cancellation boundary. |
| 3. Context Migration | [`03-context-migration.md`](exercises/03-context-migration.md) | `make exercise3` | 2 | Bind request metadata to the logical operation instead of a thread. |
| 4. Diagnosing Pinned Carriers | [`04-diagnosing-pinned-carriers.md`](exercises/04-diagnosing-pinned-carriers.md) | `make exercise4` | 1 | Use evidence and remove blocking from a serialized hot path. |
| 5. The Bottleneck Moves | [`05-the-bottleneck-moves.md`](exercises/05-the-bottleneck-moves.md) | `make exercise5` | 3 | Audit per-thread caches and explicitly bound scarce dependencies. |

Exercise 5 is optional/take-home and sits outside the two-hour core workshop.

## Useful Commands

| Command | Purpose |
| --- | --- |
| `make doctor` | Verify Java, Maven, and Git, then compile the project. |
| `make jdk21` / `make jdk25` | Open a temporary shell using the selected JDK. |
| `make run` | Start the Spring Boot service on port 8080. |
| `make exercise1` … `make exercise5` | Run one exercise’s focused tests. |
| `make test` | Run the full suite. It becomes green after all exercises are complete. |
| `make pinning-compare` | Run identical bytecode on JDK 21 and JDK 25 before Exercise 4. |
| `make pinning-jfr` | Record the current Exercise 4 behavior with JFR. |

## JFR Workflow

Immediately before Exercise 4, compare monitor pinning on JDK 21 and JDK 25:

```bash
make pinning-compare
```

The helper finds SDKMAN, macOS, and common Linux JDK installations automatically. If
needed, provide the installation directories explicitly:

```bash
JDK21_HOME=/path/to/jdk-21 JDK25_HOME=/path/to/jdk-25 make pinning-compare
```

It compiles once with `--release 21`, runs the identical bytecode on both runtimes, and
writes both JFR recordings and extracted events under `target/`. Its independent
monitors isolate carrier pinning from ordinary application lock contention.

Exercise 4 then uses a behavioral test and an optional recording of the real cache:

```bash
make exercise4
make pinning-jfr
```

The helper writes:

```text
target/pinning.jfr
target/pinning-events.txt
```

On Java 25 the extracted events file is empty. JEP 491 removed monitor-related carrier
pinning, so `jdk.VirtualThreadPinned` reports nothing, and this runtime does not emit
`jdk.JavaMonitorEnter` for virtual threads either. Both counts are zero by design, not
because the setup is wrong.

The broken implementation still serializes callers. The evidence on Java 25 is the
elapsed time from `make exercise4` together with the JDK 21 side of `make
pinning-compare`, where the identical bytecode records pinning events and runs roughly
eight times slower.

## Build An Executable JAR

The starter tests fail intentionally, so skip test execution when packaging before you
have completed the exercises:

```bash
mvn -q -DskipTests package
java --enable-preview -jar target/vithread-workshop-1.0-SNAPSHOT.jar
```

## Troubleshooting

### Wrong Java version

Run `make doctor`. This workshop requires Java 25.

### Preview-feature compilation error

Run Maven from the project root. The POM configures preview features for compilation,
tests, Spring Boot, and the executable JAR command shown above.

### Port 8080 is already in use

Stop the other process or run the packaged JAR on a different port:

```bash
java --enable-preview -jar target/vithread-workshop-1.0-SNAPSHOT.jar --server.port=8081
```

### Maven is slow on the first run

The first build downloads Spring Boot and test dependencies. Run `mvn test` before the
workshop while you have a reliable network connection.

## Repository Layout

```text
src/main/java/          Spring Boot application and production code to migrate
src/test/java/          Exercise tests and concurrency test support
exercises/              Step-by-step participant instructions
scripts/doctor.sh       Toolchain verification
scripts/record-pinning.sh
migration-checklist.txt Production migration handout
```

The tests are intentionally visible because they define the required behavior. The
completed production implementation and solution history are not included in this
repository.
