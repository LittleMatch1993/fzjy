package com.cisdi.transaction.config.utils;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import javax.validation.ValidationException;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

public class CreditCodeUtil {
    private static final String baseCode = "0123456789ABCDEFGHJKLMNPQRTUWXY";
    private static final char[] baseCodeArray = baseCode.toCharArray();
    private static final int[] wi = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};

    /**
     * 生成供较验使用的 Code Map
     *
     * @return BidiMap
     */
    private static BidiMap generateCodes() {
        BidiMap codes = new TreeBidiMap();
        for (int i = 0; i < baseCode.length(); i++) {
            codes.put(baseCodeArray[i], i);
        }

        return codes;
    }

    /**
     * 较验社会统一信用代码
     *
     * @param unifiedCreditCode 统一社会信息代码
     * @return 符合: true
     */
//    public static boolean validateUnifiedCreditCode(String unifiedCreditCode) {
//        if (("".equals(unifiedCreditCode)) || unifiedCreditCode.length() != 18) {
//            return false;
//        }
//
//        Map<Character, Integer> codes = generateCodes();
//        int parityBit;
//        try {
//            parityBit = getParityBit(unifiedCreditCode, codes);
//        } catch (ValidationException e) {
//            return false;
//        }
//
//        return parityBit == codes.get(unifiedCreditCode.charAt(unifiedCreditCode.length() - 1));
//    }

    /**
     * 获取较验码
     *
     * @param unifiedCreditCode 统一社会信息代码
     * @param codes       带有映射关系的国家代码
     * @return 获取较验位的值
     */
    private static int getParityBit(String unifiedCreditCode, Map<Character, Integer> codes) {
        char[] businessCodeArray = unifiedCreditCode.toCharArray();

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char key = businessCodeArray[i];
            if (baseCode.indexOf(key) == -1) {
                throw new ValidationException("第" + String.valueOf(i + 1) + "位传入了非法的字符" + key);
            }
            sum += (codes.get(key) * wi[i]);
        }
        int result = 31 - sum % 31;
        return result == 31 ? 0 : result;
    }

    /**
     * 获取一个随机的统一社会信用代码
     *
     * @return 统一社会信用代码
     */
    public static String generateOneUnifiedCreditCode() {
        Random random = new Random();
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < 17; ++i) {
            int num = random.nextInt(baseCode.length() - 1);
            buf.append(baseCode.charAt(num));
        }

        String code = buf.toString();
        String upperCode = code.toUpperCase();
        BidiMap codes = generateCodes();
        int parityBit = getParityBit(upperCode, codes);

        if (codes.getKey(parityBit) == null) {
            upperCode = generateOneUnifiedCreditCode();
        } else {
            upperCode = upperCode + codes.getKey(parityBit);
        }
        return upperCode;
    }


    /**
     * 较验社会统一信用代码
     *
     * @param unifiedCreditCode 统一社会信息代码
     * @return 符合: true
     */
    public static boolean validateUnifiedCreditCode(String unifiedCreditCode) {
        return Pattern.matches("^([0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}|[1-9]\\d{14})$",unifiedCreditCode);
    }
}
