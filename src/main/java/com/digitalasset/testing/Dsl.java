/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing;

import com.daml.ledger.javaapi.data.DamlList;
import com.daml.ledger.javaapi.data.Decimal;
import com.daml.ledger.javaapi.data.Int64;
import com.daml.ledger.javaapi.data.Party;
import com.daml.ledger.javaapi.data.Record;
import com.daml.ledger.javaapi.data.Text;
import com.daml.ledger.javaapi.data.Value;
import java.math.BigDecimal;

public class Dsl {

  private static final String key = "internal-cid-query";
  private static final String recordKey = "internal-recordKey";

  public static Record record(Value... values) {
    Record.Field[] records = new Record.Field[values.length];
    for (int i = 0; i < values.length; i++) {
      records[i] = new Record.Field(values[i]);
    }
    return new Record(records);
  }

  public static Record record(Record.Field... fields) {
    return new Record(fields);
  }

  public static Record emptyRecord() {
    return new Record();
  }

  public static Record.Field field(String label, Value value) {
    return new Record.Field(label, value);
  }

  public static Party party(String name) {
    return new Party(name);
  }

  public static Text text(String t) {
    return new Text(t);
  }

  public static DamlList list(Value... values) {
    return new DamlList(values);
  }

  public static Decimal decimal(BigDecimal number) {
    return new Decimal(number);
  }

  public static Decimal decimal(String number) {
    return new Decimal(new BigDecimal(number));
  }

  public static Int64 int64(long l) {
    return new Int64(l);
  }
}
