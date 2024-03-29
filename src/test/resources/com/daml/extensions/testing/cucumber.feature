Feature: Cucumber support in the Functional Testing library
  Cucumber support needs to work.

  Background:
    Given Sandbox is started in directory "src/test/resources/ping-pong" with DAR "src/test/resources/ping-pong.dar" and the following parties
    | Alice |
    | Bob   |

  Scenario: NumericTester can be created.
    Given "Bob" creates contract "MyPingPong:NumericTester" using values
    | s | Bob      |
    | x | 3.124321 |
    | y | 4.000    |

   Then "Bob" should observe the creation of "MyPingPong:NumericTester" with contract id "pingPongCid1" and values
   | s    | Bob          |
   | x    | 3.1243210000 |
   | y    | 4.0000       |


  Scenario: A contract can be created and observed.
    Given "Bob" creates contract "MyPingPong:MyPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |

    Then "Alice" should observe the creation of "MyPingPong:MyPing"

  Scenario: Multiple contracts can be created and observed.
    Given "Bob" creates contract "MyPingPong:MyPing" using values
    | sender   | Bob   | Bob   |
    | receiver | Alice | Alice |
    | count    | 3     | 4     |

    Then "Alice" should observe the creation of "MyPingPong:MyPing"
    Then "Alice" should observe the creation of "MyPingPong:MyPing"

  Scenario: A contract with specific values can be created and observed.
    Given "Bob" creates contract "MyPingPong:MyPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    Then "Alice" should observe the creation of "MyPingPong:MyPing" with contract id "pingPongCid1" and values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |

  Scenario: Contract choices can be exercised.
    Given "Bob" creates contract "MyPingPong:MyPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "MyPingPong:MyPing" with contract id "pingPongCid2" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Alice" exercises choice "RespondPong" on "MyPingPong:MyPing" with contract id "pingPongCid2"
    Then "Bob" should observe the creation of "MyPingPong:MyPong"

  Scenario: Contract choices with arguments can be exercised.
    Given "Bob" creates contract "MyPingPong:ArgumentPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "MyPingPong:ArgumentPing" with contract id "pingPongCid3" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Alice" exercises choice "ArgumentPingRespondPong" on "MyPingPong:ArgumentPing" with contract id "pingPongCid3" using values
    | 2 |
    Then "Bob" should observe the creation of "MyPingPong:MyPong"

  Scenario: Contract choices with arguments can be exercised using a data table with argument names.
    Given "Bob" creates contract "MyPingPong:ArgumentPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "MyPingPong:ArgumentPing" with contract id "pingPongCid3" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Alice" exercises choice "ArgumentPingRespondPong" on "MyPingPong:ArgumentPing" with contract id "pingPongCid3" using values
    | intArg | 2 |
    Then "Bob" should observe the creation of "MyPingPong:MyPong"

  Scenario: A created contract can be archived and the archival can be observed.
    Given "Bob" creates contract "MyPingPong:MyPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "MyPingPong:MyPing" with contract id "cidToArchive" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Bob" exercises choice "Archive" on "MyPingPong:MyPing" with contract id "cidToArchive"
    Then "Bob" should observe the archival of "MyPingPong:MyPing" with contract id "cidToArchive"

  Scenario: An expected failure can be observed.
    Given "Bob" creates contract "MyPingPong:MyPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "MyPingPong:MyPing" with contract id "pingPongCid4" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Bob" exercises choice "RespondPong" on "MyPingPong:MyPing" with contract id "pingPongCid4" and expects failure
    Then they should receive a technical failure containing message "requires authorizers Alice"
