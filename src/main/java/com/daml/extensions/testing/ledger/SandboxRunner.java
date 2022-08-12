/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.extensions.testing.container.DamlContainer;
import com.daml.extensions.testing.junit5.LogLevel;
import com.daml.extensions.testing.utils.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.daml.extensions.testing.JvmTestLibCommon.CANTON_PARTICIPANT1_LEDGER_API_PORT;
import static com.daml.extensions.testing.JvmTestLibCommon.DEFAULT_IMAGE;

/** */
public class SandboxRunner {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private boolean useContainers;
  private Optional<String> damlImage;
  private final Path relativeDarPath;
  private final Integer sandboxPort;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final Optional<LogLevel> logLevel;
  private final Path damlRoot;
  private final String[] configFiles;
  private Process sandbox;
  private DamlContainer container;

  SandboxRunner(
      Path damlRoot,
      Path relativeDarPath,
      Integer sandboxPort,
      boolean useWallclockTime,
      boolean useContainers,
      Optional<String> damlImage,
      String[] configFiles,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.damlRoot = damlRoot;
    this.relativeDarPath = relativeDarPath;
    this.sandboxPort = sandboxPort;
    this.useWallclockTime = useWallclockTime;
    this.ledgerId = ledgerId;
    this.logLevel = logLevel;
    this.useContainers = useContainers;
    this.damlImage = damlImage;
    this.configFiles = configFiles;
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

  public boolean isRunning() {
    return useContainers ? container.isRunning() : sandbox != null;
  }

  public final void startSandbox() throws IOException {
    if (useContainers) startSandboxContainer();
    else startSandboxLocal();
  }

  private void startSandboxLocal() throws IOException {
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

  private void startSandboxContainer() {
    container = new DamlContainer(damlImage.orElse(DEFAULT_IMAGE));

    String command =
        "daemon -Dcanton.participants.participant1.ledger-api.address=0.0.0.0 --no-tty --config examples/01-simple-topology/simple-topology.conf --bootstrap examples/01-simple-topology/simple-ping.canton";

    if (configFiles != null) {
      logger.info("Custom config file are not supported in this version");
      //      for (String config : configFiles) command += " --config " + CONTAINER_DAR_PATH + "/" +
      // config;
    }

    logger.info("Command " + command);

    container.withExposedPorts(CANTON_PARTICIPANT1_LEDGER_API_PORT).withCommand(command).start();

    logger.info(container.getLogs());
    logger.info("Daml is running: " + container.isRunning());
  }

  public DamlContainer getContainer() {
    return container;
  }

  public void runScriptIfConfigured(String testModule, String testStartScript)
      throws IOException, InterruptedException {
    if (useContainers) logger.info("useContainers mode doesn't support daml scripts");
    else runLocalIfConfigured(testModule, testStartScript, false);
  }

  public void runLocalIfConfigured(String testModule, String testStartScript, boolean isTrigger)
      throws IOException, InterruptedException {
    DamlScriptRunner scriptRunner =
        new DamlScriptRunner.Builder()
            .damlRoot(damlRoot)
            .dar(relativeDarPath)
            .isTrigger(isTrigger)
            .sandboxPort(sandboxPort)
            .scriptName(String.format("%s:%s", testModule, testStartScript))
            .useWallclockTime(useWallclockTime)
            .build();
    scriptRunner.run();
  }

  private File getWorkingDirectory(Path path) {
    return path != null ? path.toFile() : null;
  }

  protected String getDamlCommand() {
    if (OS.isWindows()) return "daml.cmd";
    else return "daml";
  }

  protected void closeSandbox(Process sandbox) throws IOException {
    // Do not use destroy method, otherwise subprocesses cannot be stopped properly on Windows.
    // Closing the output stream is treated as signal for graceful termination.
    if (OS.isWindows()) sandbox.getOutputStream().close();
    else sandbox.destroy();
  }

  protected void addCustomCommands(List<String> commands) {
    if (OS.isWindows()) commands.add("--shutdown-stdin-close");
  }

  public final void stopSandbox() throws Exception {
    logger.info("Stopping sandbox...");
    if (sandbox != null) {
      closeSandbox(sandbox);
      sandbox.waitFor();
      sandbox = null;
    } else if (container != null) {
      container.stop();
    }
    logger.info("Stopped sandbox");
  }
}
