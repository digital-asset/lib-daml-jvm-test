Feature: Cucumber support in the Functional Testing library
  Cucumber support needs to work.

  Background:
    Given Sandbox is started in directory "src/test/resources/ping-pong" with DAR "src/test/resources/ping-pong.dar" and the following parties
    | Alice |
    | Bob   |

  Scenario: NumericTester can be created.
    Given "Bob" creates contract "PingPong:NumericTester" using values
    | s | Bob      |
    | x | 3.124321 |
    | y | 4.000    |

   Then "Bob" should observe the creation of "PingPong:NumericTester" with contract id "pingPongCid1" and values
   | s    | Bob          |
   | x    | 3.1243210000 |
   | y    | 4.0000       |


  Scenario: A contract can be created and observed.
    Given "Bob" creates contract "PingPong:Ping" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |

    Then "Alice" should observe the creation of "PingPong:Ping"

  Scenario: Multiple contracts can be created and observed.
    Given "Bob" creates contract "PingPong:Ping" using values
    | sender   | Bob   | Bob   |
    | receiver | Alice | Alice |
    | count    | 3     | 4     |

    Then "Alice" should observe the creation of "PingPong:Ping"
    Then "Alice" should observe the creation of "PingPong:Ping"

  Scenario: A contract with specific values can be created and observed.
    Given "Bob" creates contract "PingPong:Ping" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    Then "Alice" should observe the creation of "PingPong:Ping" with contract id "pingPongCid1" and values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |

  Scenario: Contract choices can be exercised.
    Given "Bob" creates contract "PingPong:Ping" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "PingPong:Ping" with contract id "pingPongCid2" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Alice" exercises choice "RespondPong" on "PingPong:Ping" with contract id "pingPongCid2"
    Then "Bob" should observe the creation of "PingPong:Pong"

  Scenario: Contract choices with arguments can be exercised.
    Given "Bob" creates contract "PingPong:ArgumentPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "PingPong:ArgumentPing" with contract id "pingPongCid3" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Alice" exercises choice "ArgumentPingRespondPong" on "PingPong:ArgumentPing" with contract id "pingPongCid3" using values
    | 2 |
    Then "Bob" should observe the creation of "PingPong:Pong"

  Scenario: Contract choices with arguments can be exercised using a data table with argument names.
    Given "Bob" creates contract "PingPong:ArgumentPing" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "PingPong:ArgumentPing" with contract id "pingPongCid3" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Alice" exercises choice "ArgumentPingRespondPong" on "PingPong:ArgumentPing" with contract id "pingPongCid3" using values
    | intArg | 2 |
    Then "Bob" should observe the creation of "PingPong:Pong"

  Scenario: A created contract can be archived and the archival can be observed.
    Given "Bob" creates contract "PingPong:Ping" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "PingPong:Ping" with contract id "cidToArchive" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Bob" exercises choice "Archive" on "PingPong:Ping" with contract id "cidToArchive"
    Then "Bob" should observe the archival of "PingPong:Ping" with contract id "cidToArchive"

  Scenario: An expected failure can be observed.
    Given "Bob" creates contract "PingPong:Ping" using values
    | sender   | Bob   |
    | receiver | Alice |
    | count    | 3     |
    When "Alice" should observe the creation of "PingPong:Ping" with contract id "pingPongCid4" and values
    | Bob   |
    | Alice |
    | 3     |
    When "Bob" exercises choice "RespondPong" on "PingPong:Ping" with contract id "pingPongCid4" and expects failure
    Then they should receive a technical failure containing message "requires authorizers Alice"
