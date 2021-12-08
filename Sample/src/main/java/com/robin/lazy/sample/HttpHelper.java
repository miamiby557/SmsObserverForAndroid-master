package com.robin.lazy.sample;

import android.annotation.SuppressLint;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpHelper {

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
    public static void heartBreak() {
        try {
            String apiUrl = "http://47.103.93.177:8000/monitor/app/updatemonitorstatus";
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
            conn.setRequestProperty("accept", "application/json");
            // 往服务器里面发送数据
            // 设置文件长度
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("chatid", "liantong-app-chat");
            jsonObject.put("ip", "127.0.0.1");
            jsonObject.put("times", format.format(new Date()));
            jsonObject.put("chattype", 3);
            jsonObject.put("nickname", "联通验证码APP");
            byte[] writebytes = jsonObject.toString().getBytes();
            conn.setRequestProperty("Content-Length", String.valueOf(writebytes.length));
            OutputStream outwritestream = conn.getOutputStream();
            outwritestream.write(writebytes);
            outwritestream.flush();
            outwritestream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
