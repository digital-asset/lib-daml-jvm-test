/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.extensions.testing.container.DamlContainer;
import com.daml.extensions.testing.junit4.LogLevel;
import com.daml.extensions.testing.utils.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** */
public class SandboxRunner {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final String DEFAULT_IMAGE = "digitalasset/daml-sdk:2.2.0";
  private boolean useContainers = false;
  private Optional<String> damlImage;
  private final Path relativeDarPath;
  private final Integer sandboxPort;
  private final Integer adminApiPort;
  private final boolean useWallclockTime;
  private final Optional<String> ledgerId;
  private final Optional<LogLevel> logLevel;
  private final Path damlRoot;
  private final String[] configFiles;
  private Process sandbox;

  SandboxRunner(
      Path damlRoot,
      Path relativeDarPath,
      Integer sandboxPort,
      Integer adminApiPort,
      boolean useWallclockTime,
      boolean useContainers,
      Optional<String> damlImage,
      String[] configFiles,
      Optional<String> ledgerId,
      Optional<LogLevel> logLevel) {
    this.damlRoot = damlRoot;
    this.relativeDarPath = relativeDarPath;
    this.sandboxPort = sandboxPort;
    this.adminApiPort = adminApiPort;
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

  private DamlContainer container;
  private static final String CONTAINER_DAR_PATH = "/release";

  private void startSandboxContainer() throws IOException {
    container = new DamlContainer(damlImage.orElse(DEFAULT_IMAGE));
    String command = "daml sandbox --dar " + CONTAINER_DAR_PATH + "/" + relativeDarPath + " ";
    if (configFiles != null)
      for (String config : configFiles) command += " -c " + CONTAINER_DAR_PATH + "/" + config;
    logger.info("Command " + command);
    container
        .withFileSystemBind(damlRoot.toString(), CONTAINER_DAR_PATH, BindMode.READ_ONLY)
        .fixedPorts(sandboxPort, 6865)
        .fixedPorts(adminApiPort, 6866)
        .withCommand("/bin/sh", "-c", command)
        .start();
    logger.info(container.getLogs());
    logger.info("Daml is running: " + container.isRunning());
  }

  public void runScriptIfConfigured(String testModule, String testStartScript)
      throws IOException, InterruptedException {
    if (useContainers) runInContainerIfConfigured(testModule, testStartScript, false);
    else runLocalIfConfigured(testModule, testStartScript, false);
  }

  public void runInContainerIfConfigured(String testModule, String testStartScript, boolean isTrigger) throws IOException, InterruptedException {
        List<String> script = new ArrayList<>();
        String st = isTrigger ? "trigger" : "script";

        script.add("sh");
        script.add("-c");
        script.add(
            "daml "
                + st
                + " --dar "
                + CONTAINER_DAR_PATH
                + "/"
                + relativeDarPath
                + "  --"
                + st
                + "-name "
                + testModule
                + ":"
                + testStartScript
                + (useWallclockTime ? " --wall-clock-time" : " --static-time")
                + " --ledger-host localhost --ledger-port 6865");
        Container.ExecResult res = container.execInContainer(script.toArray(new String[script.size()]));
        logger.info("Output: " + res.getStdout());
        logger.info(container.getLogs());
        logger.info("Error: " + res.getStderr());
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
