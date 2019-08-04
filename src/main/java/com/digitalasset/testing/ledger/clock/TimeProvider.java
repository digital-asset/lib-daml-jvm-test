package com.digitalasset.testing.ledger.clock;

import java.time.Instant;

public interface TimeProvider {
  Instant getCurrentTime();

  void setCurrentTime(Instant time);
}
