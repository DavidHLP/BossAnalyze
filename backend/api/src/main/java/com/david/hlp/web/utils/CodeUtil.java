package com.david.hlp.web.utils;

import java.security.SecureRandom;

/**
 * 随机码工具类
 */
public class CodeUtil {

    private static final String NUMBER_CHARS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成指定位数的随机数字字符串
     *
     * @param length 随机数长度
     * @return 随机数字字符串
     */
    public static String generateRandomNumber(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = RANDOM.nextInt(NUMBER_CHARS.length());
            sb.append(NUMBER_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * 生成6位随机验证码
     *
     * @return 6位随机数字字符串
     */
    public static String generateVerificationCode() {
        return generateRandomNumber(6);
    }

    /**
     * 生成4位随机验证码
     *
     * @return 4位随机数字字符串
     */
    public static String generateShortVerificationCode() {
        return generateRandomNumber(4);
    }
}
