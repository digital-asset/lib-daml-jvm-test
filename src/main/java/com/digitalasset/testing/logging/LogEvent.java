/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.logging;

public interface LogEvent {
  String consolePrettyMsg();

  String filename();

  String filePrettyMsg();
}
