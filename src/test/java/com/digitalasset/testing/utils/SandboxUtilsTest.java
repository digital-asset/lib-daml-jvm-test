/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils;

import static com.digitalasset.testing.TestCommons.PINGPONG_PATH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SandboxUtilsTest {

  @Test
  public void pathWithDamlYamlIsDamlRoot() {
    assertTrue(SandboxUtils.isDamlRoot(PINGPONG_PATH));
  }

  @Test
  public void damlYamlIsNotSearched() {
    assertFalse(SandboxUtils.isDamlRoot(PINGPONG_PATH.getParent()));
  }
}
