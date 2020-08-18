/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator.ledger

import com.daml.ledger.javaapi.data.{
  ExercisedEvent,
  Identifier,
  TreeEvent,
  Value
}
import com.digitalasset.testing.ast.toAst
import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.comparator.MessageTester.Irrelevant
import grizzled.slf4j.Logging
import scalaz.syntax.monoid.ToSemigroupOps

object ChoiceExecuted extends Logging {
  def apply(
      expectedTemplate: Identifier,
      expectedContractId: String,
      expectedChoiceName: String,
      expectedChoiceArgumentOpt: Option[Value]): MessageTester[TreeEvent] =
    new MessageTester[TreeEvent] {
      override def prettyPrintExpected: String = ""
      override def prettyPrintActual(event: TreeEvent): String = ""

      override def test(event: TreeEvent): MessageTester.ComparisonResult = {
        event match {
          case exercised: ExercisedEvent
              if exercised.getTemplateId == expectedTemplate =>
            val contractId = exercised.getContractId
            val choiceName = exercised.getChoice
            val choiceArgument = exercised.getChoiceArgument
            val compared =
              compareValues(expectedContractId, contractId, "contractId") |+|
                compareValues(expectedChoiceName, choiceName, "choicename")

            expectedChoiceArgumentOpt match {
              case Some(expectedChoiceArgument) =>
                compared |+| compareAst(toAst(expectedChoiceArgument),
                                        toAst(choiceArgument))
              case None =>
                compared
            }
          case _ => Irrelevant
        }
      }
    }
}
