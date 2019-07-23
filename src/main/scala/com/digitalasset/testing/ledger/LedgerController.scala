/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger

import com.digitalasset.ledger.api.v1.ledger_offset.LedgerOffset
import com.digitalasset.ledger.api.v1.ledger_offset.LedgerOffset.LedgerBoundary.LEDGER_BEGIN
import com.digitalasset.ledger.api.v1.ledger_offset.LedgerOffset.Value.Boundary

trait LedgerController {

  /**
    * If lazy initialization is desired, don't call this method explicitly.
    * Any actionable method will call start implicitly.
    */
  def start(parties: Array[String]): Unit =
    start(parties, LedgerOffset(Boundary(LEDGER_BEGIN)))
  def start(explicitParties: Seq[String], offset: LedgerOffset): Unit
  def stop(): Unit
}
