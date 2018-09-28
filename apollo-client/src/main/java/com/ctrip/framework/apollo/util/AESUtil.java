package com.ctrip.framework.apollo.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 * AES算法编程实现
 * @Package com.ctrip.framework.apollo.util
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2018/9/28 下午1:38
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class AESUtil {
    //apollo秘钥
    private static final String KEY = "2wc3B79u3zHP6gnA";

    private static int parse(char c) {
        if (c >= 'a') {
            return (c - 'a' + 10) & 0x0f;
        }
        if (c >= 'A') {
            return (c - 'A' + 10) & 0x0f;
        }
        return (c - '0') & 0x0f;
    }

    /**
     * <p> 加密 </p>
     * @param data
     * @return byte[]
     * @author 文远（wenyuan@maihaoche.com）
     * @date 2018/9/28 下午1:53 
     * @since V1.1.0-SNAPSHOT
     * 
     */
    public static byte[] encryptAES(String data) throws Exception{
        byte[] key = KEY.getBytes();
        //恢复密钥
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        //Cipher完成加密
        Cipher cipher = Cipher.getInstance("AES");
        //根据密钥对cipher进行初始化
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        //加密
        byte[] encrypt = cipher.doFinal(data.getBytes());

        return encrypt;
    }
    
    
    /**
     * <p> 解密 </p>
     * @param data key
     * @return byte[]
     * @author 文远（wenyuan@maihaoche.com）
     * @date 2018/9/28 下午1:53 
     * @since V1.1.0-SNAPSHOT
     * 
     */
    public static byte[] decryptAES(String data) throws Exception{
        byte[] key = KEY.getBytes();
        //恢复密钥生成器
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        //Cipher完成解密
        Cipher cipher = Cipher.getInstance("AES");
        //根据密钥对cipher进行初始化
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] plain = cipher.doFinal(HexString2Bytes(data));
        return plain;
    }

    /**
     * <p> byte[]转为16进制HexString </p>
     * @param resultBytes
     * @return hexstring
     * @author 文远（wenyuan@maihaoche.com）
     * @date 2018/9/28 下午1:53 
     * @since V1.1.0-SNAPSHOT
     * 
     */
    public static String fromBytesToHex(byte[] resultBytes){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < resultBytes.length; i++){
            if(Integer.toHexString(0xFF & resultBytes[i]).length() == 1){
                builder.append("0").append(Integer.toHexString(0xFF & resultBytes[i]));
            }else{
                builder.append(Integer.toHexString(0xFF & resultBytes[i]));
            }
        }
        return builder.toString();
    }

    /**
     * <p> 从十六进制字符串到字节数组转换 </p>
     * @param hexstr
     * @return byte[]
     * @author 文远（wenyuan@maihaoche.com）
     * @date 2018/9/28 下午2:11
     * @since V1.1.0-SNAPSHOT
     * 
     */
    public static byte[] HexString2Bytes(String hexstr) {
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
        }
        return b;
    }

    public static void main(String[] args) throws Exception {
        String DATA = "1234";
        String key = "2wc3B79u3zHP6gnA";
        //加密
        byte[] encrypt = AESUtil.encryptAES(DATA);
        System.out.println(DATA + " AES 加密 : " + fromBytesToHex(encrypt));

        //解密
        byte[] plain = AESUtil.decryptAES(fromBytesToHex(encrypt));
        System.out.println(DATA + " AES 解密 : " + new String(plain));
    }
}