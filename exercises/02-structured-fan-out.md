# Exercise 2: Structured Fan-Out

## What Is Broken

`CartAggregationService` fans out to pricing, inventory, and shipping with `CompletableFuture`. The happy path works, but failure behavior is weak: one branch can fail while a slow sibling keeps running.

The issue is not parallelism. The issue is lifetime. Child work should not outlive the request that created it.

## Run The Failure

```bash
make exercise2
```

Expected failures:

- `FailureCancellationTest`
- `NoOrphanedTasksTest`

## Your Task

Replace the unstructured `CompletableFuture` fan-out with `StructuredTaskScope`.

Start here:

- `src/main/java/ca/bazlur/migratecart/cart/CartAggregationService.java`

The migrated code should:

- open a task scope;
- fork pricing, inventory, and shipping work inside the scope;
- join through the scope;
- fail quickly when one branch fails;
- interrupt slow sibling work when the operation cannot complete.

## Success Criteria

- Failure surfaces without waiting for a slow sibling task.
- Slow sibling work is interrupted rather than orphaned.
- `make exercise2` passes.

## Discussion Prompt

When one downstream dependency fails, which sibling tasks should continue, and which should be cancelled?
