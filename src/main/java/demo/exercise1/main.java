//void main() {
//
//
//    AtomicInteger count = new AtomicInteger(0);
//    while (true) {
//        Thread.ofPlatform().start(() -> {
//            count.incrementAndGet();
//
//            if (count.get() % 1000 == 0) {
//                IO.println("count: " + count.get());
//            }
//            ;
//            LockSupport.park();
//
//        });
//
//
//        Thread.sleep();
//    }
//}
//
//
//int calculate(int a, int b) {
//    try {
//        Thread.sleep(Duration.ofHours(2));
//    } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//    }
//    return a + b;
//}