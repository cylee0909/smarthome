package com.babt.smarthome.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by cylee on 16/12/18.
 */
public class EncryptUtil {
    private static final String EXTRA_DATA = "cylee_2016_12_18";
    public static String getVerify(String id) {
        if (id == null) id = "";
        id += EXTRA_DATA;
        return md5(md5(id));
    }


    /**
     * 对字符串进行md5加密
     *
     * @param plainText 要加密的字符串
     * @return 加密后的密文
     */
    public static String md5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte b[] = md.digest(plainText.getBytes());
            StringBuilder buf = new StringBuilder();
            for (int offset = 0; offset < b.length; offset++) {
                int i = (b[offset] & 0xFF) | 0x100;
                buf.append(Integer.toHexString(i).substring(1));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }
}
