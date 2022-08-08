Feature: Cucumber support in the Functional Testing library
  Cucumber support needs to work.

  Background:
    Given Sandbox Container "digitalasset/canton-open-source" is started with DAR "src/test/resources/ping-pong.dar" and the following parties
      | Bob     |
      | Alice   |
      | Charlie |

  Scenario: NumericTester can be created.
    Given "Bob" creates contract "PingPong:NumericTester" using values
      | s | Bob      |
      | x | 3.124321 |
      | y | 4.000    |

    Then "Bob" should observe the creation of "PingPong:NumericTester" with contract id "pingPongCid1" and values
      | s    | Bob          |
      | x    | 3.1243210000 |
      | y    | 4.0000       |
