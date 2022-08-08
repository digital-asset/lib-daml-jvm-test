package com.daml.extensions.testing.junit5;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.daml.extensions.testing.TestCommons.*;
import static com.daml.extensions.testing.utils.PackageUtils.findPackage;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SandboxContainerIT {

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
    assertTrue(sandbox.isRunnning(), "Sandbox should be started");
    assertNotNull(sandbox.getPartyId("alice"), "alice should be allocated");
    assertNotNull(sandbox.getPartyId("bob"), "bob should be allocated");
    assertNotNull(sandbox.getPartyId("charlie"), "charlie should be allocated");
    assertNotNull(customDarIsLoaded(sandbox.getClient(), PING_PONG_MODULE), "DAR is loaded");
  }

  private String customDarIsLoaded(
      DamlLedgerClient ledgerClient, DamlLf1.DottedName moduleDottedName)
      throws InvalidProtocolBufferException {
    return findPackage(ledgerClient, moduleDottedName);
  }
}
