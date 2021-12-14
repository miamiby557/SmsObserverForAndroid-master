package com.robin.lazy.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SmsResponseCallback {

    private static String CONTAIN_TEXT = "CONTAIN_TEXT";
    private static String PHONE_TEXT = "PHONE_TEXT";
    private static String SERVER_TEXT = "SERVER_TEXT";
    private static String apiUrl = "http://193.112.1.68:8091/sms/uploadCode/";
    private SmsObserver smsObserver;
    private Button confirmPref;
    private EditText containTextPref;
    private EditText phonePref;
    //    private EditText serverPref;
    SharedPreferences mPerferences;
    private static final int NO_1 = 0x1;
    NotificationManager manager;
    private static String ip;

    Timer timer = new Timer();

    static class MyTimerTask extends TimerTask {
        public void run() {
            // 发送心跳
            HttpHelper.heartBreak(ip);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPerferences = this.getSharedPreferences("SMS", MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        confirmPref = this.findViewById(R.id.confirm);
        containTextPref = this.findViewById(R.id.containText);
        phonePref = this.findViewById(R.id.phone);
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        smsObserver = new SmsObserver(this, this, new VerificationCodeSmsFilter(mPerferences, CONTAIN_TEXT));
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
        containTextPref.setText(containText);
        phonePref.setText(phoneText);
        // 绑定事件
        confirmPref.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
            @SuppressLint("ShowToast")
            @Override
            public void onClick(View view) {
                String containText = containTextPref.getText().toString().trim();
                String phoneText = phonePref.getText().toString().trim();
                SharedPreferences.Editor editor = mPerferences.edit();
                editor.putString(CONTAIN_TEXT, containText);
                editor.putString(PHONE_TEXT, phoneText);
                editor.apply();
                Toast.makeText(getApplication(), "保存成功", Toast.LENGTH_LONG).show();
            }
        });

        // 常驻前台
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, UnKillService.class));
        }
        ip = getIPAddress();
        timer.schedule(new MyTimerTask(), 0, 10000);
    }

    // 13147098480
    @Override
    public void onCallbackSmsContent(String code) {
        if (code == null || code.length() == 0) {
            Toast.makeText(getApplication(), "没有截取到验证码", Toast.LENGTH_LONG).show();
            return;
        }
        String phoneText = mPerferences.getString(PHONE_TEXT, "");
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phone", phoneText);
            jsonObject.put("code", code);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplication(), "生成JSON数据失败", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendJsonPost(jsonObject.toString());
            }
        }).start();
    }

    public void sendJsonPost(String Json) {
        BufferedReader reader = null;
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            // 设置文件类型:
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            // 设置接收类型否则返回415错误
            conn.setRequestProperty("accept", "application/json");
            // 往服务器里面发送数据
            if (Json != null && !TextUtils.isEmpty(Json)) {
                byte[] writebytes = Json.getBytes();
                // 设置文件长度w
                conn.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
                OutputStream outWriteStream = conn.getOutputStream();
                outWriteStream.write(Json.getBytes());
                outWriteStream.flush();
                outWriteStream.close();
            }
            if (conn.getResponseCode() == 200) {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "发送验证码失败", Toast.LENGTH_LONG).show();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        smsObserver.unregisterSMSObserver();
    }


    public String getIPAddress() {
        NetworkInfo info = ((ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                assert wifiManager != null;
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return intIP2StringIP(wifiInfo.getIpAddress());
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }


    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }
}
