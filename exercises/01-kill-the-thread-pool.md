# Exercise 1: Kill The Thread Pool

## What Is Broken

`MigrateCart` sends blocking pricing work through a small fixed thread pool. The code looks stable, but the pool creates wave behavior: only a few tasks run while the rest wait in the executor queue.

Virtual threads remove the need to pool threads just to survive blocking I/O. They do not remove the need for backpressure around scarce resources.

## Run The Failure

```bash
make exercise1
```

Expected failures:

- `VirtualThreadExecutorTest`
- `ScaleBeyondPoolTest`

## Your Task

Replace the fixed executor with a virtual-thread-per-task executor.

Start here:

- `src/main/java/ca/bazlur/migratecart/config/ExecutorConfig.java`

You are looking for:

```java
Executors.newFixedThreadPool(...)
```

Replace it with:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

## Success Criteria

- Submitted work runs on virtual threads.
- The pricing workload no longer completes in fixed-pool waves.
- `make exercise1` passes.

## Discussion Prompt

After removing the fixed pool, what should limit calls to a database, payment gateway, or remote service?
