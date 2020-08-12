# Read Me -- Functional Testing Library
## Introduction
This library provides functions to test that a DAML application and its bots are working together correctly. Integration tests should include the following steps:
* Starting/stopping a Sandbox ledger
* Creation of a new contract on the ledger
* Observation of ledger events (creation/exercise) and extraction of contract IDs from them
* Exercise of choices on contracts
* Ledger-time operations (getting and setting the time)


## Getting Started

### Installing

Add the library as a test scoped dependency to your project. In case of a Maven project:
```
  <dependencies>
    ...
    <dependency>
        <groupId>com.digitalasset</groupId>
        <artifactId>functest-java_2.12</artifactId>
        <version>0.1.18</version>
        <scope>test</scope>
    </dependency>
    ...
  </dependencies>
```
Additional daml dependencies are required by the library, but these dependencies are not specified as transitive dependencies. Unless you already have them in your project, add them as following:
```
  <dependencies>
    ...
    <dependency>
        <groupId>com.daml</groupId>
        <artifactId>bindings-rxjava</artifactId>
        <version>1.0.0</version>
    </dependency>
    ...
  </dependencies>
```
**Note:** versioning of this library is not tied to DAML SDK versions. The above artifacts are considered stable, changing rarely. The testing library is compatible with minor version changes in the SDK. We will release new version of the library for incompatible SDK versions. You can test a desired SDK version by using `scripts/upgrade-sdk.sh`, and running the tests.
## Using the library in Java projects

### Using the library with the Sandbox JUnit4 Rule

One can easily instantiate a Sandbox process using the JUnit4 Rule technique:
```
  private static Sandbox sandbox =
      Sandbox.builder()
          .dar(DAR_PATH)
          .moduleAndScript("Test", "testSetup")
          .parties(ALICE, BOB, CHARLIE)
          .build();

  @ClassRule public static ExternalResource sandboxClassRule = sandbox.getClassRule();
  @Rule public ExternalResource sandboxRule = sandbox.getRule();
```
The ClassRule and the Rule are mandatory parts of each integration test. They take care of starting/stopping the Sandbox. Note that `Sandbox sandbox` needs a DAR file path. DAR compilation should be done via build scripts.
Sandbox has two modes, restart mode, in which it is restarted after each test case, and reset mode (use the `useReset()` function in the builder) that is faster but *cannot be supplied with a market setup scenario*.
Sandbox object `sandbox` offers the following tools:
- a ledger adapter via `getLedgerAdapter` (which has the type *DefaultLedgerAdapter*)
- a DAML ledger client via `getClient`

### Testing with functions provided by a ledger adapter

Usage of class *DefaultLedgerAdapter* (`sandbox.getLedgerAdapter()`) can be demonstrated via examples.

To create a contract on the ledger (the last argument contains the parameters of the contract instantion):
```
sandbox.getLedgerAdapter().createContract(ALICE_PARTY, MyTemplate.TEMPLATE_ID, new Record(...));
```
To observe the creation event and capture the contract id (in the store, as "cid01"):
```
sandbox.getLedgerAdapter().observeEvent(ALICE_PARTY, ContractCreated.apply(MyTemplate.TEMPLATE_ID,
                "{CAPTURE: cid01}", arguments));
String val = sandbox.getLedgerAdapter().valueStore().get("cid01").getContractId();
```
This can be done more simply by using the class *Dsl* and *getCreatedContractId*:
```
MyTemplate.ContractId someCid =
    getCreatedContractId(sandbox.getLedgerAdapter(), ALICE_PARTY, MyTemplate.TEMPLATE_ID,
                         arguments, MyTemplate.ContractId::new);
```
If you want to check the arguments:
```
MyTemplate.ContractId someCid =
    getCreatedContractId(sandbox.getLedgerAdapter(), ALICE_PARTY, MyTemplate.TEMPLATE_ID, MyTemplate.ContractId::new);
```
If you want to use both the contract ID and the arguments:

```
ContractWithId<MyTemplate.ContractId> someCwithID =
    getMatchedContract(sandbox.getLedgerAdapter(), transportCompany, MyTemplate.TEMPLATE_ID, MyTemplate.ContractId::new);
```

To exercise a choice on the contract ID:

```
val execCmd = someCid.exerciseMyTemplate_Request(Collections.singletonList(arguments2))
sandbox.getLedgerAdapter().exerciseChoice(BUYER_PARTY, execCmd);
```

There are some other convenience functions that internally use a ledger adapter but are provided directy by `Sandbox.Process`.
To get the contract ID of a created contract, one can use (note, how contract ID constructor is used to get results of that specific contract ID):
```
BuyerSellerRelationship.ContractId buyerSellerRelationshipCid = sandbox.getCreatedContractId(
        BUYER_PARTY,
        BuyerSellerRelationship.TEMPLATE_ID,
        BuyerSellerRelationship.ContractId::new);
```

It is possible to specify the contract parameters as well:
```
BuyerSellerRelationship.ContractId buyerSellerRelationshipCid = sandbox.getCreatedContractId(
        BUYER_PARTY,
        BuyerSellerRelationship.TEMPLATE_ID,
        record(BUYER_PARTY, BUYER_ADDRESS, SELLER_PARTY),
        BuyerSellerRelationship.ContractId::new);
```
To get the contract ID with all the parameters:
```
ContractWithId<DeliveryInstruction.ContractId> deliveryInstructionWithCid = sandbox.getMatchedContract(
        TRASPORT_PARTY,
        DeliveryInstruction.TEMPLATE_ID,
        DeliveryInstruction.ContractId::new);
```

If a workflow creates multiple instances of the same template it can be useful to check if they can be observed on the
ledger regardless the order they arrive.
Example to fetch at most two SettledDvPs and check if one of them has a payment of 8550000 and the other has 4750000:
```
    assertTrue(sandbox.observeMatchingContracts(
        CCP_PARTY,
        SettledDvP.TEMPLATE_ID,
        SettledDvP::fromValue,
        true,
        dvp -> dvp.paymentAmount == 8550000L,
        dvp -> dvp.paymentAmount == 4750000L);
```
If the boolean argument (`exact`) is true, each incoming contract must match at least one predicate.
Otherwise, it may skip several events until all predicates are matched.

### Running tests

The library uses JUnit4 to provide its functionalities. Several build systems (e.g. Maven, Sbt) support running JUnit tests easily.
In Maven, one can use either *maven-failsafe-plugin* or *maven-surefire-plugin* to run tests using this test library.
To configure any of these, use the official documentation, to run, execute `mvn verify`.

© 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
