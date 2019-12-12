/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils;

import com.daml.ledger.rxjava.DamlLedgerClient;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class SandboxUtils {
  private static Range<Integer> SANDBOX_PORT_RANGE = Range.closed(6860, 6890);
  private static final AtomicInteger SANDBOX_PORT_COUNTER =
      new AtomicInteger(SANDBOX_PORT_RANGE.lowerEndpoint());

  public static int getSandboxPort() {
    return SANDBOX_PORT_COUNTER.updateAndGet(
        p -> {
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
    int attempts = 0;
    while (!connected && time.elapsed().compareTo(waitTimeout) <= 0) {
      try {
        client.connect();
        connected = true;
      } catch (Exception ignored) {
        if (!ignored.getMessage().contains("UNAVAILABLE")) {
          // Sandbox hasn't started yet
          throw ignored;
        }
        try {
          logger.info("Waiting for sandbox...");
          TimeUnit.SECONDS.sleep(2 * attempts);
          attempts += 1;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    if (connected) logger.info("Connected to sandbox.");
    else throw new TimeoutException("Can't connect to sandbox");
  }
}
