package io.hypertrack.smart_scheduler;

/**
 * Created by piyush on 07/10/16.
 */
public class Job {

    /**
     * Job parameters
     */
    private final int jobId;
    private final int jobType;
    private final SmartScheduler.JobScheduledCallback jobScheduledCallback;
    private final int networkType;
    private final boolean isPeriodic;
    private final long intervalMillis;
    private final long initialDelayInMillis;
    private final Long flexInMillis;

    // Threshold to schedule via Handlers
    protected static final long JOB_TYPE_HANDLER_THRESHOLD = 60000;

    /**
     * Network Types
     */
    public abstract class NetworkType {
        /**
         * Default
         */
        public static final int NETWORK_TYPE_ANY = 2;
        /**
         * This job requires network connectivity
         */
        public static final int NETWORK_TYPE_CONNECTED = 0;
        /**
         * This job requires network connectivity that is unmetered
         */
        public static final int NETWORK_TYPE_UNMETERED = 1;
    }

    /**
     * Job Types
     */
    public abstract class Type {
        /**
         * Default
         */
        public static final int JOB_TYPE_NONE = 0;
        /**
         * Use Handler type job if the frequency required for the Job is small enough that it can't
         * be accomplished by using PeriodicTasks
         */
        public static final int JOB_TYPE_HANDLER = 1;
        /**
         * Use Alarm type job if the frequency required for the Job is large enough to be using alarms
         */
        public static final int JOB_TYPE_ALARM = 3;
    }

    /**
     * Method to generate unique JobId
     *
     * @return Returns a uniquely generated JobID
     */
    public static int generateJobID() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * Unique job id associated with this class. This is assigned to your job by the scheduler.
     *
     * @return Returns the id assigned to the job
     */
    public int getJobId() {
        return jobId;
    }

    /**
     * One of {@link Job.Type#JOB_TYPE_HANDLER},
     * or
     * {@link Job.Type#JOB_TYPE_ALARM}.
     *
     * @return Returns the type of job assigned to the job
     */
    public int getJobType() {
        return jobType;
    }

    /**
     * Name of the callback class that will be called when Job is scheduled by the SmartScheduler.
     *
     * @return Returns the JobScheduled callback assigned to the job
     */
    public SmartScheduler.JobScheduledCallback getJobScheduledCallback() {
        return jobScheduledCallback;
    }

    /**
     * One of {@link Job.NetworkType#NETWORK_TYPE_CONNECTED},
     * {@link Job.NetworkType#NETWORK_TYPE_ANY}, or
     * {@link Job.NetworkType#NETWORK_TYPE_UNMETERED}.
     *
     * @return Returns the NetworkType assigned to the job
     */
    public int getNetworkType() {
        return networkType;
    }

    /**
     * Track whether this job will repeat with a given period.
     *
     * @return Returns if the job isPeriodic
     */
    public boolean isPeriodic() {
        return isPeriodic;
    }

    /**
     * Set to the interval between occurrences of this job. This value is <b>not</b> set if the
     * job does not recur periodically.
     *
     * @return Returns the Interval (in millis) assigned to the job
     */
    public long getIntervalMillis() {
        return intervalMillis;
    }

    /**
     * The interval for the initial delay (first occurrence) of the periodic job. This value is <b>not</b> set if the
     * job does not recur periodically.
     *
     * @return Returns the Initial Delay (in millis) assigned to the job
     */
    public long getInitialDelayInMillis() {
        return initialDelayInMillis;
    }

    /**
     * The Flex interval for the periodic job (how close to the end of the period set in
     * intervalInMillis the job is supposed to be executed). This value is <b>not</b> set if the
     * job does not recur periodically.
     *
     * @return Returns the Flex Interval (in millis) assigned to the job
     */
    public Long getFlexInMillis() {
        return flexInMillis;
    }

    private Job(Job.Builder b) {
        jobId = b.mJobId;
        jobType = b.mJobType;
        jobScheduledCallback = b.mJobScheduledCallback;
        networkType = b.mNetworkType;
        isPeriodic = b.mIsPeriodic;
        intervalMillis = b.mIntervalMillis;
        initialDelayInMillis = b.mInitialDelayInMillis;
        flexInMillis = b.mFlexInMillis;
    }

    /**
     * Builder class for constructing {@link Job} objects.
     */
    public static final class Builder {
        private int mJobId;
        private int mJobType = Job.Type.JOB_TYPE_NONE;
        private SmartScheduler.JobScheduledCallback mJobScheduledCallback;

        private int mNetworkType = Job.NetworkType.NETWORK_TYPE_ANY;

        // Time interval parameter.Nullable
        private long mIntervalMillis = 60000;

        // Periodic parameters.
        private boolean mIsPeriodic = false;
        private long mInitialDelayInMillis = 60000;

        private Long mFlexInMillis = null;

        /**
         * @param jobScheduledCallback The endpoint that you implement that will receive the callback from the
         *                             SmartScheduler.
         */
        public Builder(SmartScheduler.JobScheduledCallback jobScheduledCallback) {
            generateJobID();
            mJobScheduledCallback = jobScheduledCallback;
        }

        /**
         * @param jobScheduledCallback The endpoint that you implement that will receive the callback from the
         *                             SmartScheduler.
         * @param jobType              Type of Job to be scheduled
         */
        public Builder(SmartScheduler.JobScheduledCallback jobScheduledCallback, int jobType) {
            generateJobID();
            mJobScheduledCallback = jobScheduledCallback;
            mJobType = jobType;
        }

