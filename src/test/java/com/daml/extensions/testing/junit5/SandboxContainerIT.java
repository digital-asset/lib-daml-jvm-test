package com.daml.extensions.testing.junit5;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.daml.extensions.testing.TestCommons.*;
import static com.daml.extensions.testing.utils.PackageUtils.findPackage;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SandboxContainerIT {

  @Test
  public void sandboxContainerIsStartedAndSetUp()
      throws IOException, InterruptedException, TimeoutException {
    Sandbox sandbox =
        Sandbox.builder()
            .damlRoot(RESOURCE_DIR.toAbsolutePath())
            .dar(DAR_PATH)
            .parties(EXAMPLE_PARTIES)
            .useContainers()
            .build();
    sandbox.getSandboxManager().start();
    assertTrue(sandbox.isRunnning(), "Sandbox should be started");
    assertNotNull(findPackage(sandbox.getClient(), PING_PONG_MODULE), "DAR is loaded");
    checkIfExamplePartiesAllocated(sandbox.getSandboxManager());
  }
}
