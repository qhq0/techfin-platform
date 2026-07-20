package com.ccb.techfin.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具类。
 * 用于解密前端请求头 Authorization: Bearer <encrypted-token>。
 * token 明文格式：8 位用户编号 + key。
 */
public final class AesUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    private AesUtils() {
    }

    /**
     * AES 解密，返回明文。
     *
     * @param encrypted Base64 编码的密文
     * @param key       密钥（长度须为 16/24/32 字节）
     * @return 解密后的明文字符串
     */
    public static String decrypt(String encrypted, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("AES 解密失败", e);
        }
    }
}