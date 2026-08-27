.PHONY: test doctor pinning-jfr exercise1 exercise2 exercise3 exercise4 exercise5

test:
	mvn test

doctor:
	./scripts/doctor.sh

pinning-jfr:
	./scripts/record-pinning.sh

exercise1:
	mvn -q -Dtest=VirtualThreadExecutorTest,ScaleBeyondPoolTest test

exercise2:
	mvn -q -Dtest=FailureCancellationTest,NoOrphanedTasksTest test

exercise3:
	mvn -q -Dtest=ContextPropagationTest,ChildTaskContextTest test

exercise4:
	mvn -q -Dtest=PinningDetectionTest test

exercise5:
	mvn -q -Dtest=ThreadLocalCacheAmplificationTest,DownstreamSaturationTest test
