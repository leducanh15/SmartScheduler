package io.hypertrack.smart_scheduler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;

import static io.hypertrack.smart_scheduler.Job.JOB_TYPE_HANDLER_THRESHOLD;

/**
 * Created by piyush on 07/10/16.
 */
public class SmartScheduler {

    private static final String TAG = SmartScheduler.class.getSimpleName();
    public static final String ALARM_JOB_ID_KEY = "io.hypertrack.android_scheduler:AlarmJobID";
    public static final String PERIODIC_TASK_JOB_ID_KEY = "io.hypertrack.android_scheduler:PeriodicTaskJobID";

    private static SmartScheduler smartScheduler;

    private Context mContext;
    private HashMap<Integer, Job> scheduledJobs;

    // For Handler type jobs
    private HashMap<Integer, Handler> jobHandlers;
    private HashMap<Integer, Runnable> jobRunnables;

    public static SmartScheduler getInstance(Context context) {
        if (smartScheduler == null) {
            synchronized (SmartScheduler.class) {
                if (smartScheduler == null) {
                    smartScheduler = new SmartScheduler(context);
                }
            }
        }

        return smartScheduler;
    }

    private SmartScheduler(Context context) {
        mContext = context;
        scheduledJobs = new HashMap<>();
        jobHandlers = new HashMap<>();
        jobRunnables = new HashMap<>();
    }

    /**
     * Implement this callback to receive onJobScheduled callback.
     */
    public interface JobScheduledCallback extends Serializable {
        void onJobScheduled(Context context, Job job);
    }

    /**
     * Method to get Job for a given jobID
     *
     * @param jobId JobID for which scheduled job needs to be fetched
     * @return Returns Job object for the given jobID in case one is currently scheduled, null otherwise
     */
    public Job get(int jobId) {
        return scheduledJobs.get(jobId);
    }

    /**
     * Method to check if Job with give JobId exists in the SmartScheduler or not
     *
     * @param jobId JobID for which scheduled job needs to be checked
     * @return Returns true in case a job is currently scheduled with the given jobID, false otherwise
     */
    public boolean contains(int jobId) {
        return scheduledJobs.containsKey(jobId);
    }

    /**
     * Method to check if Job exists in the SmartScheduler or not
     *
     * @param job Job which needs to be checked if it is scheduled or not
     * @return Returns true in case given job is scheduled currently, false otherwise
     */
    public boolean contains(Job job) {
        return scheduledJobs.containsValue(job);
    }

    /**
     * Method to schedule Job based on the specified JobParams
     *
     * @param job Job which needs to be added
     * @return Returns true in case given job was added successfully, false otherwise
     */
    public boolean addJob(Job job) {
        if (job == null || job.getJobId() <= 0 || job.getJobScheduledCallback() == null)
            return false;

        boolean result = false;

        // Remove any currently running jobs
        removeJob(job.getJobId());

        switch (job.getJobType()) {
            case Job.Type.JOB_TYPE_HANDLER:
                result = addHandlerJob(job);
                break;

            case Job.Type.JOB_TYPE_ALARM:
                result = addAlarmJob(job);
                break;

            default:
                Log.e(TAG, "Error occurred while addJob: JobType is INVALID");
                break;
        }

        // Add Job to scheduledJobs if it is successfully scheduled
        if (result) {
            scheduledJobs.put(job.getJobId(), job);
        }

        return result;
    }

    /**
     * Method to remove a job
     *
     * @param jobId Job which needs to be removed
     * @return Returns true in case given job was removed successfully, false otherwise
     */
    public boolean removeJob(int jobId) {

        // Remove Jobs if it exists
        removeHandlerJob(jobId);
        removeAlarmJob(jobId);

        if (scheduledJobs != null && scheduledJobs.get(jobId) != null) {
            scheduledJobs.remove(jobId);
            return true;
        }

        return false;
    }

    /**
     * Method to check if the Job is valid or not
     *
     * @param job Job which needs to be checked to be valid or not
     * @return Returns true in case given job is valid, false otherwise
     */
    private boolean isJobValid(Job job) {
        try {
            if (job != null && scheduledJobs.get(job.getJobId()) != null
                    && (!Utils.checkIfPowerSaverModeEnabled(mContext) ||
                    scheduledJobs.get(job.getJobId()).getJobType() == job.getJobType())
                    && scheduledJobs.get(job.getJobId()).getIntervalMillis() == job.getIntervalMillis()) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while isJobValid: " + e);
            return false;
        }

        return false;
    }

    private void onJobScheduled(Job job) {
        // Check if a valid Job has been passed as param
        if (job == null)
            return;

        // Check if the scheduled Job is valid or not
        if (!isJobValid(job)) {
            // Remove current scheduled Job
            removeJob(job.getJobId());
            return;
        }

        // Check if the scheduled Job meets its net connectivity requirements
        if (job.getNetworkType() == Job.NetworkType.NETWORK_TYPE_CONNECTED && !isConnected())
            return;

        // Check if the scheduled Job meets its net connectivity metering requirements
        if (job.getNetworkType() == Job.NetworkType.NETWORK_TYPE_UNMETERED && !isConnected() && !isConnectionUnMetered())
            return;

        // Schedule the Job as all its requirements are met
        job.getJobScheduledCallback().onJobScheduled(mContext, job);

        // Remove one time Jobs after they have been scheduled
        if (!job.isPeriodic()) {
            removeJob(job.getJobId());
        }
    }

    public void onHandlerJobScheduled(int jobID) {
        if (scheduledJobs != null && scheduledJobs.get(jobID) != null) {
            onJobScheduled(scheduledJobs.get(jobID));
            return;
        }

        // Handler Job is not valid, so remove it
        removeHandlerJob(jobID);
    }

