package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *  默认metaServer提供者，Order=0，数字越小优先级越高
 * @Package com.ctrip.framework.apollo.internals
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2018/8/30 下午6:22
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class DefaultMetaServerProvider implements MetaServerProvider {

  public static final int ORDER = 0;
  private static final Logger logger = LoggerFactory.getLogger(DefaultMetaServerProvider.class);

  private final String metaServerAddress;

  public DefaultMetaServerProvider() {
    metaServerAddress = initMetaServerAddress();
  }

  private String initMetaServerAddress() {
    // 1. Get from System Property
    String metaAddress = System.getProperty(ConfigConsts.APOLLO_META_KEY);
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 2. Get from OS environment variable, which could not contain dot and is normally in UPPER case
      metaAddress = System.getenv("APOLLO_META");
    }
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 3. Get from server.properties
      metaAddress = Foundation.server().getProperty(ConfigConsts.APOLLO_META_KEY, null);
    }
    if (Strings.isNullOrEmpty(metaAddress)) {
      // 4. Get from app.properties
      metaAddress = Foundation.app().getProperty(ConfigConsts.APOLLO_META_KEY, null);
    }

    if (Strings.isNullOrEmpty(metaAddress)) {
      logger.info("Could not find meta server address, because it is not available in neither (1) JVM system property 'apollo.meta', (2) " +
              "OS env variable 'APOLLO_META' (3) property 'apollo.meta' from server.properties nor (4) property 'apollo.meta' from app" +
              ".properties, we will use custom metaServerAddress from mhc");
    } else {
      metaAddress = metaAddress.trim();
      logger.info("Located meta services from apollo.meta configuration: {}!", metaAddress);
    }

    return metaAddress;
  }

  /**
   * <p>
   *     开发环境、内网测试环境匹配metaServer地址：http://apollo-testin-meta.haimaiche.net
   *     公网测试环境匹配metaServer地址：http://apollo-testout-meta.haimaiche.net
   *     线上环境、未知环境、默认匹配metaServer地址：http://apollo-online-config.haimaiche.net
   * </p>
   * @param targetEnv
   * @return metaServerAddress
   * @author 文远（wenyuan@maihaoche.com）
   * @date 2018/8/30 下午6:17
   * @since V1.1.0-SNAPSHOT
   *
   */
  @Override
  public String getMetaServerAddress(Env targetEnv) {
    if (!Strings.isNullOrEmpty(metaServerAddress)) {
      return metaServerAddress;
    }
    switch (targetEnv) {
      case DEV:
        return "http://apollo-testin-config.haimaiche.net";
      case TESTIN:
        return "http://apollo-testin-config.haimaiche.net";
      case TESTOUT:
        return "http://apollo-testout-config.haimaiche.net";
      case ONLINE:
        return "http://apollo-online-config.haimaiche.net";
      case UNKNOWN:
        return "http://apollo-online-config.haimaiche.net";
      default:
          return "http://apollo-online-config.haimaiche.net";
    }
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}
