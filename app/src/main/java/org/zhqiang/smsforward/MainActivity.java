package org.zhqiang.smsforward;

import static android.provider.Telephony.Sms.Intents.getMessagesFromIntent;
import static org.zhqiang.smsforward.Constants.ACTION_SETTINGS;
import static org.zhqiang.smsforward.Constants.KEY_GMAIL_ACCOUNT;
import static org.zhqiang.smsforward.Constants.KEY_GMAIL_PASSWORD;
import static org.zhqiang.smsforward.Constants.START_ACTIVITY_SETTINGS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    private static final String TAG = "MainActivity";

    private final SmsManager smsManager = SmsManager.getDefault();
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch activeSwitch;
    // UI related
    private EditText phoneInput;
    private EditText emailInput;
    private Button buttonSettings;
    // phone number to forward sms
    private String receiverPhone = null;
    // email address to forward sms
    private String receiverEmail = null;
    private String gmailAccount = null;
    private String gmailPassword = null;
    // is it active?
    private boolean active = false;
    private GMailSender gMailSender;

    private void loadSettings() {
        Log.i(TAG, "Loading setting");
        SharedPreferences preference = getPreferences(Context.MODE_PRIVATE);
        receiverPhone = preference.getString(Constants.KEY_RECEIVER_PHONE, receiverPhone);
        receiverEmail = preference.getString(Constants.KEY_RECEIVER_EMAIL, receiverEmail);
        gmailAccount = preference.getString(Constants.KEY_GMAIL_ACCOUNT, gmailAccount);
        gmailPassword = preference.getString(Constants.KEY_GMAIL_PASSWORD, gmailPassword);
        active = preference.getBoolean(Constants.KEY_ACTIVE, active);
        Log.i(TAG, String.format("Loaded setting %s %s %b", receiverPhone, receiverEmail, active));
    }

    private void saveSettings() {
        SharedPreferences preference = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();
        editor.putString(Constants.KEY_RECEIVER_PHONE, receiverPhone);
        editor.putString(Constants.KEY_RECEIVER_EMAIL, receiverEmail);
        editor.putString(Constants.KEY_GMAIL_ACCOUNT, gmailAccount);
        editor.putString(Constants.KEY_GMAIL_PASSWORD, gmailPassword);
        editor.putBoolean(Constants.KEY_ACTIVE, active);
        editor.apply();
        Log.i(TAG, "Saved setting");
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSettings();
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, Constants.PERMISSION_REQUEST_RECEIVE_SMS);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, Constants.PERMISSION_REQUEST_READ_SMS);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, Constants.PERMISSION_REQUEST_SEND_SMS);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, Constants.PERMISSION_REQUEST_INTERNET);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_RECEIVE_SMS:
                Log.i(TAG, "Permission for receiving SMS is granted");
                break;
            case Constants.PERMISSION_REQUEST_READ_SMS:
                Log.i(TAG, "Permission for reading SMS is granted");
                break;
            case Constants.PERMISSION_REQUEST_SEND_SMS:
                Log.i(TAG, "Permission for sending SMS is granted");
                break;
            default:
                Log.e(TAG, "Unknown permission");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != START_ACTIVITY_SETTINGS) {
            Log.i(TAG, "result from unknown activity");
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.i(TAG, "result with bad code");
            return;
        }
        if (data == null) {
            Log.e(TAG, "intent is empty, unexpected");
            return;
        }
        gmailAccount = data.getStringExtra(KEY_GMAIL_ACCOUNT);
        gmailPassword = data.getStringExtra(KEY_GMAIL_PASSWORD);
        gMailSender = new GMailSender(gmailAccount, gmailPassword);
        Log.i(TAG, "update GMailSender");
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
        buttonSettings = findViewById(R.id.buttonSetting);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentSettings = new Intent(ACTION_SETTINGS, null, getApplicationContext(), GMailSettingActivity.class);
                //noinspection deprecation
                startActivityForResult(intentSettings, START_ACTIVITY_SETTINGS);
            }
        });
        loadSettings();
        if (receiverPhone != null) {
            phoneInput.setText(receiverPhone);
        }
        if (receiverEmail != null) {
            emailInput.setText(receiverEmail);
        }
        if (gmailAccount != null || gmailPassword != null) {
            gMailSender = new GMailSender(gmailAccount, gmailPassword);
        } else {
            Log.w(TAG, "email forwarding disabled, please configure email settings first");
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
        if (!Constants.ACTION.equals(action)) {
            return;
        }
        Log.i(TAG, "Starting due to receiving a new message");
        loadSettings();
        if (!active) {
            Log.i(TAG, "Skip forwarding due to not active");
            return;
        }
        SmsMessage[] messages = getMessagesFromIntent(intent);
        for (SmsMessage message : messages) {
            forwardMessage(message);
        }
    }

    private boolean sendSMS(SmsMessage message) {
        if (receiverPhone == null) {
            return false;
        }
        String body = String.format("%s:%s", message.getOriginatingAddress(), message.getMessageBody());
        smsManager.sendTextMessage(receiverPhone, null, body, null, null);
        Log.i(TAG, String.format("Message forwarded to %s", receiverPhone));
        return true;
    }

    private boolean sendEmail(SmsMessage message) {
        if (receiverEmail == null) {
            return false;
        }
        if (gMailSender == null) {
            return false;
        }
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    gMailSender.sendMail(
                            message.getOriginatingAddress(),
                            message.getMessageBody(),
                            String.format("%s@gmail.com", gmailAccount),
                            receiverEmail);
                } catch (Exception e) {
                    Log.e(TAG, String.format("failed to forward email to %s", receiverEmail), e);
                }
            }
        });
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
        switch (view.getId()) {
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