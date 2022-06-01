/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit4;

import org.junit.Assert;
import org.junit.Test;

import static com.daml.extensions.testing.TestCommons.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SandboxIT {


//  @ClassRule public static ExternalResource classRule = sandbox.getClassRule();
//
//  @Rule public ExternalResource rule = sandbox.getRule();

  @Test
  public void portIsAssignedAndSandboxIsStarted() {
    try {
      Sandbox sandbox =
              Sandbox.builder()
                      .damlRoot(PINGPONG_PATH)
                      .dar(DAR_PATH)
                      .ledgerId("sample-ledger")
                      .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
                      .build();
      sandbox.getSandboxManager().start();
      int sandboxPort = sandbox.getSandboxPort();
      Assert.assertTrue("Sandbox should be started", sandbox.isRunnning());
      assertsIsBetween(sandboxPort, 6860, 6890);
      assertThat(sandbox.getLedgerId(), is("sample-ledger"));
    } catch (Exception e) {
      Assert.assertTrue("Sandbox Exception " + e.getLocalizedMessage(),false);
    }
  }

  @Test
  public void portIsAssignedAndSandboxContainerIsStarted() {
    try {
      Sandbox sandbox =
              Sandbox.builder()
                      .damlRoot(RESOURCE_DIR.toAbsolutePath())
                      .dar(DAR_PATH.getFileName())
                      .useContainers()
                      .ledgerId("sample-ledger")
                      .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
                      .build();
      sandbox.getSandboxManager().start();
      int sandboxPort = sandbox.getSandboxPort();
      Assert.assertTrue("Sandbox should be started", sandbox.isRunnning());
      assertsIsBetween(sandboxPort, 6860, 6890);
      assertThat(sandbox.getLedgerId(), is("sample-ledger"));
    } catch (Exception e) {
      Assert.assertTrue("Sandbox Exception " + e.getLocalizedMessage(),false);
    }
  }

  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    Assert.assertTrue(message, low <= x && x <= high);
  }
}
