/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock
import java.time.{Clock, Instant}

import io.grpc.ManagedChannel

object SystemTimeProviderFactory extends TimeProviderFactory {
  override def getTimeProvider(channel: ManagedChannel,
                               ledgerId: String): TimeProvider =
    new SystemTimeProvider()
}

class SystemTimeProvider extends TimeProvider {
  private val systemUtcClock = Clock.systemUTC()
  override def getCurrentTime: Instant = systemUtcClock.instant()

  def setCurrentTime(time: Instant): Unit =
    throw new UnsupportedOperationException(
      "System Time Provider: System time cannot be changed.")
}
