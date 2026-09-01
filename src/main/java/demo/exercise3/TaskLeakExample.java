package demo.exercise3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskLeakExample {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();


    void main() {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        // The first task sets the current user
        executor.submit(() -> {
            currentUser.set("Alice");
            simulateWork("Task 1");
            // Oops, forgot currentUser.remove()!
        });

        // The second task uses the same thread pool.
        // If it gets the same thread as Task 1, it might see "Alice".
        executor.submit(() -> {
            System.out.println("Before set, currentUser = " + currentUser.get());
            currentUser.set("Bob");
            simulateWork("Task 2");
            //currentUser.remove();  // Proper cleanup here
        });


        executor.shutdown();
    }

    private static void simulateWork(String taskName) {
        System.out.println(taskName + ": currentUser = " + currentUser.get());
    }
}

