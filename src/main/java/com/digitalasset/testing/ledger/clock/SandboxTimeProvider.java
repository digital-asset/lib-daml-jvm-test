/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock;

import com.daml.ledger.api.v1.testing.TimeServiceGrpc;
import com.daml.ledger.api.v1.testing.TimeServiceOuterClass;

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

      TimeServiceOuterClass.GetTimeRequest req =
          TimeServiceOuterClass.GetTimeRequest.newBuilder().setLedgerId(ledgerId).build();
      stub.getTime(
          req,
          new StreamObserver<TimeServiceOuterClass.GetTimeResponse>() {
            public void onNext(TimeServiceOuterClass.GetTimeResponse value) {
              logger.debug("SandboxTimeProvider received new time {}", value.getCurrentTime());
              p.setActualTime(value.getCurrentTime());
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

      p.waitForTimeChange(Instant.EPOCH);
      logger.info("SandboxTimeProvider started");
      return p;
    };
  }

  private TimeServiceGrpc.TimeServiceStub stub;
  private final String ledgerId;
  private final Monitor monitor = new Monitor();

  private AtomicReference<Instant> actualTime = new AtomicReference<>(null);

  private SandboxTimeProvider(TimeServiceGrpc.TimeServiceStub stub, String ledgerId) {
    this.stub = stub;
    this.ledgerId = ledgerId;
  }

  private synchronized void setActualTime(Timestamp ts) {
    monitor.enter();
    try {
      actualTime.set(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()));
    } finally {
      monitor.leave();
    }
  }

  @Override
  public Instant getCurrentTime() {
    return actualTime.get();
  }

  private synchronized void stop(Throwable t) {
    stub = null;
    if (t != null) {
      logger.error("Time service stopped", t);
    } else {
      logger.info("Time service stopped");
    }
  }

  @Override
  public void setCurrentTime(Instant time) {
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

    waitForTimeChange(time);
  }

  private void waitForTimeChange(Instant time) {
    Monitor.Guard timeIsActualized =
        monitor.newGuard(
            () -> this.actualTime.get() != null && this.actualTime.get().compareTo(time) >= 0);
    try {
      if (monitor.enterWhen(timeIsActualized, Duration.ofSeconds(10))) {
        monitor.leave();
      } else {
        throw new IllegalStateException("Failed to set time.");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
