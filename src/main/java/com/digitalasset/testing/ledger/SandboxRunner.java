/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class SandboxRunner {
  private static final Logger logger = LoggerFactory.getLogger(SandboxRunner.class);

  private final String relativeDarPath;
  private final Optional<String> testModule;
  private final Optional<String> testScenario;
  private final Integer sandboxPort;
  private final Duration waitTimeout;

  private Process sandbox;

  public SandboxRunner(
      String relativeDarPath,
      Optional<String> testModule,
      Optional<String> testScenario,
      Integer sandboxPort,
      Duration waitTimeout) {
    this.relativeDarPath = relativeDarPath;
    this.testModule = testModule;
    this.testScenario = testScenario;
    this.sandboxPort = sandboxPort;
    this.waitTimeout = waitTimeout;
  }

  public void startSandbox() throws IOException, TimeoutException {
    ProcessBuilder procBuilder;
    if (testModule.isPresent() && testScenario.isPresent()) {
      procBuilder =
          new ProcessBuilder(
              "daml",
              "sandbox",
              "--",
              "-p",
              sandboxPort.toString(),
              "--scenario",
              String.format("%s:%s", testModule.get(), testScenario.get()),
              relativeDarPath);
    } else {
      procBuilder =
          new ProcessBuilder(
              "daml", "sandbox", "--", "-p", sandboxPort.toString(), relativeDarPath);
    }
    sandbox =
        procBuilder
            .redirectError(new File("integration-test-sandbox.log"))
            .redirectOutput(new File("integration-test-sandbox.log"))
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
