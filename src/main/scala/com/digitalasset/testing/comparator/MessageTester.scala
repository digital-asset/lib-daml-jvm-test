/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.comparator

import com.digitalasset.ledger.api.v1.value.Value
import com.digitalasset.testing.comparator.MessageTester.ComparisonResult
import scalaz.Monoid

trait MessageTester[T] {
  def test(t: T): ComparisonResult
  def prettyPrintExpected: String
  def prettyPrintActual(t: T): String
}

object MessageTester {
  sealed trait ComparisonResult extends Product with Serializable {
    def success: Boolean = this match {
      case _: Same => true
      case _       => false
    }
  }

  case class Same(capturedValues: List[(String, Value)])
      extends ComparisonResult
  case class Diff(diffs: List[String]) extends ComparisonResult
  case object Irrelevant extends ComparisonResult
  case class Error(msg: String) extends ComparisonResult

  object Same {
    def apply(capturedValues: (String, Value)*): Same =
      new Same(capturedValues.toList)
  }
  object Diff { def apply(diffs: String*): Diff = new Diff(diffs.toList) }

  implicit val comparisonResultMonoid: Monoid[ComparisonResult] =
    new Monoid[ComparisonResult] {
      override def zero: ComparisonResult = Irrelevant
      override def append(left: ComparisonResult,
                          right: => ComparisonResult): ComparisonResult =
        (left, right) match {
          case (e: Error, _)      => e
          case (_, e: Error)      => e
          case (Irrelevant, r)    => r
          case (l, Irrelevant)    => l
          case (_: Same, r: Diff) => r
          case (l: Diff, _: Same) => l
          case (Diff(l), Diff(r)) => Diff(l ++ r)
          case (Same(l), Same(r)) => Same(l ++ r)
        }
    }
}
