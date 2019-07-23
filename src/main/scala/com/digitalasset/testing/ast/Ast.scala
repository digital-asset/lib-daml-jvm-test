/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.ast

/**
  * ADTs representing typical hierarchical structure. This is the most generic common representation for YAML, JSON, and
  * other type-less hierarchical structures. DAML contracts can be represented using just these constructs.
  */
sealed trait Ast
object Ast {
  case class Seq(value: collection.Seq[Ast]) extends Ast
  case class Map(value: collection.Map[String, Ast]) extends Ast
  case class Value(value: String) extends Ast
  case object Null extends Ast
}
