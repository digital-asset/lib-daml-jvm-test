/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger;

import static com.digitalasset.testing.TestCommons.DAR_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.Test;

public class SandboxManagerIT {

  @Test
  public void managerStopsSandboxGracefully() throws Exception {
    eventually(() -> assertTrue(jps().stream().noneMatch(p -> p.contains("daml-sdk.jar"))));

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

  @Test
  public void managerReturnsAutomaticallyAssignedLedgerId() throws Exception {
    SandboxManager manager =
        new SandboxManager(
            Optional.empty(),
            Optional.empty(),
            Duration.ofMinutes(1),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false,
            Optional.empty(),
            Optional.empty());
    String ledgerIdPattern = "sandbox-[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}";
    try {
      manager.start();
      assertTrue(manager.getLedgerId().matches(ledgerIdPattern));
    } finally {
      manager.stop();
    }
  }

  @Test
  public void managerReturnsSpecifiedLedgerId() throws Exception {
    SandboxManager manager =
        new SandboxManager(
            Optional.empty(),
            Optional.empty(),
            Duration.ofMinutes(1),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false,
            Optional.of("Test Ledger ID"),
            Optional.empty());
    try {
      manager.start();
      assertThat(manager.getLedgerId(), is("Test Ledger ID"));
    } finally {
      manager.stop();
    }
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
        if (hasPassed.apply(Duration.ofMinutes(5))) {
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
