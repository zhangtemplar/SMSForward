package org.zhqiang.smsforward;

public class Constants {
    static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    static final String KEY_RECEIVER_PHONE = "receiver_phone";
    static final String KEY_RECEIVER_EMAIL = "receiver_email";
    static final String KEY_GMAIL_ACCOUNT = "gmail_account";
    static final String KEY_GMAIL_PASSWORD = "gmail_password";
    static final String KEY_ACTIVE = "active";
    static final int PERMISSION_REQUEST_RECEIVE_SMS = 0;
    static final int PERMISSION_REQUEST_READ_SMS = 1;
    static final int PERMISSION_REQUEST_SEND_SMS = 2;
    static final int PERMISSION_REQUEST_INTERNET = 3;
    static final int START_ACTIVITY_SETTINGS = 123;
    static final String ACTION_SETTINGS = "org.zhqiang.smsforward.GMailSettingActivity";
}
