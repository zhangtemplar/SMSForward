package org.zhqiang.smsforward;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class SMSMonitor extends BroadcastReceiver {
    private static final String TAG = "SMSMonitor";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (!Constants.ACTION.equals(action)) {
            return;
        }
        // Start the activity
        Log.d(TAG, "SMS message received.");
        Intent intentOpen = new Intent(action, null, context, MainActivity.class);
        intentOpen.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intentOpen.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
        intentOpen.putExtras(intent.getExtras());
        context.startActivity(intentOpen);
        Log.d(TAG, "Starting main activity.");
    }
}
