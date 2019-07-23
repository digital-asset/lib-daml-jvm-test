/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.logging

trait LogEvent {
  val consolePrettyMsg: String
  val filename: String
  val filePrettyMsg: String
}
