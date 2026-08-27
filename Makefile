.PHONY: test run doctor jdk21 jdk25 pinning-compare exercise4-demo pinning-jfr \
	exercise1 exercise2 exercise3 exercise4 exercise5

test:
	mvn test

run:
	mvn -q -DskipTests spring-boot:run

doctor:
	./scripts/doctor.sh

jdk21:
	./scripts/with-jdk.sh 21 $(CMD)

jdk25:
	./scripts/with-jdk.sh 25 $(CMD)

pinning-compare:
	./scripts/compare-pinning.sh

exercise4-demo: pinning-compare

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
