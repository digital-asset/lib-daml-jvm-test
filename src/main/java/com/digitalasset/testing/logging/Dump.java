/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dump {
  private static volatile String context;

  public static void dump(String logger, LogEvent event) {
    Logger log = LoggerFactory.getLogger(logger);
    if (log.isDebugEnabled()) {
      String contextString = context;
      if (contextString != null) {
        log.debug(event.consolePrettyMsg(), event, contextString);
      } else {
        log.debug("Skipping wire dump, the context is not provided");
      }
    }
  }

  public static synchronized void setContextString(String c) {
    context = c;
  }

  public static synchronized void clearContextString() {
    context = null;
  }
}
