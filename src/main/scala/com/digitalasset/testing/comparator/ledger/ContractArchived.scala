/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator.ledger

import com.digitalasset.ledger.api.v1.event.ExercisedEvent
import com.digitalasset.ledger.api.v1.transaction.TreeEvent
import com.digitalasset.ledger.api.v1.transaction.TreeEvent.Kind
import com.digitalasset.testing.Patterns.Fqn
import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.comparator.MessageTester.Irrelevant
import grizzled.slf4j.Logging

object ContractArchived extends Logging {
  def apply(expectedTemplate: String,
            expectedContractId: String): MessageTester[TreeEvent] =
    new MessageTester[TreeEvent] {
      override def prettyPrintExpected: String =
        pretty(expectedTemplate, expectedContractId)

      override def prettyPrintActual(event: TreeEvent): String =
        event.kind match {
          case Kind.Exercised(
              ExercisedEvent(_,
                             contractId,
                             Some(Fqn(`expectedTemplate`)),
                             _,
                             _,
                             _,
                             _,
                             true,
                             _,
                             _,
                             _)) =>
            pretty(expectedTemplate, contractId)

          case _ => "irrelevant"
        }

      override def test(event: TreeEvent): MessageTester.ComparisonResult =
        event.kind match {
          case Kind.Exercised(
              ExercisedEvent(_,
                             contractId,
                             Some(Fqn(`expectedTemplate`)),
                             _,
                             _,
                             _,
                             _,
                             true,
                             _,
                             _,
                             _)) =>
            compareValues(expectedContractId, contractId, "contractId")

          case _ => Irrelevant
        }
    }

  private def pretty(template: String, contractId: String) =
    s"Contract $template with id $contractId archived."
}
