/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.store

import java.util.concurrent.ConcurrentHashMap

import com.daml.ledger.javaapi.data.Value
import com.digitalasset.testing.Patterns.varName

class DefaultValueStore extends ValueStore {
  private val store = new ConcurrentHashMap[String, Value]()

  override def put(key: String, value: Value): Unit = {
    if (varName.r.unapplySeq(key).isEmpty) {
      throw new IllegalArgumentException(
        s"key [$key] doesn't match regex: $varName")
    }
    val oldValue = store.putIfAbsent(key, value)
    if (oldValue != null) {
      throw new IllegalStateException(
        "Key: " + key + ", Already exists in the store")
    }
  }

  override def get(key: String): Value = Option(store.get(key)) match {
    case Some(value) => value
    case None        => throw new NoSuchElementException(key)
  }

  override def remove(key: String): Unit = store.remove(key)
}
