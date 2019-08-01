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

#### Prerequisites

To be able to develop the library you need:
* sbt

To use the library in a project:
* download the library from |PUT LOCATION HERE| as a dependency

## Using the library in Java projects

To write tests using the library.

### Most important traits

The library has traits that define the main capabilities of different test tools:
* SandboxRunner - functions to start/stop a Sandbox ledger and the app-specific bots
* LedgerAdapter - functions to communicate with the ledger
   * Creation of a contract
   * Exercise of a choice
   * Observation of events
* JavaLedgerAdapter - similar to LedgerAdapter but optimized for usage from Java
* TimeProvider - functions to get/set ledger time

Most of these are implemented by *DefaultLedgerAdapter*, which is the most important tool for testing in the library. Sandbox runner is a class itself.

### Using the library with the Sandbox JUnit4 Rule

One can easily instantiate a Sandbox process using the JUnit4 Rule technique:
```
    private static Sandbox sandbox =
            Sandbox.builder()
                    .dar(RELATIVE_DAR_PATH)
                    .projectDir(Paths.get("."))
                    .module(TEST_MODULE)
                    .scenario(TEST_SCENARIO)
                    .parties(BUYER_PARTY.getValue(), SELLER_PARTY.getValue(), SUPPLIER_PARTY.getValue())
                    .setupAppCallback(SupplyChain::runBots)
                    .build();

    @ClassRule
    public static ExternalResource compile = sandboxC.compilation();
    @Rule
    public Sandbox.Process sandbox = sandboxC.process();
```
The rule `sandboxC.process()` automatically starts up and shuts down DAML Sandbox for each test.
The class rule `sandboxC.compilation()` takes care of the DAML compilation before running the tests.
Sandbox Process object `sandbox` offers the following tools:
- a ledger adapter via `getLedgerAdapter` (which has the type *DefaultLedgerAdapter*)
- a DAML ledger client via `getClient`

### Testing with functions provided by a ledger adapter

Usage of class *DefaultLedgerAdapter* can be demonstrated via examples.

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
Example to fetch 4 SettledDvPs and check if one of them has a payment of 8550000:
```
    assertTrue(sandbox.observeMatchingContracts(
        CCP_PARTY,
        SettledDvP.TEMPLATE_ID,
        SettledDvP::fromValue,
        true,
        dvp -> dvp.paymentAmount == 8550000L,
        dvp -> dvp.paymentAmount == 4750000L);
```

© 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
