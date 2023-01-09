/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import static com.daml.extensions.testing.utils.Preconditions.require;
import static com.daml.extensions.testing.utils.SandboxUtils.isDamlRoot;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private boolean scriptRunSuccessfully() throws InterruptedException, IOException {
    script.waitFor(4, TimeUnit.MINUTES);

    // log extra errors from the sdk
    try (InputStream errorStream = script.getErrorStream()) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
        String line = "";
        while((line =reader.readLine())!=null)
        {
          logger.error(line);
        }
      }
    }

    return (script.exitValue() == 0);
  }

  public static class Builder {
    private Path damlRoot;
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

    public Builder damlRoot(Path damlRoot) {
      this.damlRoot = damlRoot;
      return this;
    }

    public DamlScriptRunner build() {
      require(
          isDamlRoot(damlRoot),
          String.format("DAML root '%s' must contain a daml.yaml.", damlRoot));
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
          .directory(damlRoot.toFile())
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
