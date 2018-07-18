package io.hypertrack.smart_scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

/**
 * Created by piyush on 25/11/16.
 */
public class SmartSchedulerAlarmReceiverService extends JobIntentService {

    private static final String TAG = SmartSchedulerAlarmReceiverService.class.getSimpleName();

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            final Integer jobID = bundle.getInt(SmartScheduler.ALARM_JOB_ID_KEY, -1);

            SmartScheduler jobScheduler = SmartScheduler.getInstance(getApplicationContext());
            if (jobScheduler != null) {
                jobScheduler.onAlarmJobScheduled(jobID);
                return;
            }
        }
    }
}
