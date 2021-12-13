package org.zhqiang.smsforward;

import static android.provider.Telephony.Sms.Intents.getMessagesFromIntent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "MainActivity";
    private static final String KEY_RECEIVER_PHONE = "receiver_phone";
    private static final String KEY_RECEIVER_EMAIL = "receiver_email";
    private static final String KEY_ACTIVE = "active";

    private static final int PERMISSION_REQUEST_RECEIVE_SMS = 0;
    private static final int PERMISSION_REQUEST_READ_SMS = 1;
    private static final int PERMISSION_REQUEST_SEND_SMS = 2;

    // phone number to forward sms
    private String receiverPhone = null;
    // email address to forward sms
    private String receiverEmail = null;
    // is it active?
    private boolean active = false;
    private SmsManager smsManager = SmsManager.getDefault();

    // UI related
    EditText phoneInput;
    EditText emailInput;
    Switch activeSwitch;

    private void loadSettings() {
        Log.i(TAG, "Loading setting");
        SharedPreferences preference = getPreferences(Context.MODE_PRIVATE);
        receiverPhone = preference.getString(KEY_RECEIVER_PHONE, receiverPhone);
        receiverEmail = preference.getString(KEY_RECEIVER_EMAIL, receiverEmail);
        active = preference.getBoolean(KEY_ACTIVE, active);
        Log.i(TAG, String.format("Loaded setting %s %s %b", receiverPhone, receiverEmail, active));
    }

    private void saveSettings() {
        SharedPreferences preference = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();
        editor.putString(KEY_RECEIVER_PHONE, receiverPhone);
        editor.putString(KEY_RECEIVER_EMAIL, receiverEmail);
        editor.putBoolean(KEY_ACTIVE, active);
        editor.apply();
        Log.i(TAG, "Saved setting");
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSettings();
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, PERMISSION_REQUEST_RECEIVE_SMS);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_READ_SMS);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SEND_SMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_RECEIVE_SMS:
                Log.i(TAG, "Permission for receving SMS is granted");
                break;
            case PERMISSION_REQUEST_READ_SMS:
                Log.i(TAG, "Permission for reading SMS is granted");
                break;
            case PERMISSION_REQUEST_SEND_SMS:
                Log.i(TAG, "Permission for sending SMS is granted");
                break;
            default:
                Log.e(TAG, "Unknown permission");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        phoneInput = findViewById(R.id.editTextPhone);
        phoneInput.setOnFocusChangeListener(this);
        emailInput = findViewById(R.id.editTextTextEmailAddress);
        emailInput.setOnFocusChangeListener(this);
        activeSwitch = findViewById(R.id.switch1);
        activeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                    @Override
                                                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                                        active = compoundButton.isChecked();
                                                        if (active) {
                                                            Log.i(TAG, "Enabled");
                                                        } else {
                                                            Log.i(TAG, "Disabled");
                                                        }
                                                    }
                                                });
        loadSettings();
        if (receiverPhone != null) {
            phoneInput.setText(receiverPhone);
        }
        if (receiverEmail != null) {
            emailInput.setText(receiverEmail);
        }
        activeSwitch.setChecked(active);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (!ACTION.equals(action)) {
            return;
        }
        Log.i(TAG, "Starting due to receiving a new message");
        loadSettings();
        if (!active) {
            Log.i(TAG, "Skip forwarding due to not active");
            return;
        }
        SmsMessage[] messages = getMessagesFromIntent(intent);
        for (SmsMessage message: messages) {
            forwardMessage(message);
        }
    }

    private boolean sendSMS(SmsMessage message) {
        if (receiverPhone == null) {
            return false;
        }
        smsManager.sendTextMessage(receiverPhone, null, message.getMessageBody(), null, null);
        Log.i(TAG, String.format("Message forwarded to %s", receiverPhone));
        return true;
    }

    private boolean sendEmail(SmsMessage message) {
        if (receiverEmail == null) {
            return false;
        }
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setDataAndType(Uri.parse("mailto:"), "text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{receiverEmail});
        emailIntent.putExtra(Intent.EXTRA_TEXT, message.getMessageBody());
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format("forward from:%s", message.getOriginatingAddress()));
        try {
            startActivity(emailIntent);
            Log.i(TAG, String.format("Message forwarded to %s", receiverEmail));
        } catch (android.content.ActivityNotFoundException e) {
            String errorMessage = String.format("Failed to forward message to %s", receiverEmail);
            Log.e(TAG, errorMessage, e);
            return false;
        }
        return true;
    }

    private boolean forwardMessage(SmsMessage message) {
        boolean result = sendSMS(message);
        result |= sendEmail(message);
        if (!result) {
            Log.e(TAG, "Failed to forward message");
        }
        return result;
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        switch(view.getId()) {
            case R.id.editTextPhone:
                receiverPhone = phoneInput.getText().toString();
                Log.i(TAG, String.format("Set phone number %s", receiverPhone));
                break;
            case R.id.editTextTextEmailAddress:
                receiverEmail = emailInput.getText().toString();
                Log.i(TAG, String.format("Set email address %s", receiverEmail));
                break;
            default:
                // Pass
                break;
        }
    }
}