/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger.clock;

import java.time.Instant;

public interface TimeProvider {
  Instant getCurrentTime();

  void setCurrentTime(Instant time);
}
