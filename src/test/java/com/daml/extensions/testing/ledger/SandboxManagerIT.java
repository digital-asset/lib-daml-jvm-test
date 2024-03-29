/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.daml.extensions.testing.TestCommons.DAR_PATH;
import static com.daml.extensions.testing.TestCommons.RESOURCE_DIR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class SandboxManagerIT {

  @Test
  public void managerStopsSandboxGracefully() throws Exception {
    Integer jpsStreamLengthBefore = jpsStreamLengthNow();
    SandboxManager manager =
        new SandboxManager(
            RESOURCE_DIR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Duration.ofMinutes(1),
            Duration.ofSeconds(10),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false);
    manager.start();

    eventually(() -> assertTrue(jpsStreamLengthBefore < jpsStreamLengthNow()));

    manager.stop();

    eventually(() -> assertSame(jpsStreamLengthBefore, jpsStreamLengthNow()));
  }

  private Integer jpsStreamLengthNow() {
    return jps().stream().toArray().length;
  }

  @Test
  public void managerReturnsAutomaticallyAssignedLedgerId() throws Exception {
    SandboxManager manager =
        new SandboxManager(
            RESOURCE_DIR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Duration.ofMinutes(1),
            Duration.ofSeconds(10),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false,
            Optional.empty(),
            Optional.empty());

    String ledgerIdPattern = "sandbox";

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
            RESOURCE_DIR,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Duration.ofMinutes(1),
            Duration.ofSeconds(10),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false,
            Optional.of("TestLedgerID"),
            Optional.empty());
    try {
      manager.start();
      assertThat(manager.getLedgerId(), is("TestLedgerID"));
    } finally {
      manager.stop();
    }
  }

  @Test
  public void managerReturnsSpecifiedPort()
      throws IOException, InterruptedException, TimeoutException {
    int specifiedPort = 5555;
    SandboxManager manager =
        new SandboxManager(
            RESOURCE_DIR,
            Optional.empty(),
            Optional.empty(),
            Optional.of(specifiedPort),
            Duration.ofMinutes(1),
            Duration.ofSeconds(10),
            new String[0],
            DAR_PATH,
            (_ignore1, _ignore2) -> {},
            false,
            Optional.of("TestLedgerID"),
            Optional.empty());
    try {
      manager.start();
      assertThat(manager.getPort(), is(specifiedPort));
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
