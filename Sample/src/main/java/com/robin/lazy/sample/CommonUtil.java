package com.robin.lazy.sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 常用工具类
 */
public class CommonUtil {
    public static final int NOTIFICATION_REQUEST_CODE = 9527;
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";


    //获取当前版本名称
    public static String getVersionName(Context context) throws Exception {
        // 获取PackageManager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionName;
    }

    //获取当前版本号
    public static Integer getVersionCode(Context context) throws Exception {
        // 获取PackageManager的实例
        PackageManager packageManager = context.getPackageManager();
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        return packInfo.versionCode;
    }

    // 检查权限是否获取（android6.0及以上系统可能默认关闭权限，且没提示）
    @SuppressLint("InlinedApi")
    public static void CheckPermission(PackageManager pm, Context that) {
        boolean permission_internet = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.INTERNET", that.getPackageName()));
        boolean permission_receive_boot = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.ACCESS_WIFI_STATE", that.getPackageName()));
        boolean permission_foreground_service = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.FOREGROUND_SERVICE", that.getPackageName()));
        boolean permission_receive_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.RECEIVE_SMS", that.getPackageName()));
        boolean permission_read_sms = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_SMS", that.getPackageName()));
        boolean permission_read_external_storage = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.READ_EXTERNAL_STORAGE", that.getPackageName()));
        boolean permission_write_external_storage = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.WRITE_EXTERNAL_STORAGE", that.getPackageName()));

        if (!(permission_internet && permission_receive_boot && permission_foreground_service &&
                permission_read_external_storage && permission_write_external_storage &&
                permission_receive_sms && permission_read_sms)) {
            ActivityCompat.requestPermissions((Activity) that, new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
            }, 0x01);
        }
    }

    //计算MD5
    public static String MD5(String input) {
        if (input == null || input.length() == 0) return null;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            byte[] byteArray = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : byteArray) {
                // 一个byte格式化成两位的16进制，不足两位高位补零
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    //屏幕像素转换
    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    //是否合法的url
    public static boolean checkUrl(String urls, boolean emptyResult) {
        if (TextUtils.isEmpty(urls)) return emptyResult;

        String regex = "(ht|f)tp(s?)\\:\\/\\/[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(\\/?)([a-zA-Z0-9\\-\\.\\?\\,\\'\\/\\\\&%\\+\\$#_=]*)?";
        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(urls.trim());
        return mat.matches();
    }

    //焦点位置插入文本
    public static void insertOrReplaceText2Cursor(EditText editText, String str) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

}
