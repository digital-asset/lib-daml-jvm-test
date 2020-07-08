/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.triggerservice.trigger;

import com.daml.ledger.javaapi.data.Party;

public class Builder {
  private String packageId;
  private String triggerName;
  private String ledgerHost = "localhost";
  private String party;

  public Builder packageId(String packageId) {
    this.packageId = packageId;
    return this;
  }

  public Builder triggerName(String triggerName) {
    this.triggerName = triggerName;
    return this;
  }

  public Builder ledgerHost(String ledgerHost) {
    this.ledgerHost = ledgerHost;
    return this;
  }

  public Builder party(Party party) {
    this.party = party.getValue();
    return this;
  }

  public Trigger build() {
    return new Trigger(packageId, triggerName, ledgerHost, party);
  }
}
