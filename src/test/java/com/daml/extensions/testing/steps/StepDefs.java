/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.steps;

import com.daml.extensions.testing.cucumber.steps.LedgerInteractions;
import com.daml.extensions.testing.cucumber.utils.Config;

public class StepDefs extends LedgerInteractions {
  public StepDefs(Config config) {
    super(config);
  }
}
