# Exercise 5: The Bottleneck Moves

This exercise is optional and sits outside the two-hour plan. It covers what goes wrong
*after* a successful virtual-thread migration.

## What Is Broken

Two things that were correct under a fixed thread pool are now wrong, and neither one
throws an exception at the point of failure.

**Part A.** `OrderTimestampFormatter` caches a `SimpleDateFormat` in a `ThreadLocal`,
because `SimpleDateFormat` is not thread-safe. On a pooled executor that cache is
populated once per pooled thread. Virtual threads are never reused, so the cache is
populated once per task. The cache still returns correct results. It has simply stopped
being a cache.

**Part B.** `InventoryGateway` calls a backend with a hard capacity limit. Nothing in the
gateway limits how many callers arrive at once. Under a fixed pool of twenty threads that
was invisible, because the pool could not produce a twenty-first concurrent caller. The
pool was doing the bounding, and nobody wrote that down.

## Run The Failure

```bash
make exercise5
```

Expected failures:

- `ThreadLocalCacheAmplificationTest.virtualThreadsShouldNotRecreateTheCachedFormatter`
- `DownstreamSaturationTest.virtualThreadFanOutShouldNotOverwhelmTheBoundedBackend`
- `DownstreamSaturationTest.concurrencyAtTheBoundedResourceShouldStayWithinCapacity`

Read the numbers in the failure messages before you change anything. They are the exercise.

## Your Task

**Part A.** Make the expensive object be constructed a bounded number of times regardless
of threading model.

Start here:

- `src/main/java/ca/bazlur/migratecart/reporting/OrderTimestampFormatter.java`

`java.time.format.DateTimeFormatter` is immutable and thread-safe, which
`SimpleDateFormat` is not. An object that is safe to share does not need a per-thread
cache. `ca.bazlur.migratecart.observability.CountingThreadLocal` is a drop-in replacement
for `ThreadLocal.withInitial(...)` that counts initializations; use it in your own
codebase to find this failure mode before it shows up as an allocation graph.

**Part B.** Bound the concurrency reaching the backend explicitly.

Start here:

- `src/main/java/ca/bazlur/migratecart/downstream/InventoryGateway.java`

Acquire a permit before the call and release it in a `finally` block:

```java
private final Semaphore permits = new Semaphore(backend.capacity());
```

## Success Criteria

- The formatter is constructed a bounded number of times — once, in the intended fix.
- The rendered timestamp is unchanged.
- The backend rejects nothing, and peak concurrency at the backend stays within capacity.
- `make exercise5` passes.

## Discussion Prompt

The semaphore bounds concurrency. It does not apply backpressure. Callers that cannot get
a permit still queue — they just queue inside your process instead of at the database.
Where should the queue actually form, and what should happen to a caller that has been
waiting too long?

Second: which limits in your own service are currently enforced by a thread pool size
that nobody has written down as a limit?
