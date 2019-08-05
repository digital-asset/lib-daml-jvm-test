/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock;

import com.digitalasset.ledger.api.v1.testing.TimeServiceGrpc;
import com.digitalasset.ledger.api.v1.testing.TimeServiceOuterClass;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Monitor;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SandboxTimeProvider implements TimeProvider {
  private static final Logger logger = LoggerFactory.getLogger(SandboxTimeProvider.class);

  public static Supplier<TimeProvider> factory(
      TimeServiceGrpc.TimeServiceStub stub, String ledgerId) {
    return () -> {
      logger.debug("Starting SandboxTimeProvider");
      SandboxTimeProvider p = new SandboxTimeProvider(stub, ledgerId);
      Monitor monitor = new Monitor();
      Monitor.Guard started = monitor.newGuard(() -> p.running.get());
      try {
        TimeServiceOuterClass.GetTimeRequest req =
            TimeServiceOuterClass.GetTimeRequest.newBuilder().setLedgerId(ledgerId).build();
        stub.getTime(
            req,
            new StreamObserver<TimeServiceOuterClass.GetTimeResponse>() {
              public void onNext(TimeServiceOuterClass.GetTimeResponse value) {
                logger.debug("SandboxTimeProvider received new time {}", value.getCurrentTime());
                monitor.enter();
                try {
                  p.setActualTime(value.getCurrentTime());
                } finally {
                  monitor.leave();
                }
              }

              public void onError(Throwable t) {
                logger.warn("SandboxTimeProvider request received an error", t);
                p.stop(t);
              }

              public void onCompleted() {
                logger.warn("SandboxTimeProvider finished");
                p.stop(null);
              }
            });

        monitor.enter();
        try {
          boolean running = monitor.waitFor(started, Duration.ofSeconds(30));
          Preconditions.checkState(running, "SandboxTimeProvider start failed");
        } finally {
          monitor.leave();
        }
      } catch (InterruptedException ignored) {
      }

      return p;
    };
  }

  private TimeServiceGrpc.TimeServiceStub stub;
  private final String ledgerId;

  private AtomicReference<Instant> actualTime = new AtomicReference<>(Instant.EPOCH);
  private AtomicBoolean running = new AtomicBoolean(false);

  private SandboxTimeProvider(TimeServiceGrpc.TimeServiceStub stub, String ledgerId) {
    this.stub = stub;
    this.ledgerId = ledgerId;
  }

  private synchronized void setActualTime(Timestamp ts) {
    actualTime.set(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()));
    running.set(true);
  }

  @Override
  public Instant getCurrentTime() {
    ensureRunning();
    return actualTime.get();
  }

  private synchronized void stop(Throwable t) {
    running.set(false);
    stub = null;
    if (t != null) {
      logger.error("Time service stopped", t);
    } else {
      logger.info("Time service stopped");
    }
  }

  private void ensureRunning() {
    if (!running.get()) throw new IllegalStateException("TimeService is not running");
  }

  @Override
  public void setCurrentTime(Instant time) {
    ensureRunning();
    logger.debug("Setting new time {}", time);

    Instant at = actualTime.get();
    TimeServiceOuterClass.SetTimeRequest req =
        TimeServiceOuterClass.SetTimeRequest.newBuilder()
            .setLedgerId(ledgerId)
            .setCurrentTime(
                Timestamp.newBuilder()
                    .setNanos(at.getNano())
                    .setSeconds(at.getEpochSecond())
                    .build())
            .setNewTime(
                Timestamp.newBuilder()
                    .setNanos(time.getNano())
                    .setSeconds(time.getEpochSecond())
                    .build())
            .build();
    TimeServiceGrpc.newBlockingStub(stub.getChannel()).setTime(req);
  }
}
