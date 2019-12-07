/*
 * Copyright 2019 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.digitalasset.testing.utils;

import com.daml.ledger.javaapi.data.GetPackageResponse;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.rxjava.PackageClient;
import com.digitalasset.daml_lf_dev.DamlLf;
import com.digitalasset.daml_lf_dev.DamlLf1;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PackageUtils {
  private static final ConcurrentHashMap<DamlLf1.DottedName, String> packageNames =
      new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, Identifier> identifiers =
      new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, DataType> dataTypes = new ConcurrentHashMap<>();

  public static class TemplateType {
    public final Identifier identifier;
    public final List<DamlLf1.FieldWithType> createFields;
    public final Map<String, List<DamlLf1.FieldWithType>> choices;

    TemplateType(
        Identifier identifier,
        List<DamlLf1.FieldWithType> createFields,
        Map<String, List<DamlLf1.FieldWithType>> m) {
      this.identifier = identifier;
      this.createFields = createFields;
      this.choices = m;
    }
  }

  private static class DataType {
    private Optional<Map<String, DamlLf1.Type>> choices = Optional.empty();
    private List<DamlLf1.FieldWithType> fieldList = null;

    public DataType(DamlLf1.Module mod, DamlLf1.DefDataType dataType) {
      if (dataType.hasRecord()) {
        fieldList = dataType.getRecord().getFieldsList();
        choices = getChoices(mod, dataType.getNameDname());
      }
    }

    public boolean isTemplate() {
      return fieldList != null && choices.isPresent();
    }

    public boolean hasFields() {
      return fieldList != null;
    }

    public List<DamlLf1.FieldWithType> getCreateFields() {
      return fieldList;
    }

    public Optional<Map<String, DamlLf1.Type>> getTemplateChoices() {
      return choices;
    }
  }

  public static String findPackage(DamlLedgerClient ledgerClient, DamlLf1.DottedName packageName)
      throws InvalidProtocolBufferException {
    String strName = packageNames.get(packageName);
    if (strName != null) {
      return strName;
    } else {
      PackageClient pkgClient = ledgerClient.getPackageClient();
      Iterable<String> pkgs = pkgClient.listPackages().blockingIterable();
      for (String pkgId : pkgs) {
        GetPackageResponse pkgResp = pkgClient.getPackage(pkgId).blockingGet();
        DamlLf.ArchivePayload archivePl =
            DamlLf.ArchivePayload.parseFrom(pkgResp.getArchivePayload());
        List<DamlLf1.Module> mods = archivePl.getDamlLf1().getModulesList();
        for (DamlLf1.Module mod : mods) {
          if (mod.getNameDname().equals(packageName)) {
            packageNames.put(packageName, pkgId);
            return pkgId;
          }
        }
      }
    }
    throw new IllegalArgumentException("No package found " + packageName);
  }

  public static TemplateType findTemplate(DamlLedgerClient ledgerClient, String moduleAndEntityName)
      throws InvalidProtocolBufferException {
    String[] parts = moduleAndEntityName.split(":");

    if (parts.length != 2) {
      throw new IllegalArgumentException(
          "Malformed module and entity name: " + moduleAndEntityName);
    }

    String moduleName = parts[0];
    DataType dt = findDataType(ledgerClient, moduleAndEntityName);
    if (dt.isTemplate()) {
      Map<String, List<DamlLf1.FieldWithType>> m = new HashMap<>();
      for (Map.Entry<String, DamlLf1.Type> choiceArgEntry :
          dt.getTemplateChoices().get().entrySet()) {
        String choiceArgName = choiceArgEntry.getKey();
        DamlLf1.Type choiceArgType = choiceArgEntry.getValue();
        String choiceDataTypeName =
            dottedNameToString(choiceArgType.getCon().getTycon().getNameDname());
        String choiceDataTypeFqn = toFqn(moduleName, choiceDataTypeName);
        if (choiceArgName.equals("Archive") || choiceDataTypeName.equals("Archive")) {
          choiceDataTypeFqn = "DAInternalTemplate:Archive";
        }
        DataType choiceArgDataType = findDataType(ledgerClient, choiceDataTypeFqn);
        if (choiceArgDataType.hasFields()) {
          m.put(choiceArgName, choiceArgDataType.fieldList);
        } else {
          throw new IllegalStateException("Choice " + choiceArgName + " has no fields?");
        }
      }
      return new TemplateType(identifiers.get(moduleAndEntityName), dt.getCreateFields(), m);
    }
    throw new IllegalArgumentException("No template found with the name " + moduleAndEntityName);
  }

  public static String dottedNameToString(DamlLf1.DottedName name) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < name.getSegmentsCount(); i++) {
      b.append(name.getSegments(i));
    }
    return b.toString();
  }

  private static DataType findDataType(DamlLedgerClient ledgerClient, String moduleAndEntityName)
      throws InvalidProtocolBufferException {
    assert !moduleAndEntityName.isEmpty();
    DataType dt = dataTypes.get(moduleAndEntityName);
    if (dt != null) {
      return dt;
    } else {
      // Init or reinit the cache...
      initCache(ledgerClient);
      // Try again and throw.
      dt = dataTypes.get(moduleAndEntityName);
      if (dt != null) {
        return dt;
      }
      throw new IllegalArgumentException(
          "No datatype found with the name '" + moduleAndEntityName + "'");
    }
  }

  private static void initCache(DamlLedgerClient ledgerClient)
      throws InvalidProtocolBufferException {
    PackageClient pkgClient = ledgerClient.getPackageClient();
    Iterable<String> pkgs = pkgClient.listPackages().blockingIterable();
    for (String pkgId : pkgs) {
      GetPackageResponse pkgResp = pkgClient.getPackage(pkgId).blockingGet();
      DamlLf.ArchivePayload archivePl =
          DamlLf.ArchivePayload.parseFrom(pkgResp.getArchivePayload());
      List<DamlLf1.Module> mods = archivePl.getDamlLf1().getModulesList();
      for (DamlLf1.Module mod : mods) {
        for (DamlLf1.DefDataType dataType : mod.getDataTypesList()) {
          String modN = dottedNameToString(mod.getNameDname());
          String dataN = dottedNameToString(dataType.getNameDname());
          String moduleAndEntityName = toFqn(modN, dataN);
          Identifier id = new Identifier(pkgId, modN, dataN);
          identifiers.put(moduleAndEntityName, id);
          dataTypes.put(moduleAndEntityName, new DataType(mod, dataType));
        }
      }
    }
  }

  private static String toFqn(String moduleName, String entityName) {
    Objects.requireNonNull(moduleName);
    Objects.requireNonNull(entityName);
    assert !moduleName.isEmpty() && !entityName.isEmpty();
    return moduleName + ":" + entityName;
  }

  private static Optional<Map<String, DamlLf1.Type>> getChoices(
      DamlLf1.Module mod, DamlLf1.DottedName dataTypeName) {
    Optional<DamlLf1.DefTemplate> template =
        mod.getTemplatesList()
            .stream()
            .filter(t -> t.getTyconDname().equals(dataTypeName))
            .findFirst();
    if (template.isPresent()) {
      HashMap<String, DamlLf1.Type> m = new HashMap<>();
      for (DamlLf1.TemplateChoice choice : template.get().getChoicesList()) {
        m.put(choice.getNameStr(), choice.getArgBinder().getType());
      }
      return Optional.of(m);
    } else {
      return Optional.empty();
    }
  }
}
