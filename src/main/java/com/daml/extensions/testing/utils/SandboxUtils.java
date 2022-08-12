/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.utils;

import com.daml.ledger.rxjava.DamlLedgerClient;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.daml.extensions.testing.JvmTestLibCommon.SANDBOX_PORT_RANGE;

public class SandboxUtils {
  private static final Path DAML_YAML = Paths.get("daml.yaml");
  private static Range<Integer> SANDBOX_PORT_BAD_RANGE = Range.closed(6865, 6867);

  private static final AtomicInteger SANDBOX_PORT_COUNTER =
      new AtomicInteger(SANDBOX_PORT_RANGE.lowerEndpoint());

  public static int getSandboxPort() {
    return SANDBOX_PORT_COUNTER.updateAndGet(
        p -> {
          while (SANDBOX_PORT_BAD_RANGE.contains(p)) {
            ++p;
          }
          if (SANDBOX_PORT_RANGE.contains(p)) {
            return p + 1;
          } else {
            return SANDBOX_PORT_RANGE.lowerEndpoint();
          }
        });
  }

  public static void waitForSandbox(DamlLedgerClient client, Duration waitTimeout, Logger logger)
      throws TimeoutException {
    boolean connected = false;
    Stopwatch time = Stopwatch.createStarted();
    while (!connected && time.elapsed().compareTo(waitTimeout) <= 0) {
      try {
        client.connect();
        connected = true;
      } catch (Exception ignored) {
        if (!ignored.getMessage().contains("UNAVAILABLE: io exception")) {
          // Sandbox hasn't started yet
          throw ignored;
        }
        try {
          logger.info("Waiting for sandbox...");
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (connected) logger.info("Connected to sandbox.");
    else throw new TimeoutException("Can't connect to sandbox");
  }

  public static boolean isDamlRoot(Path path) {
    try {
      return Files.isDirectory(path) && Files.list(path).anyMatch(p -> p.endsWith(DAML_YAML));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
