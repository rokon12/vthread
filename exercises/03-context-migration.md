# Exercise 3: Context Migration

## What Is Broken

The request context is stored in `ThreadLocal`. That ties user and trace state to a thread, not to the request. The result is two different bugs:

- context can leak after a request completes;
- child work can miss the request context entirely.

Virtual threads make this easier to notice, but the root problem is the same: request lifetime and thread lifetime are different things.

## Run The Failure

```bash
make exercise3
```

Expected failures:

- `ContextPropagationTest`
- `ChildTaskContextTest`

## Your Task

Replace the thread-bound context holder with scoped request context.

Start here:

- `src/main/java/ca/bazlur/migratecart/cart/CartFacade.java`
- `src/main/java/ca/bazlur/migratecart/context/RequestContextHolder.java`
- `src/main/java/ca/bazlur/migratecart/context/RequestContext.java`

The migrated code should:

- bind request context for the duration of `handleRequest`;
- let child tasks observe the same user and trace ID;
- leave no request context behind after the request returns.

## Success Criteria

- Request context is cleared after the request completes.
- Child tasks observe the bound request context.
- `make exercise3` passes.

## Discussion Prompt

Where would you still need a bridge to legacy `ThreadLocal` context in a real Spring application?
