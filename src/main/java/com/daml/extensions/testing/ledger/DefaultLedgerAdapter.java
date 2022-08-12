/*
 * Copyright 2020 Digital Asset (Switzerland) GmbH and/or its affiliates
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daml.extensions.testing.ledger;

import com.daml.daml_lf_dev.DamlLf1;
import com.daml.extensions.testing.comparator.MessageTester;
import com.daml.extensions.testing.comparator.ledger.ContractCreated;
import com.daml.extensions.testing.ledger.clock.TimeProvider;
import com.daml.extensions.testing.logging.Dump;
import com.daml.extensions.testing.store.InMemoryMessageStorage;
import com.daml.extensions.testing.store.ValueStore;
import com.daml.extensions.testing.utils.ContractWithId;
import com.daml.ledger.api.v1.*;
import com.daml.ledger.api.v1.admin.PackageManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.PackageManagementServiceOuterClass;
import com.daml.ledger.api.v1.admin.PartyManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass;
import com.daml.ledger.javaapi.data.*;
import com.daml.ledger.rxjava.DamlLedgerClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.daml.extensions.testing.utils.PackageUtils.findPackage;
import static com.google.protobuf.ByteString.copyFrom;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.fail;

public class DefaultLedgerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(DefaultLedgerAdapter.class);

  private static final String APP_ID = "func-test";

  private static final Duration TTL = Duration.ofSeconds(10);

  private static final String ChannelName = "ledger";

  private static final String wireLogger = "LEDGER.WIRE";
  private static final String interactionLogger = "LEDGER.INTERACTION";

  // TODO(lt) figure out an interface for interacting with the value store
  public final ValueStore valueStore;
  private final Supplier<TimeProvider> timeProviderFactory;
  private final Duration timeout;

  private TimeProvider timeProvider;
  private ManagedChannel channel;
  private String ledgerId;
  private LedgerOffset startOffset;
  private Map<String, InMemoryMessageStorage<TreeEvent>> storageByParty;

  // todo which one did we use in rln?
  public DefaultLedgerAdapter(
      ValueStore valueStore,
      String ledgerId,
      ManagedChannel channel,
      Duration timeout,
      Supplier<TimeProvider> timeProviderFactory) {
    this.valueStore = valueStore;
    this.ledgerId = ledgerId;
    this.channel = channel;
    this.timeout = timeout;
    this.timeProviderFactory = timeProviderFactory;
  }

  private static Timestamp toProtobufTimestamp(Instant instant) {
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  public void start(boolean useContainers, String... parties) {
    start(parties, LedgerOffset.LedgerBegin.getInstance(), useContainers);
  }

  public synchronized void start(
      String[] explicitParties, LedgerOffset suggestStartOffset, boolean useContainers) {
    logger.info("Starting Ledger Adapter");
    this.startOffset = initStartOffset(suggestStartOffset);
    if (!useContainers) {
      // todo why so?
      this.timeProvider = timeProviderFactory.get();
    }
    storageByParty = new ConcurrentHashMap<>();
    for (String explicitParty : explicitParties) {
      // todo what is that?
      getStorage(explicitParty);
    }
    logger.info("Ledger Adapter Started");
  }

  public synchronized void stop() throws InterruptedException {
    if (channel != null) {
      logger.info("Stopping Ledger Adapter");
      channel.shutdown().awaitTermination(5L, TimeUnit.SECONDS);
      channel = null;
      logger.info("Ledger Adapter stopped");
    }
  }

  public synchronized void createContract(Party party, Identifier templateId, DamlRecord payload)
      throws InvalidProtocolBufferException {
    logger.debug("Attempting to create a contract {}", templateId);
    submit(party, new CreateCommand(templateId, payload));
  }

  public synchronized void exerciseChoice(
      Party party, Identifier templateId, ContractId contractId, String choice, Value payload)
      throws InvalidProtocolBufferException {
    exerciseChoice(party, new ExerciseCommand(templateId, contractId.getValue(), choice, payload));
  }

  public void exerciseChoice(Party party, ExerciseCommand exerciseCommand) {
    logger.debug(
        "Attempting to create exercise {} on {} in contract {}",
        exerciseCommand.getChoice(),
        exerciseCommand.getTemplateId(),
        exerciseCommand.getContractId());
    submit(party, exerciseCommand);
  }

  public TreeEvent observeEvent(String party, MessageTester<TreeEvent> eventTester) {
    TreeEvent event = getStorage(party).observe(timeout, eventTester);
    Dump.dump(interactionLogger, new ObserveEvent(party, event));
    return event;
  }

  public void assertDidntHappen(String party, MessageTester<TreeEvent> eventTester) {
    getStorage(party).assertDidntHappen(eventTester);
  }

  private synchronized InMemoryMessageStorage<TreeEvent> getStorage(String party) {
    // ensureStarted();
    if (!storageByParty.containsKey(party)) {
      InMemoryMessageStorage<TreeEvent> storage = initStorageAndStartListening(party);
      storageByParty.put(party, storage);
    }
    return storageByParty.get(party);
  }

  private LedgerOffset initStartOffset(LedgerOffset suggestStartOffset) {
    if (LedgerOffset.LedgerEnd.getInstance().equals(suggestStartOffset)) {
      LedgerOffsetOuterClass.LedgerOffset endOffset =
          TransactionServiceGrpc.newBlockingStub(channel)
              .getLedgerEnd(
                  TransactionServiceOuterClass.GetLedgerEndRequest.newBuilder()
                      .setLedgerId(ledgerId)
                      .build())
              .getOffset();

      return LedgerOffset.fromProto(endOffset);
    }
    return suggestStartOffset;
  }

  private InMemoryMessageStorage<TreeEvent> initStorageAndStartListening(String party) {
    InMemoryMessageStorage<TreeEvent> storage =
        new InMemoryMessageStorage<>(ChannelName, valueStore);
    StreamObserver<TransactionServiceOuterClass.GetTransactionTreesResponse> observer =
        new StreamObserver<TransactionServiceOuterClass.GetTransactionTreesResponse>() {
          public void onNext(TransactionServiceOuterClass.GetTransactionTreesResponse response) {
            onMessage(response, party, storage);
          }

          public void onError(Throwable t) {
            logger.error("Error occurred in stream handler for party " + party, t);
          }

          public void onCompleted() {
            logger.trace("Stream processing completed for party {}", party);
          }
        };

    GetTransactionsRequest request =
        new GetTransactionsRequest(
            ledgerId,
            startOffset,
            new FiltersByParty(Collections.singletonMap(party, NoFilter.instance)),
            true);

    TransactionServiceGrpc.newStub(channel).getTransactionTrees(request.toProto(), observer);
    return storage;
  }

  private synchronized void onMessage(
      TransactionServiceOuterClass.GetTransactionTreesResponse response,
      String party,
      InMemoryMessageStorage<TreeEvent> storage) {
    if (channel != null) {
      response
          .getTransactionsList()
          .forEach(
              tree -> {
                tree.getEventsByIdMap()
                    .values()
                    .forEach(
                        protoEvent -> {
                          TreeEvent event = TreeEvent.fromProtoTreeEvent(protoEvent);
                          Dump.dump(wireLogger, new ObserveEvent(party, event));
                          storage.onMessage(event);
                        });
              });
    }
  }

  private void submit(Party party, Command command) {
    String cmdId = UUID.randomUUID().toString();

    CommandServiceOuterClass.SubmitAndWaitRequest.Builder commands =
        CommandServiceOuterClass.SubmitAndWaitRequest.newBuilder()
            .setCommands(
                CommandsOuterClass.Commands.newBuilder()
                    .setLedgerId(ledgerId)
                    .setWorkflowId(String.format("%s:%s", APP_ID, cmdId))
                    .setApplicationId(APP_ID)
                    .setCommandId(cmdId)
                    .setParty(party.getValue())
                    .addCommands(command.toProtoCommand()));
    CommandEvent event = new CommandEvent(cmdId, party.getValue(), command);
    Dump.dump(wireLogger, event);
    CommandServiceGrpc.newBlockingStub(channel).submitAndWait(commands.build());
    Dump.dump(interactionLogger, event);
  }

  public Hashtable<String, Party> getMapKnownParties() {
    PartyManagementServiceOuterClass.ListKnownPartiesResponse listOfParties =
        PartyManagementServiceGrpc.newBlockingStub(channel)
            .listKnownParties(
                PartyManagementServiceOuterClass.ListKnownPartiesRequest.newBuilder().build());
    Hashtable<String, Party> mapPartyId = new Hashtable<>();
    listOfParties
        .getPartyDetailsList()
        .forEach(p -> mapPartyId.put((p.getDisplayName()), new Party(p.getParty())));
    return mapPartyId;
  }

  public void allocatePartyOnLedger(String p) throws InterruptedException {
    eventually(
        () ->
            PartyManagementServiceGrpc.newBlockingStub(channel)
                .allocateParty(
                    PartyManagementServiceOuterClass.AllocatePartyRequest.newBuilder()
                        .setPartyIdHint(p)
                        .setDisplayName(p)
                        .build()));
  }

  public void uploadDarFile(Path darPath) throws IOException, InterruptedException {
    ByteString b = copyFrom(Files.readAllBytes(darPath));
    PackageManagementServiceOuterClass.UploadDarFileResponse uploadDarFileResponse =
        PackageManagementServiceGrpc.newBlockingStub(channel)
            .uploadDarFile(
                PackageManagementServiceOuterClass.UploadDarFileRequest.newBuilder()
                    .setDarFile(b)
                    .build());
    logger.info("DAR file upload response. Empty if success: ", uploadDarFileResponse);
  }

  private void eventually(Runnable code) throws InterruptedException {
    Instant started = Instant.now();
    Function<Duration, Boolean> hasPassed =
        x -> Duration.between(started, Instant.now()).compareTo(x) > 0;
    boolean isSuccessful = false;
    while (!isSuccessful) {
      try {
        code.run();
        isSuccessful = true;
      } catch (Throwable ignore) {
        if (hasPassed.apply(Duration.ofMinutes(1))) {
          fail("Code did not succeed in time.");
        } else {
          sleep(200);
          isSuccessful = false;
        }
      }
    }
  }

  private boolean isPackageReadyToUse(
      DamlLedgerClient damlLedgerClient, DamlLf1.DottedName moduleDottedName) {
    String packageId;
    try {
      packageId = findPackage(damlLedgerClient, moduleDottedName);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
    PackageServiceOuterClass.GetPackageStatusResponse getPackageStatusResponse =
        PackageServiceGrpc.newBlockingStub(channel)
            .getPackageStatus(
                PackageServiceOuterClass.GetPackageStatusRequest.newBuilder()
                    .setLedgerId(ledgerId)
                    .setPackageId(packageId)
                    .build());
    return getPackageStatusResponse.getPackageStatus().getNumber() == 1;
  }

  public List<PackageManagementServiceOuterClass.PackageDetails> getPackages() {
    PackageManagementServiceOuterClass.ListKnownPackagesResponse listKnownPackagesResponse =
        PackageManagementServiceGrpc.newBlockingStub(channel)
            .listKnownPackages(
                PackageManagementServiceOuterClass.ListKnownPackagesRequest.newBuilder().build());
    return listKnownPackagesResponse.getPackageDetailsList();
  }

  public void setCurrentTime(Instant time) {
    timeProvider.setCurrentTime(time);
  }

  private static final String key = "internal-cid-query";
  private static final String recordKey = "internal-recordKey";

  public <Cid> Cid getCreatedContractId(
      Party party, Identifier identifier, DamlRecord arguments, Function<String, Cid> ctor) {
    observeEvent(
        party.getValue(),
        ContractCreated.expectContractWithArguments(
            identifier, "{CAPTURE:" + key + "}", arguments));

    String val = valueStore.get(key).asContractId().get().getValue();
    Cid cid = ctor.apply(val);
    valueStore.remove(key);
    return cid;
  }

  public <Cid> Cid getCreatedContractId(
      Party party, Identifier identifier, Function<String, Cid> ctor) {
    observeEvent(
        party.getValue(), ContractCreated.expectContract(identifier, "{CAPTURE:" + key + "}"));
    String val = valueStore.get(key).asContractId().get().getValue(); // getContractId();
    Cid cid = ctor.apply(val);
    valueStore.remove(key);
    return cid;
  }

  public <Cid> ContractWithId<Cid> getMatchedContract(
      Party party, Identifier identifier, Function<String, Cid> ctor) {
    observeEvent(
        party.getValue(), ContractCreated.capture(identifier, "{CAPTURE:" + key + "}", recordKey));

    String contractId = valueStore.get(key).asContractId().get().getValue();
    Cid cid = ctor.apply(contractId);
    Value record = valueStore.get(recordKey).asRecord().get();
    valueStore.remove(key);
    valueStore.remove(recordKey);
    return new ContractWithId<>(cid, record);
  }

  /**
   * Makes sure that a set of contracts are present on the ledger. The contracts with the given
   * templateIds are fetched one-by-one and matched against the given set of predicates. If all
   * predicates are satisfied the method returns with true.
   *
   * <p>If `exact` is true, each incoming contract must match at least one predicate, otherwise the
   * method will return false.
   *
   * <p>Note: the ledger cursor will be moved during the execution of this method.
   *
   * @return true if all predicates were satisfied, false if a non-matching contract was observed in
   *     exact mode.
   */
  public <Contract> boolean observeMatchingContracts(
      Party party,
      Identifier templateId,
      Function<Value, Contract> ctor,
      boolean exact,
      Predicate<Contract>... predicates) {
    Set<Predicate<Contract>> predicateSet = new HashSet<>(Arrays.asList(predicates));
    while (!predicateSet.isEmpty()) {
      ContractWithId<String> contractWithId = getMatchedContract(party, templateId, i -> i);
      Contract contract = ctor.apply(contractWithId.record);
      Optional<Predicate<Contract>> predicateMatch =
          predicateSet.stream().filter(p -> p.test(contract)).findFirst();
      if (predicateMatch.isPresent()) {
        predicateSet.remove(predicateMatch.get());
      } else {
        if (exact) return false;
      }
    }
    return true;
  }
}
