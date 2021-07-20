/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.comparator.ledger

import com.daml.ledger.javaapi.data.{
  CreatedEvent,
  Identifier,
  DamlRecord,
  TreeEvent
}
import com.daml.extensions.testing.comparator.MessageTester
import com.daml.extensions.testing.comparator.MessageTester.{Irrelevant, Same}
import scalaz.syntax.monoid.ToSemigroupOps
import com.daml.extensions.testing.ast.toAst

object ContractCreated {
  private def apply(
      expectedTemplate: Identifier,
      expectedContractId: String,
      captureOrexpectedArgumentsOpt: Either[String, Option[DamlRecord]])
    : MessageTester[TreeEvent] =
    new MessageTester[TreeEvent] {
      override def prettyPrintExpected: String =
        s"${expectedTemplate} - ${expectedContractId}"

      override def prettyPrintActual(event: TreeEvent): String = event.toString

      override def test(event: TreeEvent): MessageTester.ComparisonResult = {
        event match {
          case event1: CreatedEvent
              if event1.getTemplateId == expectedTemplate =>
            val contractId = event1.getContractId
            val createArguments = event1.getArguments

            val valueDiff =
              compareValues(expectedContractId, contractId, "contractId")
            captureOrexpectedArgumentsOpt match {
              case Right(Some(expectedArguments)) =>
                valueDiff |+| compareAst(toAst(expectedArguments),
                                         toAst(createArguments))
              case Right(None) => valueDiff
              case Left(capture) =>
                valueDiff |+| Same(capture -> createArguments)
            }
          case _ =>
            Irrelevant
        }
      }
    }

  def expectContract(expectedTemplate: Identifier,
                     expectedContractId: String): MessageTester[TreeEvent] =
    apply(expectedTemplate, expectedContractId, Right(None))

  def expectContractWithArguments(
      expectedTemplate: Identifier,
      expectedContractId: String,
      expectedArguments: DamlRecord): MessageTester[TreeEvent] =
    apply(expectedTemplate, expectedContractId, Right(Some(expectedArguments)))

  def capture(expectedTemplate: Identifier,
              expectedContractId: String,
              capture: String): MessageTester[TreeEvent] =
    apply(expectedTemplate, expectedContractId, Left(capture))

}
