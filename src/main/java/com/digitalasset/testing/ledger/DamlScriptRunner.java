/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import static com.digitalasset.testing.utils.SandboxUtils.damlYamlP;
import static com.digitalasset.testing.utils.SandboxUtils.findDamlYaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class DamlScriptRunner {
  private final Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

  private final ProcessBuilder processBuilder;

  private Process script;

  private DamlScriptRunner(ProcessBuilder processBuilder) {
    this.processBuilder = processBuilder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void run() throws IOException, InterruptedException {
    logger.debug("Executing: {}", String.join(" ", processBuilder.command()));
    script = processBuilder.start();
    if (!scriptRunSuccessfully()) {
      throw new IllegalStateException("Unexpected termination of DAML script.");
    }
    logger.info("DAML Script has run successfully.");
  }

  private boolean scriptRunSuccessfully() throws InterruptedException {
    return script.waitFor(4, TimeUnit.MINUTES) && script.exitValue() == 0;
  }

  public static class Builder {
    private Path projectRoot;
    private Path darPath;
    private String scriptName;
    private String sandboxPort;
    private boolean useWallClockTime = false;

    public Builder dar(Path path) {
      this.darPath = path;
      return this;
    }

    public Builder scriptName(String scriptName) {
      this.scriptName = scriptName;
      return this;
    }

    public Builder sandboxPort(int port) {
      this.sandboxPort = Integer.toString(port);
      return this;
    }

    public Builder useWallclockTime(boolean useWallclockTime) {
      this.useWallClockTime = useWallclockTime;
      return this;
    }

    public Builder projectRoot(Path projectRoot) {
      try {
        if (Files.list(projectRoot).noneMatch(damlYamlP()))
          throw new IllegalArgumentException("Project root must contain a daml.yaml");

        this.projectRoot = projectRoot;
        return this;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public DamlScriptRunner build() {
      if (projectRoot == null) {
        projectRoot = findDamlYaml(darPath.toAbsolutePath().getParent());
      }

      File logFile = new File(String.format("integration-test-%s.log", scriptName));
      ProcessBuilder processBuilder =
          command()
              .redirectError(ProcessBuilder.Redirect.appendTo(logFile))
              .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
      return new DamlScriptRunner(processBuilder);
    }

    private ProcessBuilder command() {
      String sandboxHost = "localhost";
      return new ProcessBuilder()
          .directory(projectRoot.toFile())
          .command(
              "daml",
              "script",
              "--dar",
              darPath.toString(),
              "--script-name",
              scriptName,
              "--ledger-host",
              sandboxHost,
              "--ledger-port",
              sandboxPort,
              useWallClockTime ? "--wall-clock-time" : "--static-time");
    }
  }
}
