/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.utils;

import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
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

  @Test
  public void fileIsNotDamlYamlRoot() {
    assertFalse(SandboxUtils.isDamlRoot(PINGPONG_PATH.resolve("daml.yaml")));
  }
}
