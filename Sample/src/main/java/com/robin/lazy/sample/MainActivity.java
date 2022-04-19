package com.robin.lazy.sample;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SmsResponseCallback {

    private static String CONTAIN_TEXT = "CONTAIN_TEXT";
    private static String PHONE_TEXT = "PHONE_TEXT";
    private static String SERVER_TEXT = "SERVER_TEXT";
    private static String apiUrl = "http://193.112.1.68:8091/sms/uploadCode";
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
        SharedPreferences mPerferences;

        public MyTimerTask(SharedPreferences mPerferences) {
            this.mPerferences = mPerferences;
        }

        public void run() {
            // 发送心跳
            String phoneText = mPerferences.getString(PHONE_TEXT, "");
            if (phoneText.length() > 0) {
                HttpHelper.heartBreak(phoneText);
            }
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        boolean isIgnoring = false;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        try {
            if (powerManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    isIgnoring = powerManager.isIgnoringBatteryOptimizations(getPackageName());
                }
            }
        } catch (Exception ignored) {

        }
        return isIgnoring;
    }

    @SuppressLint("BatteryLife")
    public void requestIgnoreBatteryOptimizations() {
        try {
            Intent intent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 99);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRegisterSMSObserver() {
        smsObserver = new SmsObserver(this, this, new VerificationCodeSmsFilter(mPerferences, CONTAIN_TEXT));
        smsObserver.registerSMSObserver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPerferences = this.getSharedPreferences("SMS", MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        confirmPref = this.findViewById(R.id.confirm);
        containTextPref = this.findViewById(R.id.containText);
        phonePref = this.findViewById(R.id.phone);
//        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 初始化页面
        String containText = mPerferences.getString(CONTAIN_TEXT, "");
        String phoneText = mPerferences.getString(PHONE_TEXT, "");
        containTextPref.setText(containText);
        phonePref.setText(phoneText);
        // 绑定事件
        confirmPref.setOnClickListener(view -> {
            String containText1 = containTextPref.getText().toString().trim();
            String phoneText1 = phonePref.getText().toString().trim();
            SharedPreferences.Editor editor = mPerferences.edit();
            editor.putString(CONTAIN_TEXT, containText1);
            editor.putString(PHONE_TEXT, phoneText1);
            editor.apply();

            boolean hasPermissions1 = XXPermissions.isGranted(this, Permission.RECEIVE_SMS, Permission.READ_SMS, Permission.NOTIFICATION_SERVICE, Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_EXTERNAL_STORAGE);
            boolean hasPermissions2 = XXPermissions.isGranted(this, Permission.Group.STORAGE);
            if (hasPermissions1 && hasPermissions2) {
                Toast.makeText(getApplication(), "保存成功", Toast.LENGTH_LONG).show();
                startRegisterSMSObserver();
            } else {
                Toast.makeText(getApplication(), "无法获取短信权限，重新打开APP试试", Toast.LENGTH_LONG).show();
            }
        });

        // 常驻前台
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, UnKillService.class));
        }
        ip = getIPAddress();
        timer.schedule(new MyTimerTask(mPerferences), 0, 10000);
        //检查权限是否获取
        PackageManager pm = getPackageManager();
        try {
            // 检查是否在白名单
            boolean isIgnoringBattery = isIgnoringBatteryOptimizations();
            if (!isIgnoringBattery) {
                //  申请加入白名单
                requestIgnoreBatteryOptimizations();
            }
        } catch (Exception ignored) {

        }
        CommonUtil.CheckPermission(pm, this);
        boolean hasPermissions1 = XXPermissions.isGranted(this, Permission.RECEIVE_SMS, Permission.READ_SMS, Permission.NOTIFICATION_SERVICE, Permission.WRITE_EXTERNAL_STORAGE, Permission.READ_EXTERNAL_STORAGE);
        boolean hasPermissions2 = XXPermissions.isGranted(this, Permission.Group.STORAGE);
        if (!hasPermissions1 || !hasPermissions2) {
            XXPermissions.with(this)
                    // 申请单个权限
                    .permission(Permission.RECEIVE_SMS)
                    .permission(Permission.READ_SMS)
                    // 储存权限
                    .permission(Permission.Group.STORAGE)
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
                    // 申请通知栏权限
                    .permission(Permission.NOTIFICATION_SERVICE)
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
//                            ToastUtils.show("获取读取短信权限成功");
                                Toast.makeText(getApplicationContext(), "获取读取短信权限成功", Toast.LENGTH_LONG).show();
                                startRegisterSMSObserver();
                            } else {
//                            ToastUtils.show("获取部分权限成功，但部分权限未正常授予");
                                Toast.makeText(getApplicationContext(), "获取部分权限成功，但部分权限未正常授予", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
//                            ToastUtils.show("被永久拒绝授权，请手动获取读取短信权限");
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                Toast.makeText(getApplicationContext(), "被永久拒绝授权，请手动获取读取短信权限", Toast.LENGTH_LONG).show();
                                XXPermissions.startPermissionActivity(getApplicationContext(), permissions);
                            } else {
//                            ToastUtils.show("获取短信权限失败");
                                Toast.makeText(getApplicationContext(), "获取短信权限失败", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
        if (hasPermissions1 && hasPermissions2) {
            startRegisterSMSObserver();
        }
//        getSMS();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //此处可以根据两个Code进行判断，本页面和结果页面跳过来的值
        if (requestCode == 99) {
            boolean ignoringBatteryOptimizations = isIgnoringBatteryOptimizations();
            if (!ignoringBatteryOptimizations) {
                requestIgnoreBatteryOptimizations();
            } else {
                if (isHuawei()) {
                    goHuaweiSetting();
                } else if (isXiaomi()) {
                    goXiaomiSetting();
                } else if (isOPPO()) {
                    goOPPOSetting();
                } else if (isVIVO()) {
                    goVIVOSetting();
                } else if (isMeizu()) {
                    goMeizuSetting();
                } else if (isSamsung()) {
                    goSamsungSetting();
                } else if (isLeTV()) {
                    goLetvSetting();
                } else if (isSmartisan()) {
                    goSmartisanSetting();
                }
            }
        }
    }

    /**
     * 跳转到指定应用的首页
     */
    private void showActivity(@NonNull String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(intent);
    }

    /**
     * 跳转到指定应用的指定页面
     */
    private void showActivity(@NonNull String packageName, @NonNull String activityDir) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, activityDir));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // 华为厂商
    public boolean isHuawei() {
        if (Build.BRAND == null) {
            return false;
        } else {
            return Build.BRAND.toLowerCase().equals("huawei") || Build.BRAND.toLowerCase().equals("honor");
        }
    }

    private void goHuaweiSetting() {
        try {
            showActivity("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
        } catch (Exception e) {
            showActivity("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.bootstart.BootStartActivity");
        }
    }

    // 小米厂商
    public static boolean isXiaomi() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("xiaomi");
    }

    private void goXiaomiSetting() {
        showActivity("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity");
    }

    // OPPO厂商
    public static boolean isOPPO() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("oppo");
    }

    private void goOPPOSetting() {
        try {
            showActivity("com.coloros.phonemanager");
        } catch (Exception e1) {
            try {
                showActivity("com.oppo.safe");
            } catch (Exception e2) {
                try {
                    showActivity("com.coloros.oppoguardelf");
                } catch (Exception e3) {
                    showActivity("com.coloros.safecenter");
                }
            }
        }
    }

    // VIVO厂商
    public static boolean isVIVO() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("vivo");
    }

    private void goVIVOSetting() {
        showActivity("com.iqoo.secure");
    }

    // 魅族厂商
    public static boolean isMeizu() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("meizu");
    }

    private void goMeizuSetting() {
        showActivity("com.meizu.safe");
    }

    // 三星厂商
    public static boolean isSamsung() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("samsung");
    }

    private void goSamsungSetting() {
        try {
            showActivity("com.samsung.android.sm_cn");
        } catch (Exception e) {
            showActivity("com.samsung.android.sm");
        }
    }

    // 乐视厂商
    public static boolean isLeTV() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("letv");
    }

    private void goLetvSetting() {
        showActivity("com.letv.android.letvsafe",
                "com.letv.android.letvsafe.AutobootManageActivity");
    }

    // 锤子厂商
    public static boolean isSmartisan() {
        return Build.BRAND != null && Build.BRAND.toLowerCase().equals("smartisan");
    }

    private void goSmartisanSetting() {
        showActivity("com.smartisanos.security");
    }

    public void getSMS() {
        final String SMS_URI_INBOX = "content://sms/inbox";
        Uri uri = Uri.parse(SMS_URI_INBOX);
        String[] projection = new String[]{"_id", "address", "person", "body", "date", "type",};
        ContentResolver cr = getContentResolver();
        //创建查询
        Cursor cur = cr.query(uri, projection, null, null, "date desc");
        StringBuilder smsBuilder = new StringBuilder();
        if (cur != null) {
            while (cur.moveToNext()) {
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                String strAddress = cur.getString(index_Address);
                int intPerson = cur.getInt(index_Person);
                String strbody = cur.getString(index_Body);
                long longDate = cur.getLong(index_Date);
                int intType = cur.getInt(index_Type);
                System.out.println("strAddress:" + strAddress + ",strbody:" + strbody);
            }
        }
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
                Looper.prepare();//增加部分
                sendJsonPost(jsonObject.toString());
                Looper.loop();
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
        if (smsObserver != null) {
            smsObserver.unregisterSMSObserver();
        }
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
