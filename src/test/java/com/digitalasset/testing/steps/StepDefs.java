/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.steps;

import com.digitalasset.testing.cucumber.steps.LedgerInteractions;
import com.digitalasset.testing.cucumber.utils.Config;

public class StepDefs extends LedgerInteractions {
  public StepDefs(Config config) {
    super(config);
  }
}
