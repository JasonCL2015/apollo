package com.ctrip.framework.apollo.core.enums;

import com.ctrip.framework.apollo.core.utils.StringUtils;

public final class EnvUtils {
  
  public static Env transformEnv(String envName) {
    if (StringUtils.isBlank(envName)) {
      return Env.UNKNOWN;
    }
    switch (envName.trim().toUpperCase()) {
      case "DEV":
        return Env.DEV;
      case "TESTIN":
        return Env.TESTIN;
      case "TESTOUT":
        return Env.TESTOUT;
      case "ONLINE":
        return Env.ONLINE;
      default:
        return Env.UNKNOWN;
    }
  }
}
