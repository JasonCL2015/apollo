package com.ctrip.framework.apollo.core.enums;

import static org.junit.Assert.*;

import org.junit.Test;

public class EnvUtilsTest {

  @Test
  public void testTransformEnv() throws Exception {
    assertEquals(Env.DEV, EnvUtils.transformEnv(Env.DEV.name()));
    assertEquals(Env.TESTIN, EnvUtils.transformEnv(" " + Env.TESTIN.name().toUpperCase() + ""));
    assertEquals(Env.TESTOUT, EnvUtils.transformEnv(" " + Env.TESTOUT.name().toUpperCase() + ""));
    assertEquals(Env.ONLINE, EnvUtils.transformEnv(Env.ONLINE.name().toLowerCase()));
    assertEquals(Env.UNKNOWN, EnvUtils.transformEnv("someInvalidEnv"));
  }

  @Test
  public void testFromString() throws Exception {
    assertEquals(Env.DEV, Env.fromString(Env.DEV.name()));
    assertEquals(Env.ONLINE, Env.fromString(Env.ONLINE.name().toLowerCase()));
    assertEquals(Env.TESTIN, Env.fromString(" " + Env.TESTIN.name().toUpperCase() + ""));
    assertEquals(Env.TESTOUT, Env.fromString(" " + Env.TESTOUT.name().toUpperCase() + ""));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromInvalidString() throws Exception {
    Env.fromString("someInvalidEnv");
  }
}