    public void onAlarmJobScheduled(int jobID) {
        if (scheduledJobs != null && scheduledJobs.get(jobID) != null) {
            onJobScheduled(scheduledJobs.get(jobID));
            return;
        }

        // Alarm Job is not valid, so remove it
        removeAlarmJob(jobID);
    }

    public void onPeriodicTaskJobScheduled(String periodicTaskTag, Bundle bundle) {
        try {
            if (bundle == null)
                return;

            int jobID = bundle.getInt(SmartScheduler.PERIODIC_TASK_JOB_ID_KEY);

            if (scheduledJobs != null && scheduledJobs.get(jobID) != null) {
                onJobScheduled(scheduledJobs.get(jobID));
                return;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while onPeriodicTaskJobScheduled: " + e);
        }
    }

    public void onPowerSaverModeChanged(boolean powerSaverModeEnabled) {

        Log.i(TAG, "SmartScheduler onPowerSaverModeChanged: " + powerSaverModeEnabled);
        if (scheduledJobs != null && scheduledJobs.size() > 0) {

            HashMap<Integer, Job> copyScheduledJobs = new HashMap<>();
            copyScheduledJobs.putAll(scheduledJobs);
        }
    }

    private boolean updateJobsOnPowerSaverModeChanged(Job job, boolean powerSaverModeEnabled) {

        // PowerSaverMode Enabled, Reset PeriodicTasks to Handlers/Alarms
        if (powerSaverModeEnabled) {

            // Schedule via handlers if mIntervalMillis is less than JOB_TYPE_HANDLER_THRESHOLD
            if (job.getIntervalMillis() < JOB_TYPE_HANDLER_THRESHOLD) {
                return addHandlerJob(job);

                // Else, schedule via alarms
            } else {
                return addAlarmJob(job);
            }
        }

        return addHandlerJob(job);
    }

    // Methods to add jobs
    private boolean addHandlerJob(final Job job) {
        if (job == null)
            return false;

        final int jobID = job.getJobId();

        // Remove any existing Handler Job with current jobID
        removeHandlerJob(jobID);

        // Add Handler & Runnable instance
        jobRunnables.put(jobID, new Runnable() {
            @Override
            public void run() {
                try {
                    // Call onJobScheduled method
                    onHandlerJobScheduled(job.getJobId());

                    if (!isJobValid(job)) {
                        return;
                    }

                    // For Periodic Jobs, Schedule Job for the next time
                    if (job.isPeriodic() && jobHandlers.get(jobID) != null && jobRunnables.get(jobID) != null) {
                        jobHandlers.get(jobID)
                                .postDelayed(jobRunnables.get(jobID), job.getIntervalMillis());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception occurred while HandlerTypeJob.onRun(): " + e);
                }
            }
        });

        // Initialize JobHandler
        initializeJobHandler(jobID);

        // Schedule the first instance for the job
        long intervalInMillis = job.isPeriodic() ? job.getInitialDelayInMillis() : job.getIntervalMillis();
        jobHandlers.get(jobID).postDelayed(jobRunnables.get(jobID), intervalInMillis);
        return true;
    }

    private void initializeJobHandler(int jobID) {
        if (jobHandlers.get(jobID) == null) {
            synchronized (SmartScheduler.class) {

                if (jobHandlers.get(jobID) == null) {
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }

                    jobHandlers.put(jobID, new Handler(Looper.myLooper()));
                }
            }
        }
    }

    private boolean addAlarmJob(Job job) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt(SmartScheduler.ALARM_JOB_ID_KEY, job.getJobId());

            Intent intent = new Intent(mContext, SmartSchedulerAlarmReceiver.class);
            intent.putExtras(bundle);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, job.getJobId(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Calendar calendar = Calendar.getInstance();

            //Set the alarm for the first time and update the same in SharedPreferences
            AlarmManager alarm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

            if (job.isPeriodic()) {
                alarm.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        job.getIntervalMillis(),
                        pendingIntent);

            } else {
                long triggerInMillis = calendar.getTimeInMillis() + job.getIntervalMillis();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerInMillis, pendingIntent);

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, triggerInMillis, pendingIntent);

                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, triggerInMillis, pendingIntent);
                }
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while addAlarmJob: " + e);
            return false;
        }
    }

    // Methods to remove jobs
    private boolean removeHandlerJob(final int jobID) {
        try {
            // Clear any existing Handlers & Runnables
            if (jobHandlers.get(jobID) != null) {
                jobHandlers.get(jobID).removeCallbacksAndMessages(null);
            }

            jobRunnables.remove(jobID);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while removeHandlerJob: " + e);
            return false;
        }
    }


    private boolean removeAlarmJob(int jobID) {
        try {
            //removing existing alarm
            Intent intent = new Intent(mContext, SmartSchedulerAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, jobID, intent, 0);

            //check if any alarm is set or no, if yes then remove
            if (pendingIntent != null) {
                AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while removeAlarmJob: " + e);
            return false;
        }
    }

    /**
     * Method to check if the device is charging.
     *
     * @return Returns true if device is either charging or full, false otherwise
     */
    private boolean isCharging() {
        try {
            Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int batteryPowerStatus = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);

                switch (batteryPowerStatus) {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                    case BatteryManager.BATTERY_STATUS_FULL:
                        return true;

                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    default:
                        return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while isCharging: " + e);
        }

        return false;
    }

    /**
     * Method to check if the device is connected to network.
     *
     * @return Returns true if device has net connectivity, false otherwise
     */
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Method to check if the device is connected to an un-metered network like WiFi etc.
     *
     * @return Returns true if device is connected to un-metered network, false otherwise
     */
    private boolean isConnectionUnMetered() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Checks if the device is on a metered network
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return !cm.isActiveNetworkMetered();
        } else {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
        }
    }
}
