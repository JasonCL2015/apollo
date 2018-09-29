package com.ctrip.framework.apollo.util;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *
 * @Package com.ctrip.framework.apollo.util
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2018/9/29 下午4:09
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class PropertiesUtil {

    private static Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    //加密前缀
    private static final String PASSWORDPRE = "{apollo}";
    /**
     * <p> 解密配置文件中加密属性 </p>
     * @param properties
     * @return Properties
     * @author 文远（wenyuan@maihaoche.com）
     * @date 2018/9/29 下午4:15
     * @since V1.1.0-SNAPSHOT
     *
     */
    public static Properties descyptProperties(Properties properties) {
        Properties newProperties = (Properties) properties.clone();
        for (Object key : newProperties.keySet()) {
            String val = (String) newProperties.get(key);
            if (!StringUtils.isEmpty(val) && val.contains(PASSWORDPRE)) {
                String passwordHexString = val.replace(PASSWORDPRE, "");
                try {
                    val = new String(AESUtil.decryptAES(passwordHexString));
                    newProperties.put(key, val);
                } catch (Exception e) {
                    logger.error("解密字符串失败:" + e);
                }
            }
        }
        return newProperties;
    }
}
