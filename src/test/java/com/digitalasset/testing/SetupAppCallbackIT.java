package com.digitalasset.testing;

import com.daml.ledger.javaapi.data.*;
import com.digitalasset.daml_lf.DamlLf1;
import com.digitalasset.testing.junit4.Sandbox;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.digitalasset.testing.Dsl.*;
import static com.digitalasset.testing.TestCommons.*;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SetupAppCallbackIT {
  private static AtomicBoolean boolFlag = new AtomicBoolean(false);

  private static Sandbox sandbox =
      Sandbox.builder()
          .dar(DAR_PATH)
          .parties(ALICE.getValue(), BOB.getValue(), CHARLIE.getValue())
          .setupAppCallback(client -> boolFlag.set(true))
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();

  @Test
  public void testSetupAppCallback() {
    assertTrue("Setup should set the boolFlag.", boolFlag.get());
  }
}
