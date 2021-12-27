package org.zhqiang.smsforward;

import static android.widget.Toast.LENGTH_SHORT;
import static org.zhqiang.smsforward.Constants.KEY_GMAIL_ACCOUNT;
import static org.zhqiang.smsforward.Constants.KEY_GMAIL_PASSWORD;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;


public class GMailSettingActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "GMailSettingActivity";
    private EditText inputAccount;
    private EditText inputPassword;
    private Button cancel;
    private Button ok;
    private Button test;
    private TextView faq;
    private float faqTextSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gmail_setting);
        inputAccount = findViewById(R.id.editTextAccount);
        inputPassword = findViewById(R.id.editTextPassword);
        cancel = findViewById(R.id.buttonCancel);
        ok = findViewById(R.id.buttonOK);
        test = findViewById(R.id.buttonTest);
        cancel.setOnClickListener(this);
        ok.setOnClickListener(this);
        test.setOnClickListener(this);
        faq = findViewById(R.id.textViewFaq);
        faqTextSize = faq.getTextSize();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonCancel:
                onCancel(view);
                Log.d(TAG, "press cancel");
                break;
            case R.id.buttonOK:
                onOK(view);
                Log.d(TAG, "press ok");
                break;
            case R.id.buttonTest:
                onTest(view);
                Log.d(TAG, "press test");
                break;
            default:
                Log.e(TAG, "unknown button is clicked");
        }
    }

    private void onOK(View view) {
        Intent data = new Intent();
        data.putExtra(KEY_GMAIL_PASSWORD, inputPassword.getText().toString());
        data.putExtra(KEY_GMAIL_ACCOUNT, inputAccount.getText().toString());
        setResult(RESULT_OK, data);
        finish();
    }

    private void onCancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void onTest(View view) {
        String account = inputAccount.getText().toString();
        String password = inputPassword.getText().toString();
        String email = String.format("%s@gmail.com", account);
        GMailSender sender = new GMailSender(account, password);
        test.setText("Testing");
        test.setEnabled(false);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sender.sendMail("test email from SMSForward", "this is test email from SMSForward", email, email);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            faq.setTextSize(faqTextSize);
                            ok.setEnabled(true);
                            Toast.makeText(getApplicationContext(), "sending email succeeded, press OK to save and quit", LENGTH_SHORT);
                            Log.i(TAG, "testing GmailSender succeeded");
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "failed to sending email, check again", LENGTH_SHORT);
                            // Enlarge the text
                            faq.setTextSize(faqTextSize * 1.2f);
                            ok.setEnabled(false);
                            Log.i(TAG, "testing GmailSender failed", e);
                        }
                    });
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            test.setText("Test");
                            test.setEnabled(true);
                        }
                    });
                }
            }
        });
    }
}