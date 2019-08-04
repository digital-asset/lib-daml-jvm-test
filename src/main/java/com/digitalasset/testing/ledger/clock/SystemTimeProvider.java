package com.digitalasset.testing.ledger.clock;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

public class SystemTimeProvider implements TimeProvider {
  public static Supplier<TimeProvider> factory() {
    return SystemTimeProvider::new;
  }

  private static final Clock systemUtcClock = Clock.systemUTC();

  @Override
  public Instant getCurrentTime() {
    return systemUtcClock.instant();
  }

  @Override
  public void setCurrentTime(Instant time) {
    throw new UnsupportedOperationException("System Time Provider: System time cannot be changed.");
  }
}
