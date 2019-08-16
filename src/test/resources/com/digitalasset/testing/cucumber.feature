Feature: Cucumber support in the Functional Testing library
  Cucumber support needs to work.

  Background:
    Given Sandbox is started with DAR "src/test/resources/ping-pong.dar"
    | Alice | Bob |

  Scenario: A contract can be created and observed.
    When "Bob" creates contract "PingPong:Ping" using values
    | Bob | Alice | 3 |
    Then "Bob" should observe the creation of "PingPong:Ping" with values
    | Bob | Alice | 3 |

  Scenario: A contract with specific values can be created and observed.
    When "Bob" creates contract "PingPong:Ping" using values
    | Bob | Alice | 3 |
    Then "Bob" should observe the creation of "PingPong:Ping" with contract id "pingPongCid1" and values
    | Bob | Alice | 3 |

  Scenario: Contract choices can be exercised.
    Given "Bob" creates contract "PingPong:Ping" using values
    | Bob | Alice | 3 |
    When "Bob" should observe the creation of "PingPong:Ping" with contract id "pingPongCid2" and values
    | Bob | Alice | 3 |
    When "Alice" exercises choice "RespondPong" on "PingPong:Ping" with contract id "pingPongCid2"

  Scenario: Contract choices with arguments can be exercised.
    Given "Bob" creates contract "PingPong:ArgumentPing" using values
    | Bob | Alice | 3 |
    When "Bob" should observe the creation of "PingPong:ArgumentPing" with contract id "pingPongCid3" and values
    | Bob | Alice | 3 |
    When "Alice" exercises choice "ArgumentPingRespondPong" on "PingPong:ArgumentPing" with contract id "pingPongCid3" using values
    | 2 |