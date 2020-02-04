/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.utils.OS;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class SandboxRunner {
  private static final String DAML_COMMAND = OS.isWindows() ? "daml.cmd" : "daml";

  private final String relativeDarPath;
  private final Optional<String> testModule;
  private final Optional<String> testScenario;
  private final Integer sandboxPort;
  private final boolean useWallclockTime;

  private Process sandbox;

  public SandboxRunner(
      String relativeDarPath,
      Optional<String> testModule,
      Optional<String> testScenario,
      Integer sandboxPort,
      boolean useWallclockTime) {
    this.relativeDarPath = relativeDarPath;
    this.testModule = testModule;
    this.testScenario = testScenario;
    this.sandboxPort = sandboxPort;
    this.useWallclockTime = useWallclockTime;
  }

  public void startSandbox() throws IOException {
    ProcessBuilder procBuilder;
    if (testModule.isPresent() && testScenario.isPresent()) {
      procBuilder =
          new ProcessBuilder(
              DAML_COMMAND,
              "sandbox",
              "--",
              "-p",
              sandboxPort.toString(),
              "--scenario",
              String.format("%s:%s", testModule.get(), testScenario.get()),
              useWallclockTime ? "-w" : "-s",
              relativeDarPath);
    } else {
      procBuilder =
          new ProcessBuilder(
              DAML_COMMAND,
              "sandbox",
              "--",
              "-p",
              sandboxPort.toString(),
              useWallclockTime ? "-w" : "-s",
              relativeDarPath);
    }
    Redirect redirect = Redirect.appendTo(new File("integration-test-sandbox.log"))
    sandbox =
        procBuilder
            .redirectError(redirect)
            .redirectOutput(redirect)
            .start();
  }

  public void stopSandbox() throws Exception {
    if (sandbox != null) {
      sandbox.destroy();
      sandbox.waitFor();
    }
    sandbox = null;
  }
}
