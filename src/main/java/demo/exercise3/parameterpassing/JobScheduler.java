package demo.exercise3.parameterpassing;


import demo.exercise3.JobContext;
import demo.exercise3.Priority;

public class JobScheduler {

    public void schedule(Job job, String jobName, Priority priority) {
        JobContext context = new JobContext(jobName, priority);
        runJob(job, context);
    }

    private void runJob(Job job, JobContext context) {
        job.execute(context);
    }

    public Object getJobMetadata(String key, JobContext context) {
        if (context == null) {
            return null;
        }
        return context.getMetadataValue(key);
    }
}
