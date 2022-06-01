/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit4;

import org.junit.Test;

import java.util.Optional;

import static com.daml.extensions.testing.TestCommons.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SandboxTest {

  @Test
  public void logLevelIsSet() {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(PINGPONG_PATH)
            .dar(DAR_PATH)
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.TRACE)
            .build();
    assertThat(sandbox.getLogLevel(), is(Optional.of(LogLevel.TRACE)));
  }

  @Test(expected = IllegalStateException.class)
  public void darPathIsRequiredForSandbox() {
    Sandbox.builder().build();
  }

  @Test(expected = IllegalStateException.class)
  public void damlRootIsRequiredForSandbox() {
    Sandbox.builder().dar(DAR_PATH).damlRoot(RESOURCE_DIR).build();
  }
  
}
