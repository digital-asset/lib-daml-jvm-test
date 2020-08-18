/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils;

import com.daml.ledger.javaapi.data.Value;

import java.util.Objects;

public class ContractWithId<Cid> {
  public final Cid contractId;
  public final Value record;

  public ContractWithId(Cid contractId, Value record) {
    this.contractId = contractId;
    this.record = record;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ContractWithId that = (ContractWithId) o;
    return contractId.equals(that.contractId) && record.equals(that.record);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contractId, record);
  }
}
