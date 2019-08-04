/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing

import java.time.Instant

import com.digitalasset.ledger.api.v1.value.Value.Sum
import com.digitalasset.ledger.api.v1.value.{
  Record,
  Value,
  Variant,
  Optional => ProtoOptional
}

import com.daml.ledger.javaapi.{data => jdata}

import scala.collection.JavaConverters._

import com.digitalasset.testing.Patterns.{NoneCtor, SomeCtor}

package object ast {
  def toAst(v: Value): Ast = v.sum match {
    case Sum.Record(Record(_, fields)) =>
      Ast.Seq(fields.map(f => toAst(f.getValue)))
    case Sum.Variant(Variant(_, constructor, Some(value))) =>
      Ast.Map(Map(constructor -> toAst(value)))
    case Sum.Variant(Variant(_, constructor, None)) =>
      Ast.Map(Map(constructor -> Ast.Null))
    case Sum.Bool(value)       => Ast.Value(value.toString)
    case Sum.Empty             => Ast.Null
    case Sum.ContractId(value) => Ast.Value(value)
    case Sum.List(value)       => Ast.Seq(value.elements.map(toAst))
    case Sum.Int64(value)      => Ast.Value(value.toString)
    case Sum.Decimal(value)    => Ast.Value(value.toString)
    case Sum.Text(value)       => Ast.Value(value)
    case Sum.Timestamp(value) =>
      Ast.Value(Instant.ofEpochMilli(value / 1000).toString)
    case Sum.Party(value) => Ast.Value(value)
    case Sum.Unit(_)      => Ast.Null
    case Sum.Optional(ProtoOptional(Some(value))) =>
      Ast.Map(Map(SomeCtor -> toAst(value)))
    case Sum.Optional(ProtoOptional(None)) => Ast.Map(Map(NoneCtor -> Ast.Null))
    case Sum.Date(d)                       => Ast.Value(d.toString)
  }

  def toAstJ(v: jdata.Value): Ast = v match {
    case null => Ast.Null
    case r: jdata.Record => Ast.Seq(r.getFields.asScala.map(f => toAstJ(f.getValue)))
    case v: jdata.Variant => Ast.Map(Map(v.getConstructor -> toAstJ(v.getValue)))
    case b: jdata.Bool => Ast.Value(b.getValue.toString)
    case c: jdata.ContractId => Ast.Value(c.getValue)
    case l: jdata.DamlList => Ast.Seq(l.getValues.asScala.map(toAstJ))
    case i: jdata.Int64 => Ast.Value(i.getValue.toString)
    case d: jdata.Decimal => Ast.Value(d.getValue.toString)
    case t: jdata.Text => Ast.Value(t.getValue)
    case t: jdata.Timestamp => Ast.Value(t.getValue.toString)
    case p: jdata.Party => Ast.Value(p.getValue)
    case u: jdata.Unit => Ast.Null
    case o: jdata.DamlOptional if o.getValue.isPresent => Ast.Map(Map(SomeCtor -> toAstJ(o.getValue.get))) //map(v => toAstJ(v)).orElseGet(() => Ast.Null)))
    case o: jdata.DamlOptional  => Ast.Map(Map(SomeCtor -> Ast.Null))
    case d: jdata.Date => Ast.Value(d.getValue.toString)
  }
}
