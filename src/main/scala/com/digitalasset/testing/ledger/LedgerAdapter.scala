/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ledger

import com.daml.ledger.javaapi.data.{
  Party,
  ContractId => JavaContractId,
  ExerciseCommand => JavaExerciseCommand,
  Identifier => JavaIdentifier,
  Record => JavaRecord,
  Value => JavaValue
}
import com.digitalasset.ledger.api.v1.transaction.TreeEvent
import com.digitalasset.testing.comparator.MessageTester

trait LedgerAdapter {
  def createContract(party: Party,
                     templateId: JavaIdentifier,
                     payload: JavaRecord): Unit
  def exerciseChoice(party: Party,
                     templateId: JavaIdentifier,
                     contractId: JavaContractId,
                     choice: String,
                     payload: JavaValue): Unit
  def exerciseChoice(party: Party, exerciseCmd: JavaExerciseCommand): Unit

  /**
    * Check that a specific ledger event is received (observed).
    * Some implementations may save the event in a store as well.
    * @param party party to act in the name of
    * @param eventTester a message tester that needs to match an observed event
    * @return Unit
    */
  def observeEvent(party: String, eventTester: MessageTester[TreeEvent]): Unit

  def assertDidntHappen(party: String,
                        eventTester: MessageTester[TreeEvent]): Unit
}
