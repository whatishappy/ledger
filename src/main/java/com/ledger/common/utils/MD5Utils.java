package com.ledger.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MD5 工具类（用于幂等 Key 生成）
 */
public final class MD5Utils {

    private MD5Utils() {
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hex[i * 2] = HEX_CHARS[v >>> 4];
                hex[i * 2 + 1] = HEX_CHARS[v & 0x0F];
            }
            return new String(hex);
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }
    }
}
