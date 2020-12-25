package com.robin.lazy.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.robin.lazy.sms.SmsObserver;
import com.robin.lazy.sms.SmsResponseCallback;
import com.robin.lazy.sms.VerificationCodeSmsFilter;

public class MainActivity extends AppCompatActivity implements SmsResponseCallback {

    private static String CONTAIN_TEXT = "CONTAIN_TEXT";
    private static String PHONE_TEXT = "PHONE_TEXT";
    private static String SERVER_TEXT = "SERVER_TEXT";
    private SmsObserver smsObserver;
    private Button confirmPref;
    private EditText containTextPref;
    private EditText phonePref;
    private EditText serverPref;
    SharedPreferences mPerferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPerferences = this.getSharedPreferences("SMS",MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        confirmPref = this.findViewById(R.id.confirm);
        containTextPref = this.findViewById(R.id.containText);
        phonePref = this.findViewById(R.id.phone);
        serverPref = this.findViewById(R.id.server);
        smsObserver = new SmsObserver(this, this, new VerificationCodeSmsFilter(mPerferences,CONTAIN_TEXT));
        smsObserver.registerSMSObserver();
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_SMS)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {/* ... */}

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {/* ... */}

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                }).check();
        // 初始化页面
        String containText = mPerferences.getString(CONTAIN_TEXT, "");
        String phoneText = mPerferences.getString(PHONE_TEXT, "");
        String serverText = mPerferences.getString(SERVER_TEXT, "");
        containTextPref.setText(containText);
        phonePref.setText(phoneText);
        serverPref.setText(serverText);
        // 绑定事件
        confirmPref.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onClick(View view) {
                String containText = containTextPref.getText().toString().trim();
                String phoneText = phonePref.getText().toString().trim();
                String serverText = serverPref.getText().toString().trim();
                SharedPreferences.Editor editor = mPerferences.edit();
                editor.putString(CONTAIN_TEXT, containText);
                editor.putString(PHONE_TEXT, phoneText);
                editor.putString(SERVER_TEXT, serverText);
                editor.apply();
                Toast.makeText(getApplication(),"保存成功",Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCallbackSmsContent(String code) {
        Toast.makeText(getApplication(),"验证码:"+code,Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        smsObserver.unregisterSMSObserver();
    }
}
