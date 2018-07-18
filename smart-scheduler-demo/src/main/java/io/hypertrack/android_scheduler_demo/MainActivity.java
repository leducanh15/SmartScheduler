package io.hypertrack.android_scheduler_demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import io.hypertrack.smart_scheduler.Job;
import io.hypertrack.smart_scheduler.SmartScheduler;

public class MainActivity extends AppCompatActivity implements SmartScheduler.JobScheduledCallback {

    private static final int JOB_ID = 1;
    private static final String TAG = "DucAnh";

    private Spinner jobTypeSpinner, networkTypeSpinner;
    private Switch requiresChargingSwitch, isPeriodicSwitch;
    private EditText intervalInMillisEditText;
    private Button smartJobButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Views
        jobTypeSpinner = findViewById(R.id.spinnerJobType);
        networkTypeSpinner = findViewById(R.id.spinnerNetworkType);
        isPeriodicSwitch = findViewById(R.id.switchPeriodicJob);
        intervalInMillisEditText = findViewById(R.id.jobInterval);
        smartJobButton = findViewById(R.id.smartJobButton);
    }

    public void onSmartJobBtnClick(View view) {
            SmartScheduler jobScheduler = SmartScheduler.getInstance(this);

        // Check if any periodic job is currently scheduled
        if (jobScheduler.contains(JOB_ID)) {
            removePeriodicJob();
            return;
        }

        // Create a new job with specified params
        Job job = createJob();
        if (job == null) {
            Toast.makeText(MainActivity.this, "Invalid paramteres specified. " +
                    "Please try again with correct job params.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Schedule current created job
        if (jobScheduler.addJob(job)) {
            Toast.makeText(MainActivity.this, "Job successfully added!", Toast.LENGTH_SHORT).show();

            if (job.isPeriodic()) {
                smartJobButton.setText(getString(R.string.remove_job_btn));
            } else {
                smartJobButton.setAlpha(0.5f);
                smartJobButton.setEnabled(false);
            }
        }
    }

    private Job createJob() {
        int jobType = getJobType();
        int networkType = getNetworkTypeForJob();
        boolean isPeriodic = isPeriodicSwitch.isChecked();

        String intervalInMillisString = intervalInMillisEditText.getText().toString();
        if (TextUtils.isEmpty(intervalInMillisString)) {
            return null;
        }

        Long intervalInMillis = Long.parseLong(intervalInMillisString);
        Job.Builder builder = new Job.Builder(JOB_ID, this, jobType)
                .setRequiredNetworkType(networkType)
                .setIntervalMillis(intervalInMillis);

        if (isPeriodic) {
            builder.setPeriodic(intervalInMillis);
        }

        return builder.build();
    }

    private int getJobType() {
        int jobTypeSelectedPos = jobTypeSpinner.getSelectedItemPosition();
        switch (jobTypeSelectedPos) {
            default:
            case 1:
                return Job.Type.JOB_TYPE_HANDLER;
            case 2:
                return Job.Type.JOB_TYPE_ALARM;
        }
    }

    private int getNetworkTypeForJob() {
        int networkTypeSelectedPos = networkTypeSpinner.getSelectedItemPosition();
        switch (networkTypeSelectedPos) {
            default:
            case 0:
                return Job.NetworkType.NETWORK_TYPE_ANY;
            case 1:
                return Job.NetworkType.NETWORK_TYPE_CONNECTED;
            case 2:
                return Job.NetworkType.NETWORK_TYPE_UNMETERED;
        }
    }

    private void removePeriodicJob() {
        smartJobButton.setText(getString(R.string.schedule_job_btn));

        SmartScheduler jobScheduler = SmartScheduler.getInstance(this);
        if (!jobScheduler.contains(JOB_ID)) {
            Toast.makeText(MainActivity.this, "No job exists with JobID: " + JOB_ID, Toast.LENGTH_SHORT).show();
            return;
        }

        if (jobScheduler.removeJob(JOB_ID)) {
            Toast.makeText(MainActivity.this, "Job successfully removed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onJobScheduled(Context context, final Job job) {
        if (job != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Job: " + job.getJobId() + " scheduled!", Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG, "Job: " + job.getJobId() + " scheduled!");

            if (!job.isPeriodic()) {
                smartJobButton.setAlpha(1.0f);
                smartJobButton.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onResetSchedulerClick(MenuItem item) {
        SmartScheduler smartScheduler = SmartScheduler.getInstance(getApplicationContext());
        smartScheduler.removeJob(JOB_ID);

        smartJobButton.setText(getString(R.string.schedule_job_btn));
        smartJobButton.setEnabled(true);
        smartJobButton.setAlpha(1.0f);
    }
}

