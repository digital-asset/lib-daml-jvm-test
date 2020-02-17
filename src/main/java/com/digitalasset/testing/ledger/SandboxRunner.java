/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.utils.OS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    List<String> commands = new ArrayList<>();
    commands.add(DAML_COMMAND);
    commands.add("sandbox");
    commands.add("--");
    commands.add("--shutdown-stdin-close");
    commands.add("-p");
    commands.add(sandboxPort.toString());
    commands.add(useWallclockTime ? "-w" : "-s");
    if (testModule.isPresent() && testScenario.isPresent()) {
      commands.add("--scenario");
      commands.add(String.format("%s:%s", testModule.get(), testScenario.get()));
    }
    commands.add(relativeDarPath);
    ProcessBuilder procBuilder = new ProcessBuilder(commands);
    ProcessBuilder.Redirect redirect =
        ProcessBuilder.Redirect.appendTo(new File("integration-test-sandbox.log"));
    sandbox = procBuilder.redirectError(redirect).redirectOutput(redirect).start();
  }

  public void stopSandbox() throws Exception {
    if (sandbox != null) {
      // Do not use destroy method, otherwise subprocesses cannot be stopped properly on Windows.
      // Closing the output stream is treated as signal for graceful termination.
      sandbox.getOutputStream().close();
      sandbox.waitFor();
    }
    sandbox = null;
  }
}
