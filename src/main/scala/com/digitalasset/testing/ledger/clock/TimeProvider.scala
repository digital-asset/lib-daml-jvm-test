/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock

import java.time.Instant

import io.grpc.ManagedChannel

trait TimeProviderFactory {
  def getTimeProvider(channel: ManagedChannel, ledgerId: String): TimeProvider
}

trait TimeProvider {
  def getCurrentTime: Instant
  def setCurrentTime(time: Instant): Unit
}
