/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Copyright (c) 2019, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.digitalasset.testing.cucumber.utils;

import com.daml.ledger.javaapi.data.Record;
import com.daml.daml_lf_dev.DamlLf1;

import java.util.*;

import static com.digitalasset.testing.Dsl.*;

public class TableUtils {
  public static Record fieldsToArgs(List<String> args, List<DamlLf1.FieldWithType> fields) {
    if (args.size() != fields.size()) {
      throw new IllegalArgumentException(
          "Wrong number of actual arguments: " + args.size() + " (formal: " + fields.size() + ")");
    }
    LinkedList<Record.Field> fieldList = new LinkedList<>();
    HashMap<String, String> m = new HashMap<>();
    for (int i = 0; i < args.size(); i++) {
      DamlLf1.Type.Prim prim = fields.get(i).getType().getPrim();
      String arg = args.get(i);
      System.out.println(
          "Formal arg: "
              + prim.getPrim().getValueDescriptor().getFullName()
              + " Actual arg: "
              + arg);
      if (prim.getArgsList().size() == 0) {
        switch (prim.getPrim()) {
          case BOOL:
            fieldList.addLast(field(bool(arg)));
            break;
          case TEXT:
            fieldList.addLast(field(text(arg)));
            break;
          case INT64:
            fieldList.addLast(field(integer(arg)));
            break;
          case PARTY:
            fieldList.addLast(field(party(arg)));
            break;
          case DECIMAL:
            fieldList.addLast(field(decimal(arg)));
            break;
          case CONTRACT_ID:
            fieldList.addLast(field(contractId(arg)));
            break;
          default:
            throw new IllegalArgumentException("Cannot handle type: " + prim.getPrim().getNumber());
        }
      } else {
        if (prim.getArgsList().size() == 1) {
          switch (prim.getPrim()) {
            case NUMERIC:
              fieldList.addLast(field(numeric(arg)));
              break;
            default:
              throw new IllegalArgumentException(
                  "Cannot handle parametric type: " + prim.getPrim().getNumber());
          }
        } else {
          throw new IllegalArgumentException(
              "Cannot handle parametric type: " + prim.getPrim().getNumber());
        }
      }
    }
    return new Record(fieldList);
  }
}
