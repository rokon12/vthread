package demo.exercise3.parameterpassing;

import demo.exercise3.JobContext;

public class UserJob implements Job {

    private final JobScheduler jobScheduler;

    public UserJob(JobScheduler jobScheduler) {
        this.jobScheduler = jobScheduler;
    }

    @Override
    public void execute(JobContext context) {
        System.out.println("User job is running!");

        Object creationTime = jobScheduler.getJobMetadata("creationTime", context);
        System.out.println("Job creation time: " + creationTime);

        processJobData(context);
    }

    private void processJobData(JobContext context) {
        Object priority = jobScheduler.getJobMetadata("priority", context);
        System.out.println("Processing job with priority: " + priority);
    }
}
