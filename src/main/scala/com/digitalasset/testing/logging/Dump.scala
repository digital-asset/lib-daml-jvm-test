/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.logging
import grizzled.slf4j.Logger

object Dump {
  def dump(logger: String, event: => LogEvent): Unit = {
    val log = Logger(logger)
    if (log.isDebugEnabled) {
      val contextString = getContextString
      if (contextString != null)
        Logger(logger).logger
          .debug(event.consolePrettyMsg, event: Any, getContextString: Any)
      else
        Logger[Dump.type]
          .debug("Skipping wire dump, the context is not provided")
    }
  }

  private var context: String = _
  def setContextString(context: String): Unit = synchronized {
    this.context = context
  }
  def getContextString: String = synchronized { context }
  def clearContextString(): Unit = synchronized { context = null }
}