        /**
         * @param jobId                Application-provided id for this job. Subsequent calls to cancel, or
         *                             jobs created with the same jobId, will update the pre-existing job with
         *                             the same id.
         * @param jobScheduledCallback The endpoint that you implement that will receive the callback from the
         *                             SmartScheduler.
         */
        public Builder(int jobId, SmartScheduler.JobScheduledCallback jobScheduledCallback) {
            mJobScheduledCallback = jobScheduledCallback;
            mJobId = jobId;
        }

        /**
         * @param jobId                Application-provided id for this job. Subsequent calls to cancel, or
         *                             jobs created with the same jobId, will update the pre-existing job with
         *                             the same id.
         * @param jobScheduledCallback The endpoint that you implement that will receive the callback from the
         *                             SmartScheduler.
         * @param jobType              Type of Job to be scheduled
         */
        public Builder(int jobId, SmartScheduler.JobScheduledCallback jobScheduledCallback, int jobType) {
            mJobScheduledCallback = jobScheduledCallback;
            mJobType = jobType;
            mJobId = jobId;
        }

        /**
         * Set some description of the kind of network type your job needs to have.
         * Not calling this function means the network is not necessary, as the default is
         * {@link Job.NetworkType#NETWORK_TYPE_ANY}.
         * Bear in mind that calling this function defines network as a strict requirement for your
         * job. If the network requested is not available your job will never run.
         *
         * @param networkType NetworkType to be set for the job.
         * @return Returns the Builder class for currently configured Job params
         */
        public Builder setRequiredNetworkType(int networkType) {
            mNetworkType = networkType;
            return this;
        }

        /**
         * Specify that this job should happen only once after the provided interval has elapsed.
         *
         * @param intervalMillis Millisecond interval after which this job has to be performed.
         * @return Returns the Builder class for currently configured Job params
         */
        public Builder setIntervalMillis(long intervalMillis) {
            mIsPeriodic = false;
            mIntervalMillis = intervalMillis;
            return this;
        }

        /**
         * Specify that how close to the end of the period should this job be executed
         * in a recur with the provided interval, not more than once per period.
         *
         * @param flexInMillis Millisecond interval for which this job will repeat.
         * @return Returns the Builder class for currently configured Job params
         */
        public Builder setFlex(long flexInMillis) {
            this.mFlexInMillis = flexInMillis;
            return this;
        }

        /**
         * Specify that this job should recur with the provided interval, not more than once per
         * period.
         *
         * @param intervalMillis Millisecond interval for which this job will repeat.
         * @return Returns the Builder class for currently configured Job params
         */
        public Builder setPeriodic(long intervalMillis) {
            mIsPeriodic = true;
            mIntervalMillis = intervalMillis;
            mInitialDelayInMillis = intervalMillis;
            return this;
        }

        /**
         * Specify that this job should recur with the provided interval, not more than once per
         * period.
         *
         * @param intervalMillis       Millisecond interval for which this job will repeat.
         * @param initialDelayInMillis Initial Delay (in millis) for the job's first occurrence.
         * @return Returns the Builder class for currently configured Job params
         */
        public Builder setPeriodic(long intervalMillis, long initialDelayInMillis) {
            mIsPeriodic = true;
            mIntervalMillis = intervalMillis;
            mInitialDelayInMillis = initialDelayInMillis;
            return this;
        }

        /**
         * @return The job object to hand to the SmartScheduler. This object is immutable.
         */
        public Job build() {
            if (mJobType == Job.Type.JOB_TYPE_NONE) {

                // Schedule via Handlers if mIntervalMillis is less than JOB_TYPE_HANDLER_THRESHOLD
                if (mIntervalMillis < JOB_TYPE_HANDLER_THRESHOLD) {
                    mJobType = Job.Type.JOB_TYPE_HANDLER;
                } else {
                    mJobType = Job.Type.JOB_TYPE_ALARM;
                }
            }

            return new Job(this);
        }
    }

    @Override
    public String toString() {
        return "Job{" +
                "jobId=" + jobId +
                ", jobType=" + jobType +
                ", jobScheduledCallback=" + (jobScheduledCallback != null ? jobScheduledCallback : " null") +
                ", networkType=" + networkType +
                ", isPeriodic=" + isPeriodic +
                ", intervalMillis=" + intervalMillis +
                ", initialDelayInMillis=" + initialDelayInMillis +
                ", flexInMillis=" + (flexInMillis != null ? flexInMillis : " null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Job job = (Job) o;

        if (jobId != job.jobId) return false;
        if (jobType != job.jobType) return false;
        if (networkType != job.networkType) return false;
        if (isPeriodic != job.isPeriodic) return false;
        if (intervalMillis != job.intervalMillis) return false;
        if (initialDelayInMillis != job.initialDelayInMillis) return false;
        if (!jobScheduledCallback.equals(job.jobScheduledCallback)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = jobId;
        result = 31 * result + jobType;
        result = 31 * result + jobScheduledCallback.hashCode();
        result = 31 * result + networkType;
        result = 31 * result + (isPeriodic ? 1 : 0);
        result = 31 * result + (int) (intervalMillis ^ (intervalMillis >>> 32));
        result = 31 * result + (int) (initialDelayInMillis ^ (initialDelayInMillis >>> 32));
        return result;
    }
}
