package demo.exercise3;

public class ScopedValueDefaultsExample {
    private static final ScopedValue<String> USER_NAME = ScopedValue.newInstance();

    void main() {

        String userNameUnbound = USER_NAME.orElse("Guest"); //①
        IO.println("No binding -> user name defaults to: "
                + userNameUnbound);

        // Using orElseThrow for validation
        try {
            USER_NAME.orElseThrow(() ->
                    new IllegalStateException("No user name bound yet!")); //②
        } catch (IllegalStateException e) {
            IO.println("Caught exception: " + e.getMessage());
        }

        // Within a bound scope
        ScopedValue.where(USER_NAME, "Bazlur").run(() -> {
            String boundUserName = USER_NAME.orElse("Guest"); //③
            IO.println("Within binding -> user name is: " + boundUserName);

            // This won't throw since the value is bound
            String validatedName = USER_NAME.orElseThrow(()
                    -> new IllegalStateException("No user name bound yet!")); //④
            IO.println("Validated name: " + validatedName);
        });

    }
}
