/*
 *  Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils;

import static com.digitalasset.testing.TestCommons.PINGPONG_PATH;
import static com.digitalasset.testing.TestCommons.RESOURCE_DIR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public class SandboxUtilsTest {

  @Test
  public void pathWithDamlYamlIsDamlRoot() throws IOException {
    assertTrue(SandboxUtils.isDamlRoot(PINGPONG_PATH));
  }

  @Test
  public void damlYamlIsNotSearched() throws IOException {
    assertFalse(SandboxUtils.isDamlRoot(RESOURCE_DIR));
  }
}
