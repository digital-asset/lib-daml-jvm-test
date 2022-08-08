/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.PINGPONG_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SandboxTestExtension.class)
public class SandboxIT {

  private final int CUSTOM_PORT = 5555;

  @TestSandbox
  public static final Sandbox sandbox =
      Sandbox.builder()
          .damlRoot(PINGPONG_PATH)
          .dar(DAR_PATH)
          .ledgerId("sample-ledger")
          .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
          .build();

  @Test
  public void portIsAssignedWhenSandboxIsStarted() {
    int sandboxPort = sandbox.getSandboxPort();
    assertsIsBetween(sandboxPort, 6860, 6890);
  }

  @Test
  public void ledgerIdSpecified() throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(PINGPONG_PATH)
            .dar(DAR_PATH)
            .port(CUSTOM_PORT)
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
            .build();
    sandbox.getSandboxManager().start();
    assertTrue(sandbox.getLedgerId() == "sample-ledger");
    sandbox.getSandboxManager().stop();
  }

  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    assertTrue(low <= x && x <= high, message);
  }
}
