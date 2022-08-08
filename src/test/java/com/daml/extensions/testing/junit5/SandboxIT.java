/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.hamcrest.Matchers;

import static com.daml.extensions.testing.TestCommons.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.daml.extensions.testing.utils.PackageUtils.findPackage;

@ExtendWith(SandboxTestExtension.class)
public class SandboxIT {

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
    Assert.assertTrue(sandbox.getLedgerId() == "sample-ledger");
    sandbox.getSandboxManager().stop();
  }

  @Test
  public void sandboxContainerIsStartedAndPartiesAreAllocated()
      throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(RESOURCE_DIR.toAbsolutePath())
            .dar(DAR_PATH)
            .parties("alice", "bob", "charlie")
            .useContainers()
            .ledgerId("sample-ledger")
            .logLevel(LogLevel.DEBUG) // implicitly test loglevel override
            .build();
    sandbox.getSandboxManager().start();
    Assert.assertTrue("Sandbox should be started", sandbox.isRunnning());
    Assert.assertNotNull("alice should be allocated", sandbox.getPartyId("alice"));
    Assert.assertNotNull("bob should be allocated", sandbox.getPartyId("bob"));
    Assert.assertNotNull("charlie should be allocated", sandbox.getPartyId("charlie"));
    Assert.assertNotNull("DAR is loaded", customDarIsLoaded(sandbox.getClient(), PING_PONG_MODULE));
  }

  private String customDarIsLoaded(DamlLedgerClient ledgerClient, DamlLf1.DottedName moduleDottedName) throws InvalidProtocolBufferException {
    return findPackage(ledgerClient, moduleDottedName);
  }
  private void assertsIsBetween(int x, int low, int high) {
    String message = String.format("Expected '%d' to be between '%d' and '%d'", x, low, high);
    assertTrue(low <= x && x <= high, message);
  }
}
