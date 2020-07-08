/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.digitalasset.testing.triggerservice.trigger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trigger extends ExternalResource {
  private static final int defaultTriggerServicePort = 8088;

  private final Logger logger;
  private final String packageId;
  private final String triggerName;
  private final String ledgerHost;
  private final String party;

  private HttpClient httpClient = new HttpClient();

  Trigger(String packageId, String triggerName, String ledgerHost, String party) {
    this.packageId = packageId;
    this.triggerName = triggerName;
    this.ledgerHost = ledgerHost;
    this.party = party;
    this.logger =
        LoggerFactory.getLogger(
            String.format("%s: %s-%s", getClass().getCanonicalName(), triggerName, party));
  }

  public static Builder builder() {
    return new Builder();
  }

  private void start() throws Throwable {
    URL startUrl = getStartUrl(this.ledgerHost);
    String starterJsonBody = getTriggerStarterJsonBody();
    logger.info("Starting trigger.");
    httpClient.post(startUrl, starterJsonBody, getAuthString());
    logger.info("Trigger started.");
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    start();
  }

  @Override
  protected void after() {
    super.after();
  }

  public static URL getStartUrl(String ledgerHost) throws MalformedURLException {
    return new URL(String.format("http://%s:%s/v1/start", ledgerHost, defaultTriggerServicePort));
  }

  private String getAuthString() {
    String authString = String.format("%s:secret", this.party);
    String encodedAuthString = Base64.getEncoder().encodeToString(authString.getBytes());
    return String.format("Basic %s", encodedAuthString);
  }

  private String getTriggerStarterJsonBody() {
    return String.format(
        "{ \"triggerName\" : \"%s:%s\" }", this.packageId, this.triggerName);
  }
}
