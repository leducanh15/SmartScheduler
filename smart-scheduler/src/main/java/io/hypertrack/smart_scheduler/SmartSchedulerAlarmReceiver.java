package io.hypertrack.smart_scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by piyush on 25/11/16.
 */
public class SmartSchedulerAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = SmartSchedulerAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
      /*  Intent onAlarmReceiverServiceIntent = new Intent(context, SmartSchedulerAlarmReceiverService.class);
        onAlarmReceiverServiceIntent.putExtras(intent.getExtras());
        context.startService(onAlarmReceiverServiceIntent);*/
        Log.d(TAG, "onReceive: ");
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            final Integer jobID = bundle.getInt(SmartScheduler.ALARM_JOB_ID_KEY, -1);

            SmartScheduler jobScheduler = SmartScheduler.getInstance(context.getApplicationContext());
            if (jobScheduler != null) {
                jobScheduler.onAlarmJobScheduled(jobID);
                return;
            }
        }
    }
}