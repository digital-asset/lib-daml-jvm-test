package com.digitalasset.testing.ledger.clock;

import java.time.Instant;
import java.util.function.Supplier;

public class FixedTimeProvider implements TimeProvider {
  public static Supplier<TimeProvider> factory(Instant fixedTime) {
    return () -> new FixedTimeProvider(fixedTime);
  }

  private volatile Instant fixedTime;

  public FixedTimeProvider(Instant fixedTime) {
    this.fixedTime = fixedTime;
  }

  @Override
  public Instant getCurrentTime() {
    return fixedTime;
  }

  @Override
  public void setCurrentTime(Instant time) {
    fixedTime = time;
  }
}
