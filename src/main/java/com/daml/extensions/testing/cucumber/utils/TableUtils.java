/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Copyright (c) 2020, Digital Asset (Switzerland) GmbH and/or its affiliates.
// All rights reserved.

package com.daml.extensions.testing.cucumber.utils;

import com.daml.extensions.testing.ledger.SandboxManager;
import com.daml.ledger.javaapi.data.DamlRecord;
import com.daml.daml_lf_dev.DamlLf1;
import com.daml.ledger.javaapi.data.DamlRecord;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.daml.extensions.testing.Dsl.*;
import static com.daml.extensions.testing.utils.PackageUtils.getTypePrim;

public class TableUtils {
  public static DamlRecord fieldsToArgs(
      List<String> args,
      List<DamlLf1.FieldWithType> fields,
      DamlLf1.Package lfPackage,
      SandboxManager sandboxManager) {
    if (args.size() != fields.size()) {
      throw new IllegalArgumentException(
          "Wrong number of actual arguments: " + args.size() + " (formal: " + fields.size() + ")");
    }
    LinkedList<DamlRecord.Field> fieldList = new LinkedList<>();
    HashMap<String, String> m = new HashMap<>();
    for (int i = 0; i < args.size(); i++) {
      DamlLf1.Type fieldType = fields.get(i).getType();
      DamlLf1.Type.Prim prim = getTypePrim(fieldType, lfPackage);
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
            fieldList.addLast(field(sandboxManager.getPartyId(arg)));
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
    return new DamlRecord(fieldList);
  }
}
