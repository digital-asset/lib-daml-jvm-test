/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing

import com.daml.ledger.javaapi.data.Identifier

import scala.util.matching.Regex

object Patterns {
  val varName = "[a-z][a-zA-Z0-9_-]*"

  // interpolators
  val XegerMatchRegex: Regex = "\\{XEGER:((?:\\\\[{}\\\\]|[^{}\\\\])+?)\\}".r
  val ReferVariableRegex: Regex = s"\\{($varName)\\}".r
  val DictionaryRegex: Regex = s"\\{($varName):($varName)\\}".r

  // egress
  val IgnoreRegex: Regex = "\\{IGNORE\\}".r
  val CaptureVariableRegex: Regex = s"\\{CAPTURE:(.+?)\\}".r
  val RegexMatchRegex: Regex = "\\{REGEX:((?:\\\\[{}\\\\]|[^{}\\\\])+?)\\}".r

  def unescape(pattern: String): String =
    pattern.replaceAll("""\\([{}\\])""", "$1")

  /* Unapply patterns, used in pattern matching */

  object Fqn {
    def unapply(identifier: Identifier): Option[String] = Some(identifier.fqn)
  }

  /* Rich objects */

  implicit class IdentifierOps(id: Identifier) {
    def fqn: String = s"${id.getModuleName}:${id.getEntityName}"
    def entName: String = id.getEntityName
  }

  val SomeCtor = "Some"
  val NoneCtor = "None"
}
