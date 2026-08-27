# Exercise 4: Diagnosing Pinned Carriers

## What Is Broken

The service now uses virtual threads, but a synchronized cache miss performs blocking
inventory I/O while holding the cache monitor. One slow miss prevents unrelated SKUs
from checking or populating the cache. On runtimes affected by carrier pinning, the
blocked virtual thread can also remain mounted on its carrier.

This exercise is about evidence. A migration is not done just because the code compiles.

## Before You Start: See Pinning Change Across JDKs

The complete workshop uses JDK 25, but monitor pinning was removed in JDK 24. Run the
small isolated comparison before changing the cache:

```bash
make pinning-compare
```

The helper finds JDK 21 and JDK 25 automatically when possible. Otherwise provide their
installation directories:

```bash
JDK21_HOME=/path/to/jdk-21 JDK25_HOME=/path/to/jdk-25 make pinning-compare
```

It compiles one Java 21 class and runs the identical bytecode on both runtimes. Each
virtual thread uses a different monitor, so the JDK 21 slowdown and JFR events isolate
carrier pinning rather than ordinary lock contention. JDK 25 should show no
monitor-pinning event for that code and should complete much faster.

That runtime improvement does not fix the cache below: its single, oversized critical
section still serializes unrelated keys on JDK 25.

## Run The Exercise Failure

```bash
make exercise4
```

For JFR evidence from the starter implementation, run:

```bash
make pinning-jfr
```

Expected failure:

- `PinningDetectionTest`

## Your Task

Remove the hot-path blocking while the monitor is held.

Start here:

- `src/main/java/ca/bazlur/migratecart/diagnostics/HotPathInventoryCache.java`

The migrated code should:

- check cached state while holding a short lock;
- perform the blocking inventory load after releasing that lock;
- reacquire the lock briefly to publish the loaded value;
- return the value that won publication when concurrent misses race;
- let concurrent virtual-thread work complete without monitor-bound serialization.

Simply replacing `synchronized` with `ReentrantLock` is not sufficient. Holding any
exclusive lock during the blocking call still serializes the workload, even if the new
lock changes the carrier-pinning behavior.

## Success Criteria

- The load test completes materially faster.
- Repeated reads use the cached value.
- Concurrent readers agree on the value stored in the cache.
- `make exercise4` passes.
- You can explain what runtime evidence you would collect in production.

## Discussion Prompt

Would you fix this by changing application code, upgrading the runtime, or both? What evidence would decide?
