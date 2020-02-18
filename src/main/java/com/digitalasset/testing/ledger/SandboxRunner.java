/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SandboxRunner {
  protected final String relativeDarPath;
  protected final Optional<String> testModule;
  protected final Optional<String> testScenario;
  protected final Integer sandboxPort;
  protected final boolean useWallclockTime;
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
    commands.add(getDamlCommand());
    commands.add("sandbox");
    commands.add("--");
    addCustomCommands(commands);
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

  protected void addCustomCommands(List<String> commands) {}

  protected abstract String getDamlCommand();

  public void stopSandbox() throws Exception {
    if (sandbox != null) {
      closeSandbox(sandbox);
      sandbox.waitFor();
    }
    sandbox = null;
  }

  protected abstract void closeSandbox(Process sandbox) throws IOException;
}
