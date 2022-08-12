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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SandboxCustomTest {

  private final int CUSTOM_PORT = 5555;
  private static final String CUSTOM_LEDGER_ID = "sample-ledger";

  @Test
  public void customLogLevelIsSet() {
    Sandbox sandbox =
        Sandbox.builder().damlRoot(PINGPONG_PATH).dar(DAR_PATH).logLevel(LogLevel.TRACE).build();
    Assertions.assertEquals(sandbox.getLogLevel(), Optional.of(LogLevel.TRACE));
  }

  @Test
  public void customPortIsAssignedWhenSandboxIsStarted()
      throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox =
        Sandbox.builder().damlRoot(PINGPONG_PATH).dar(DAR_PATH).port(CUSTOM_PORT).build();
    sandbox.restart();
    assertEquals(sandbox.getSandboxPort(), CUSTOM_PORT);
    sandbox.stop();
  }

  @Test
  public void customLedgerIdSpecified() throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(PINGPONG_PATH)
            .dar(DAR_PATH)
            .ledgerId(CUSTOM_LEDGER_ID)
            .logLevel(LogLevel.TRACE)
            .build();
    sandbox.restart();
    assertTrue(sandbox.getLedgerId() == CUSTOM_LEDGER_ID);
    sandbox.stop();
  }
}
