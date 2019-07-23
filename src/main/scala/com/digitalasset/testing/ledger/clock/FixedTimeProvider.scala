/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock

import java.time.Instant

import io.grpc.ManagedChannel

class FixedTimeProviderFactory(private val fixedTime: Instant)
    extends TimeProviderFactory {
  override def getTimeProvider(channel: ManagedChannel,
                               ledgerId: String): TimeProvider =
    new FixedTimeProvider(fixedTime)
}

class FixedTimeProvider(private var fixedTime: Instant) extends TimeProvider {
  override def getCurrentTime: Instant = fixedTime

  override def setCurrentTime(time: Instant): Unit = fixedTime = time
}
