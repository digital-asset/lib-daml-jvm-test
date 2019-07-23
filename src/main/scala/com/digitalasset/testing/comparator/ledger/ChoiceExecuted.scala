/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator.ledger

import com.daml.ledger.javaapi.data.{
  Identifier => JavaIdentifier,
  Value => JavaValue
}
import com.digitalasset.ledger.api.v1.transaction.TreeEvent
import com.digitalasset.ledger.api.v1.value.Value
import com.digitalasset.testing.ast.toAst
import com.digitalasset.testing.comparator.MessageTester
import com.digitalasset.testing.comparator.MessageTester.Irrelevant
import grizzled.slf4j.Logging
import scalaz.syntax.monoid.ToSemigroupOps

object ChoiceExecuted extends Logging {
  def apply(
      expectedTemplate: JavaIdentifier,
      expectedContractId: String,
      expectedChoiceName: String,
      expectedChoiceArgumentOpt: Option[JavaValue]): MessageTester[TreeEvent] =
    new MessageTester[TreeEvent] {
      override def prettyPrintExpected: String = ""
      override def prettyPrintActual(event: TreeEvent): String = ""

      override def test(event: TreeEvent): MessageTester.ComparisonResult =
        if (event.kind.isExercised && compareIdentifier(
              event.getExercised.getTemplateId,
              expectedTemplate)) {
          val exercised = event.getExercised
          val contractId = exercised.contractId
          val choiceName = exercised.choice
          val choiceArgument = exercised.getChoiceArgument
          val compared = compareValues(expectedContractId,
                                       contractId,
                                       "contractId") |+|
            compareValues(expectedChoiceName, choiceName, "choicename")
          expectedChoiceArgumentOpt match {
            case Some(expectedChoiceArgument) =>
              compared |+| compareAst(
                toAst(
                  Value.parseFrom(expectedChoiceArgument.toProto.toByteArray)),
                toAst(choiceArgument))
            case None =>
              compared
          }
        } else {
          Irrelevant
        }
    }

}
