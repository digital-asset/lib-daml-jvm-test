/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import com.digitalasset.testing.junit4.LogLevel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SandboxRunner {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Path relativeDarPath;
  private final Integer sandboxPort;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final Optional<LogLevel> logLevel;
  private final File damlRoot;
  private Process sandbox;

  SandboxRunner(
      Path damlRoot,
      Path relativeDarPath,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.damlRoot = damlRoot != null ? damlRoot.toFile() : null;
    this.relativeDarPath = relativeDarPath;
    this.sandboxPort = sandboxPort;
    this.useWallclockTime = useWallclockTime;
    this.ledgerId = ledgerId;
    this.logLevel = logLevel;
  }

  private List<String> getDamlSandboxStarterCommand() {
    List<String> commands = new ArrayList<>();
    commands.add(getDamlCommand());
    commands.add("sandbox");
    commands.add("--");
    addCustomCommands(commands);
    commands.add("-p");
    commands.add(sandboxPort.toString());
    commands.add(useWallclockTime ? "-w" : "-s");
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
    commands.add(relativeDarPath.toString());
    return commands;
  }

  public final void startSandbox() throws IOException {
    ProcessBuilder procBuilder =
        new ProcessBuilder(getDamlSandboxStarterCommand()).directory(damlRoot);

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
