# Exercise 4: Diagnosing Pinned Carriers

## What Is Broken

The service now uses virtual threads, but a hot path still serializes work by blocking while synchronized. On runtimes affected by carrier pinning, this can also keep a virtual thread mounted while it blocks.

This exercise is about evidence. A migration is not done just because the code compiles.

## Run The Failure

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
- `src/main/java/ca/bazlur/migratecart/diagnostics/PinningLoadService.java`

The migrated code should:

- avoid blocking inside a synchronized method or block;
- keep the cache state protected;
- let concurrent virtual-thread work complete without monitor-bound serialization.

## Success Criteria

- The load test completes materially faster.
- `make exercise4` passes.
- You can explain what runtime evidence you would collect in production.

## Discussion Prompt

Would you fix this by changing application code, upgrading the runtime, or both? What evidence would decide?
