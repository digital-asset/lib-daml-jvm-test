/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing

import com.daml.ledger.javaapi.data._
import com.daml.extensions.testing.Patterns.SomeCtor

import scala.collection.JavaConverters._

package object ast {
  def toAst(v: Value): Ast = v match {
    case null          => Ast.Null
    case r: Record     => Ast.Seq(r.getFields.asScala.map(f => toAst(f.getValue)))
    case v: Variant    => Ast.Map(Map(v.getConstructor -> toAst(v.getValue)))
    case b: Bool       => Ast.Value(b.getValue.toString)
    case c: ContractId => Ast.Value(c.getValue)
    case l: DamlList   => Ast.Seq(l.getValues.asScala.map(toAst))
    case i: Int64      => Ast.Value(i.getValue.toString)
    case d: Decimal    => Ast.Value(d.getValue.toString)
    case n: Numeric    => Ast.Value(n.getValue.toString)
    case t: Text       => Ast.Value(t.getValue)
    case t: Timestamp  => Ast.Value(t.getValue.toString)
    case p: Party      => Ast.Value(p.getValue)
    case u: Unit       => Ast.Null
    case o: DamlOptional if o.getValue.isPresent =>
      Ast.Map(Map(SomeCtor -> toAst(o.getValue.get))) //map(v => toAstJ(v)).orElseGet(() => Ast.Null)))
    case o: DamlOptional => Ast.Map(Map(SomeCtor -> Ast.Null))
    case d: Date         => Ast.Value(d.getValue.toString)
  }
}
