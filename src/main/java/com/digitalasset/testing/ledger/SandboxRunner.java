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

import com.digitalasset.testing.junit4.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SandboxRunner {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String relativeDarPath;
  private final Optional<String> testModule;
  private final Optional<String> testStartScript;
  private final Integer sandboxPort;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final Optional<LogLevel> logLevel;
  private Process sandbox;

  public SandboxRunner(
      String relativeDarPath,
      Optional<String> testModule,
      Optional<String> testStartScript,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.relativeDarPath = relativeDarPath;
    this.testModule = testModule;
    this.testStartScript = testStartScript;
    this.sandboxPort = sandboxPort;
    this.useWallclockTime = useWallclockTime;
    this.ledgerId = ledgerId;
    this.logLevel = logLevel;
  }

  private List<String> commands() {
    List<String> commands = new ArrayList<>();
    commands.add(getDamlCommand());
    commands.add("sandbox");
    commands.add("--");
    addCustomCommands(commands);
    commands.add("-p");
    commands.add(sandboxPort.toString());
    commands.add(useWallclockTime ? "-w" : "-s");
    if (testModule.isPresent() && testStartScript.isPresent()) {
      commands.add("--scenario");
      commands.add(String.format("%s:%s", testModule.get(), testStartScript.get()));
    }
    ledgerId.ifPresent(
        value -> {
          commands.add("--ledgerid");
          commands.add(value);
        });
    logLevel.ifPresent(
        value -> {
          commands.add("--log-level");
          commands.add(value.toString());
        });
    commands.add(relativeDarPath);
    return commands;
  }

  public final void startSandbox() throws IOException {
    ProcessBuilder procBuilder = new ProcessBuilder(commands());
    ProcessBuilder.Redirect redirect =
        ProcessBuilder.Redirect.appendTo(new File("integration-test-sandbox.log"));
    logger.debug("Executing: {}", String.join(" ", procBuilder.command()));
    sandbox = procBuilder.redirectError(redirect).redirectOutput(redirect).start();
    logger.info("Starting sandbox...");
  }

  protected void addCustomCommands(List<String> commands) {}

  protected abstract String getDamlCommand();

  public final void stopSandbox() throws Exception {
    if (sandbox != null) {
      logger.info("Stopping sandbox...");
      closeSandbox(sandbox);
      sandbox.waitFor();
      logger.info("Stopped sandbox");
    }
    sandbox = null;
  }

  protected abstract void closeSandbox(Process sandbox) throws IOException;
}
