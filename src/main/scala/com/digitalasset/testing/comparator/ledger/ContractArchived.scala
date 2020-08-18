/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator.ledger

import com.daml.ledger.javaapi.data.{ContractId, ExercisedEvent, TreeEvent}
import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.comparator.MessageTester.Irrelevant
import grizzled.slf4j.Logging

object ContractArchived extends Logging {
  def apply(expectedTemplate: String,
            expectedContractId: ContractId): MessageTester[TreeEvent] =
    new MessageTester[TreeEvent] {
      override def prettyPrintExpected: String =
        pretty(expectedTemplate, expectedContractId.getValue)

      override def prettyPrintActual(event: TreeEvent): String =
        event match {
          case e: ExercisedEvent if e.isConsuming =>
            pretty(
              s"${e.getTemplateId.getModuleName}:${e.getTemplateId.getEntityName}",
              e.getContractId)

          case _ => "irrelevant"
        }

      override def test(event: TreeEvent): MessageTester.ComparisonResult =
        event match {
          case e: ExercisedEvent if e.isConsuming =>
            compareValues(expectedContractId.getValue,
                          e.getContractId,
                          "contractId")
          case _ => Irrelevant
        }
    }

  private def pretty(template: String, contractId: String) =
    s"Contract $template with id $contractId archived."
}
