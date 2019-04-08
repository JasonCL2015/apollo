package com.ctrip.framework.apollo.core.internals;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * All rights Reserved, Designed By www.maihaoche.com For legacy meta server
 * configuration use, i.e. apollo-env.properties 根据逻辑环境名获取metaserver地址，优先级最低
 * 
 * @Package com.ctrip.framework.apollo.core.internals
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2018/8/30 下午5:00
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 *             注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class LegacyMetaServerProvider implements MetaServerProvider {

  // make it as lowest as possible, yet not the lowest
  public static final int ORDER = MetaServerProvider.LOWEST_PRECEDENCE - 1;
  private static final Map<Env, String> domains = new HashMap<>();

  public LegacyMetaServerProvider() {
    initialize();
  }

  private void initialize() {
    Properties prop = new Properties();
    prop = ResourceUtils.readConfigFile("apollo-env.properties", prop);
    Properties env = System.getProperties();
    domains.put(Env.DEV, env.getProperty("dev_meta", prop.getProperty("dev.meta")));
    domains.put(Env.TESTIN, env.getProperty("testin_meta", prop.getProperty("testin.meta")));
    domains.put(Env.TESTOUT, env.getProperty("testout_meta", prop.getProperty("testout.meta")));
    domains.put(Env.ONLINE, env.getProperty("online_meta", prop.getProperty("online.meta")));

    // domains.put(Env.LOCAL, getMetaServerAddress(prop, "local_meta",
    // "local.meta"));
    // domains.put(Env.DEV, getMetaServerAddress(prop, "dev_meta", "dev.meta"));
    // domains.put(Env.FAT, getMetaServerAddress(prop, "fat_meta", "fat.meta"));
    // domains.put(Env.UAT, getMetaServerAddress(prop, "uat_meta", "uat.meta"));
    // domains.put(Env.LPT, getMetaServerAddress(prop, "lpt_meta", "lpt.meta"));
    // domains.put(Env.PRO, getMetaServerAddress(prop, "pro_meta", "pro.meta"));
  }

  private String getMetaServerAddress(Properties prop, String sourceName, String propName) {
    // 1. Get from System Property.
    String metaAddress = System.getProperty(sourceName);
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 2. Get from OS environment variable, which could not contain dot and is
      // normally in UPPER case,like DEV_META.
      metaAddress = System.getenv(sourceName.toUpperCase());
    }
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 3. Get from properties file.
      metaAddress = prop.getProperty(propName);
    }
    return metaAddress;
  }

  @Override
  public String getMetaServerAddress(Env targetEnv) {
    String metaServerAddress = domains.get(targetEnv);
    return metaServerAddress == null ? null : metaServerAddress.trim();
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
