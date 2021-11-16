/*
 * 文 件 名:  CerificationCode.java
 * 版    权:  Technologies Co., Ltd. Copyright YYYY-YYYY,  All rights reserved
 * 描    述:  <描述>
 * 修 改 人:  江钰锋 00501
 * 修改时间:  16/6/2
 * 跟踪单号:  <跟踪单号>
 * 修改单号:  <修改单号>
 * 修改内容:  <修改内容>
 */

package com.robin.lazy.sms;

import android.content.SharedPreferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 短信验证码过滤器
 */
public class VerificationCodeSmsFilter implements SmsFilter {
    private SharedPreferences mPerferences;
    private String tag;

    /**
     * 需要过滤的发短信的人
     */
    public VerificationCodeSmsFilter(SharedPreferences mPerferences, String tag) {
        this.mPerferences = mPerferences;
        this.tag = tag;
    }

    @Override
    public String filter(String address, String smsContent) {
        String containText = mPerferences.getString(tag, "");
        boolean containTextTag = false;
        String[] strings = containText.split("[,，]");
        if (strings.length == 0) {
            return null;
        }
        for (String text : strings) {
            containTextTag = smsContent.contains(text);
            if (containTextTag) {
                break;
            }
        }
        if (!containTextTag) {
            return null;
        }
        // 深圳联通
        smsContent = smsContent.replace("SZCW0018", "");
        if (smsContent.contains("验证码")) {
            smsContent = smsContent.split("验证码")[1];
        }
        Pattern pattern = Pattern.compile("(\\d{4,6})");//匹配4-8位的数字
        Matcher matcher = pattern.matcher(smsContent);
        if (matcher.find()) {
            return matcher.group();
        }
        /*
        if(smsContent.contains("登录cBSS系统")){
            strings = smsContent.split("验证码");
            Pattern pattern = Pattern.compile("(\\d{4,6})");//匹配4-8位的数字
            Matcher matcher = pattern.matcher(strings[1]);
            if (matcher.find()) {
                return matcher.group();
            }
        }else{
            Pattern pattern = Pattern.compile("[a-zA-Z0-9]{4,6}");//匹配4-6位的数字或者字母
            Matcher matcher = pattern.matcher(smsContent);
            if (matcher.find()) {
                return matcher.group();
            }
        }*/
        return null;
    }
}
