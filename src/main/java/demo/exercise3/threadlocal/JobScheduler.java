package demo.exercise3.threadlocal;

import demo.exercise3.JobContext;
import demo.exercise3.Priority;

public class JobScheduler {
    private static final ThreadLocal<JobContext> jobContextHolder = new ThreadLocal<>();

    public void schedule(Job job, String jobName, Priority priority) {
        JobContext context = new JobContext(jobName, priority);
        try {
            jobContextHolder.set(context);
            runJob(job);
        } finally {
            jobContextHolder.remove();
        }
    }

    private void runJob(Job job) {
        job.execute();
    }

    public JobContext getContext() {
        return jobContextHolder.get();
    }

    public Object getJobMetadata(String key) {
        JobContext context = jobContextHolder.get();
        return (context != null) ? context.getMetadataValue(key) : null;
    }
}
