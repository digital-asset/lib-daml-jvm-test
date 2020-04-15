/*
 * Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.testing.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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
    if (script.waitFor() != 0) {
      throw new IllegalStateException("Unexpected termination of DAML script.");
    }
    logger.info("DAML Script has run successfully.");
  }

  public void kill() {
    try {
      if (script.isAlive()) {
        script.destroyForcibly().waitFor();
      }
    } catch (InterruptedException e) {
      logger.error("Could not stop DAML script.", e);
    }
  }

  public static class Builder {

    private String darPath;
    private String scriptName;
    private String sandboxPort;
    private boolean useWallclockTime = false;

    public Builder dar(Path path) {
      this.darPath = path.toString();
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
      this.useWallclockTime = useWallclockTime;
      return this;
    }

    public Builder useWallclockTime() {
      this.useWallclockTime = true;
      return this;
    }

    public DamlScriptRunner build() {
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
          .command(
              "daml",
              "script",
              "--dar",
              darPath,
              "--script-name",
              scriptName,
              "--ledger-host",
              sandboxHost,
              "--ledger-port",
              sandboxPort,
              useWallclockTime ? "--wall-clock-time" : "--static-time");
    }
  }
}
