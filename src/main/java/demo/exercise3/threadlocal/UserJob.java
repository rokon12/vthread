package demo.exercise3.threadlocal;

public class UserJob implements Job {

    private final JobScheduler jobScheduler;

    public UserJob(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @Override
    public void execute() {
        System.out.println("User job is running!");

        Object creationTime = jobScheduler.getJobMetadata("creationTime");
        System.out.println("Job creation time: " + creationTime);

        processJobData();
    }

    private void processJobData() {
        Object priority = jobScheduler.getJobMetadata("priority");
        System.out.println("Processing job with priority: " + priority);
    }
}
