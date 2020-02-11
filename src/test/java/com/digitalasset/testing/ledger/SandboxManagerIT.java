/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.digitalasset.testing.TestCommons.DAR_PATH;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SandboxManagerIT {

  @Test
  public void managerStopsSandboxGracefully() throws Exception {
    SandboxManager manager =
        new SandboxManager(
            Optional.empty(),
            Optional.empty(),
            Duration.ofMinutes(1),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false);
    manager.start();

    eventually(() -> assertTrue(jps().stream().anyMatch(p -> p.contains("daml-sdk.jar"))));

    manager.stop();

    eventually(() -> assertTrue(jps().stream().noneMatch(p -> p.contains("daml-sdk.jar"))));
  }

  private void eventually(Runnable code) throws InterruptedException {
    Instant started = Instant.now();
    Function<Duration, Boolean> hasPassed =
        x -> Duration.between(started, Instant.now()).compareTo(x) > 0;
    boolean isSuccessful = false;
    while (!isSuccessful) {
      try {
        code.run();
        isSuccessful = true;
      } catch (Throwable ignore) {
        if (hasPassed.apply(Duration.ofSeconds(60))) {
          fail("Code did not succeed in time.");
        } else {
          Thread.sleep(200);
          isSuccessful = false;
        }
      }
    }
  }

  private Collection<String> jps() {
    try {
      List<String> jvmProcesses;
      Process jps = new ProcessBuilder("jps").start();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(jps.getInputStream()))) {
        jvmProcesses = reader.lines().collect(Collectors.toList());
      }
      jps.destroy();
      jps.waitFor();
      return jvmProcesses;
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
