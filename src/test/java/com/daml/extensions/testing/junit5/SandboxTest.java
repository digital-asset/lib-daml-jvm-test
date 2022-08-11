/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SandboxTest {

  private final int CUSTOM_PORT = 5555;

  @Test
  public void logLevelIsSet() {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(PINGPONG_PATH)
            .dar(DAR_PATH)
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.TRACE)
            .build();
    Assertions.assertEquals(sandbox.getLogLevel(), Optional.of(LogLevel.TRACE));
  }

  @Test
  public void darPathIsRequiredForSandbox() {
    Assertions.assertThrows(IllegalStateException.class, () -> Sandbox.builder().build());
  }

  @Test
  public void damlRootIsRequiredForSandbox() {
    Assertions.assertThrows(
        IllegalStateException.class, () -> Sandbox.builder().dar(DAR_PATH).build());
  }

  @Test
  public void specifiedPortIsAssignedWhenSandboxIsStarted()
      throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(PINGPONG_PATH)
            .dar(DAR_PATH)
            .port(CUSTOM_PORT)
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
            .build();
    sandbox.restart();
    assertEquals(sandbox.getSandboxPort(), CUSTOM_PORT);
    sandbox.stop();
  }
}
