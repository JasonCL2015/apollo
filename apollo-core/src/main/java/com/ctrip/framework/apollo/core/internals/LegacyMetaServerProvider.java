package com.ctrip.framework.apollo.core.internals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *  For legacy meta server configuration use, i.e. apollo-env.properties
 *  根据逻辑环境名获取metaserver地址，优先级最低
 * @Package com.ctrip.framework.apollo.core.internals
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2018/8/30 下午5:00
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class LegacyMetaServerProvider implements MetaServerProvider {
  public static final int ORDER = MetaServerProvider.LOWEST_PRECEDENCE - 1;
  private static final Map<Env, String> domains = new HashMap<>();

  public LegacyMetaServerProvider() {
    initialize();
  }

  private void initialize() {
    Properties prop = new Properties();
    prop = ResourceUtils.readConfigFile("apollo-env.properties", prop);
    Properties env = System.getProperties();
    domains.put(Env.DEV,
            env.getProperty("dev_meta", prop.getProperty("dev.meta")));
    domains.put(Env.TESTIN,
        env.getProperty("testin_meta", prop.getProperty("testin.meta")));
    domains.put(Env.TESTOUT,
        env.getProperty("testout_meta", prop.getProperty("testout.meta")));
    domains.put(Env.ONLINE,
            env.getProperty("online_meta", prop.getProperty("online.meta")));
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
