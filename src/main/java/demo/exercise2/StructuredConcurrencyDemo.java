
import static java.util.concurrent.StructuredTaskScope.*;

void main() {
    try {
        demo3();
    } catch (Throwable e) {
        e.printStackTrace();
    }
    sleep(Duration.ofSeconds(5));
}


String orchestrate(String userId) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        CompletableFuture<String> profile = CompletableFuture.supplyAsync(() -> fetch("profile", userId, 50), executor);
        CompletableFuture<String> prefs = CompletableFuture.supplyAsync(() -> fetch("prefs", userId, 40), executor);
        CompletableFuture<String> risk = CompletableFuture.supplyAsync(() -> fetch("risk", userId, 80), executor);

        CompletableFuture<Void> all = CompletableFuture.allOf(profile, prefs, risk);

        Instant deadline = Instant.now().plusMillis(200);
        try {
            all.orTimeout(Duration.between(Instant.now(), deadline).toMillis(), TimeUnit.MILLISECONDS).join();
            return "OK: " + Map.of(
                    "profile", profile.join(),
                    "prefs", prefs.join(),
                    "risk", risk.join()
            );
        } catch (CancellationException e) {
            profile.cancel(true);
            prefs.cancel(true);
            risk.cancel(true);
            throw new IllegalStateException("timed out orchestrating " + userId, e);
        } catch (Exception e) {
            profile.cancel(true);
            prefs.cancel(true);
            risk.cancel(true);
            throw e;
        }
    }
}

//void demo() {
//    String result = orchestrate("user-structured");
//    System.out.println("Structured fan-out: " + result);
//}


private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

void demo2() throws ExecutionException, InterruptedException {
    // What happens if profile.get() throws an exception?
    Future<Profile> profile = executor.submit(this::fetchProfile);
    Future<Preference> prefs = executor.submit(this::fetchPrefs);
    // This keeps running...
    Future<Risk> risk = executor.submit(this::fetchRisk);
    // ...and so does this.

    Profile p = profile.get(); // 💥 Exception!
}

void demo3() throws InterruptedException {
    try (var scope = StructuredTaskScope.open(Joiner.allSuccessfulOrThrow(),
            (configuration -> configuration.withTimeout(Duration.ofSeconds(3))))) {
        var profileTask = scope.fork(this::fetchProfile);
        var prefsTask = scope.fork(this::fetchPrefs);
        var risk = scope.fork(this::fetchRisk);
        scope.join();//wait for all, or fail fast
    }
}

void demo4() throws InterruptedException {
    try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
        scope.fork(() -> fetchFromMirror("mirror-1"));
        scope.fork(() -> fetchFromMirror("mirror-2"));
        scope.fork(() -> fetchFromMirror("mirror-3"));
        String fastest = scope.join();
        //slowest tasks are automatically canceled
    }
}

private String fetchFromMirror(String s) {
    return null;
}

Risk fetchRisk() {
    sleep(Duration.ofSeconds(1));
    IO.println("I'm fetching Risk");
    return new Risk();
}

Preference fetchPrefs() {
    sleep(Duration.ofSeconds(2));
    IO.println("I'm fetching Prefs");
    return new Preference();
}

void sleep(Duration duration) {
    try {
        Thread.sleep(duration);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}

private Profile fetchProfile() {
    throw new RuntimeException("Failed to fetch profile!");
//        sleep(Duration.ofSeconds(1));
//        IO.println("I'm fetching Profile");
//        return new Profile("Alice");
}

record Profile(String name) {
}

record Preference() {
}

record Risk() {
}


private static String fetch(String kind, String userId, long delayMillis) {
    try {
        Thread.sleep(delayMillis);
        return "%s:%s via %s".formatted(kind, userId,
                Thread.currentThread().isVirtual() ? "virtual" : "platform");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(kind + " cancelled", e);
    }
}

