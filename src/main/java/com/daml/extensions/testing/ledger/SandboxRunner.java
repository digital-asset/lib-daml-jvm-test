/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.extensions.testing.junit5.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SandboxRunner {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Path relativeDarPath;
  private final Integer sandboxPort;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final Optional<LogLevel> logLevel;
  private final Path damlRoot;
  private Process sandbox;

  SandboxRunner(
      Path damlRoot,
      Path relativeDarPath,
      Integer sandboxPort,
      boolean useWallclockTime,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.damlRoot = damlRoot;
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
    addCustomCommands(commands);
    commands.add("--port");
    commands.add(sandboxPort.toString());
    commands.add(useWallclockTime ? "" : "--static-time");
    ledgerId.ifPresent(
        value -> {
          commands.add("-C");
          commands.add(String.format("ledgerId=%s", value));
        });
    logLevel.ifPresent(
        value -> {
          commands.add("--log-level-root");
          commands.add(value.toString());
        });
    commands.add("--dar");
    commands.add(relativeDarPath.toString());

    return commands;
  }

  public final void startSandbox() throws IOException {
    File workingDirectory = getWorkingDirectory(damlRoot);
    ProcessBuilder procBuilder =
        new ProcessBuilder(getDamlSandboxStarterCommand()).directory(workingDirectory);

    ProcessBuilder.Redirect redirect =
        ProcessBuilder.Redirect.appendTo(new File("integration-test-sandbox.log"));
    logger.debug("Executing: {}", String.join(" ", procBuilder.command()));
    logger.debug("Working directory: {}", workingDirectory);
    sandbox = procBuilder.redirectError(redirect).redirectOutput(redirect).start();
    logger.info("Starting sandbox...");
  }

  private File getWorkingDirectory(Path path) {
    return path != null ? path.toFile() : null;
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
