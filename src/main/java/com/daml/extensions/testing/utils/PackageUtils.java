/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.utils;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.daml_lf_dev.DamlLf1;
import com.daml.ledger.javaapi.data.GetPackageResponse;
import com.daml.ledger.javaapi.data.Identifier;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.daml.ledger.rxjava.PackageClient;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    public DataType(DamlLf1.Module mod, DamlLf1.DefDataType dataType, DamlLf1.Package lfPackage) {
      if (dataType.hasRecord()) {
        fieldList = dataType.getRecord().getFieldsList();
        choices = getChoices(mod, dataType, lfPackage);
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

  private static DamlLf1.DottedName getInternedName(int internedNameId, DamlLf1.Package lfPackage) {
    DamlLf1.InternedDottedName internedDottedModuleName =
        lfPackage.getInternedDottedNames(internedNameId);
    List<String> actualModuleNameList =
        internedDottedModuleName
            .getSegmentsInternedStrList()
            .stream()
            .map(lfPackage::getInternedStrings)
            .collect(Collectors.toList());
    return DamlLf1.DottedName.newBuilder().addAllSegments(actualModuleNameList).build();
  }

  private static DamlLf1.DottedName getModuleName(DamlLf1.Module mod, DamlLf1.Package lfPackage) {
    DamlLf1.DottedName modN;
    if (mod.hasNameDname()) { // DamlLf version <= 1.6 or nameCase_ == 1
      modN = mod.getNameDname();
    } else {
      modN = getInternedName(mod.getNameInternedDname(), lfPackage);
    }
    return modN;
  }

  private static DamlLf1.DottedName getDataTypeName(
      DamlLf1.DefDataType dataType, DamlLf1.Package lfPackage) {
    DamlLf1.DottedName dataN;
    if (dataType.hasNameDname()) { // DamlLf version <= 1.6 or nameCase_ == 1
      dataN = dataType.getNameDname();
    } else {
      dataN = getInternedName(dataType.getNameInternedDname(), lfPackage);
    }
    return dataN;
  }

  private static DamlLf1.DottedName getTypeConName(
      DamlLf1.TypeConName tycon, DamlLf1.Package lfPackage) {
    DamlLf1.DottedName tyconN;
    if (tycon.hasNameDname()) { // DamlLf version <= 1.6 or nameCase_ == 2
      tyconN = tycon.getNameDname();
    } else {
      tyconN = getInternedName(tycon.getNameInternedDname(), lfPackage);
    }
    return tyconN;
  }

  private static DamlLf1.DottedName getDefTemplateName(
      DamlLf1.DefTemplate dt, DamlLf1.Package lfPackage) {
    DamlLf1.DottedName dtN;
    if (dt.hasTyconDname()) { // DamlLf version <= 1.6 or nameCase_ == 1
      dtN = dt.getTyconDname();
    } else {
      dtN = getInternedName(dt.getTyconInternedDname(), lfPackage);
    }
    return dtN;
  }

  private static String getInternedString(int internedNameId, DamlLf1.Package lfPackage) {
    return lfPackage.getInternedStrings(internedNameId);
  }

  private static DamlLf1.Type.Con getTypeCon(DamlLf1.Type t, DamlLf1.Package lfPackage) {
    if (t.hasCon()) {
      return t.getCon();
    } else {
      DamlLf1.Type resolved = lfPackage.getInternedTypes(t.getInterned());
      return resolved.getCon();
    }
  }

  public static DamlLf1.Type.Prim getTypePrim(DamlLf1.Type t, DamlLf1.Package lfPackage) {
    if (t.hasPrim()) {
      return t.getPrim();
    } else {
      DamlLf1.Type resolved = lfPackage.getInternedTypes(t.getInterned());
      return resolved.getPrim();
    }
  }

  private static String getChoiceName(
      DamlLf1.TemplateChoice choice, // no .getNameDname()  and .hasNameDname() methods
      DamlLf1.Package lfPackage) {
    String choN;
    if (!choice.getNameStr().equals("")) { // DamlLf version <= 1.6
      choN = choice.getNameStr();
    } else {
      choN = getInternedString(choice.getNameInternedStr(), lfPackage);
    }
    return choN;
  }

  public static String findPackage(DamlLedgerClient ledgerClient, DamlLf1.DottedName moduleName)
      throws InvalidProtocolBufferException {
    String strName = packageNames.get(moduleName);
    if (strName != null) {
      return strName;
    } else {
      PackageClient pkgClient = ledgerClient.getPackageClient();
      Iterable<String> pkgs = pkgClient.listPackages().blockingIterable();
      for (String pkgId : pkgs) {
        GetPackageResponse pkgResp = pkgClient.getPackage(pkgId).blockingGet();
        DamlLf.ArchivePayload archivePl =
            DamlLf.ArchivePayload.parseFrom(pkgResp.getArchivePayload());
        DamlLf1.Package dl1 = archivePl.getDamlLf1();
        List<DamlLf1.Module> mods = dl1.getModulesList();
        for (DamlLf1.Module mod : mods) {
          DamlLf1.DottedName modN = getModuleName(mod, dl1);
          if (modN.equals(moduleName)) {
            packageNames.put(moduleName, pkgId);
            return pkgId;
          }
        }
      }
    }
    throw new IllegalArgumentException("No package found " + moduleName);
  }

  public static DamlLf1.Package findPackageObject(
      DamlLedgerClient ledgerClient, DamlLf1.DottedName moduleName)
      throws InvalidProtocolBufferException {
    PackageClient pkgClient = ledgerClient.getPackageClient();
    String pkgId = findPackage(ledgerClient, moduleName);
    GetPackageResponse pkgResp = pkgClient.getPackage(pkgId).blockingGet();
    DamlLf.ArchivePayload archivePl = DamlLf.ArchivePayload.parseFrom(pkgResp.getArchivePayload());
    return archivePl.getDamlLf1();
  }

  public static DamlLf1.Package findPackageObject(DamlLedgerClient ledgerClient, String moduleName)
      throws InvalidProtocolBufferException {
    return findPackageObject(
        ledgerClient, DamlLf1.DottedName.newBuilder().addAllSegments(List.of(moduleName.split("\\."))).build());
  }

  public static TemplateType findTemplate(DamlLedgerClient ledgerClient, String moduleAndEntityName)
          throws IOException {
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

        DamlLf1.Package dl1 = findPackageObject(ledgerClient, moduleName);

        String choiceArgName = choiceArgEntry.getKey();
        DamlLf1.Type.Con choiceArgTypeCon = getTypeCon(choiceArgEntry.getValue(), dl1);

        String choiceDataTypeName =
            dottedNameToString(getTypeConName(choiceArgTypeCon.getTycon(), dl1));
        String choiceDataTypeFqn = toFqn(moduleName, choiceDataTypeName);
        if (choiceArgName.equals("Archive") || choiceDataTypeName.equals("Archive")) {
          choiceDataTypeFqn = "DA.Internal.Template:Archive";
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
    int segmentsCount = name.getSegmentsCount();

    if (segmentsCount > 0) {
      b.append(name.getSegments(0));
      for (int i = 1; i < segmentsCount; i++) {
        b.append('.').append(name.getSegments(i));
      }
    }
    return b.toString();
  }

  private static DataType findDataType(DamlLedgerClient ledgerClient, String moduleAndEntityName)
          throws IOException {
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
          throws IOException {
    PackageClient pkgClient = ledgerClient.getPackageClient();
    Iterable<String> pkgs = pkgClient.listPackages().blockingIterable();
    for (String pkgId : pkgs) {
      GetPackageResponse pkgResp = pkgClient.getPackage(pkgId).blockingGet();
      CodedInputStream codeInputStream = CodedInputStream.newInstance(pkgResp.getArchivePayload());
      codeInputStream.setRecursionLimit(1000); // default is 100 which is not enough for a package
      DamlLf.ArchivePayload archivePl =
          DamlLf.ArchivePayload.parseFrom(codeInputStream);
      DamlLf1.Package dl1 = archivePl.getDamlLf1();
      List<DamlLf1.Module> mods = dl1.getModulesList();
      for (DamlLf1.Module mod : mods) {
        for (DamlLf1.DefDataType dataType : mod.getDataTypesList()) {
          String modN = dottedNameToString(getModuleName(mod, dl1));
          String dataN = dottedNameToString(getDataTypeName(dataType, dl1));
          String moduleAndEntityName = toFqn(modN, dataN);
          Identifier id = new Identifier(pkgId, modN, dataN);
          identifiers.put(moduleAndEntityName, id);
          dataTypes.put(moduleAndEntityName, new DataType(mod, dataType, dl1));
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
      DamlLf1.Module mod, DamlLf1.DefDataType dataTypeName, DamlLf1.Package lfPackage) {
    Optional<DamlLf1.DefTemplate> template =
        mod.getTemplatesList()
            .stream()
            .filter(
                t ->
                    getDefTemplateName(t, lfPackage)
                        .equals(getDataTypeName(dataTypeName, lfPackage)))
            .findFirst();
    if (template.isPresent()) {
      HashMap<String, DamlLf1.Type> m = new HashMap<>();
      for (DamlLf1.TemplateChoice choice : template.get().getChoicesList()) {
        m.put(getChoiceName(choice, lfPackage), choice.getArgBinder().getType());
      }
      return Optional.of(m);
    } else {
      return Optional.empty();
    }
  }
}
