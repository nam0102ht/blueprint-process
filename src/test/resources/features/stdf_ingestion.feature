Feature: STDF File Ingestion Pipeline

  Scenario: Ingest a standard STDF file and verify database inserts and ActiveMQ queue messages
    Given the OracleDB and ActiveMQ services are running
    And a mock STDF binary file "test_lot_123.stdf" exists with lot "LOT_123" and 100 parametric tests
    When the STDF pipeline ingests "test_lot_123.stdf"
    Then 1 lot record for "LOT_123" should be present in OracleDB
    And 100 test results for lot "LOT_123" should be present in OracleDB
    And 100 JSON messages should be published to ActiveMQ queue "stdf.test.results"
    And 1 JSON message should be published to ActiveMQ queue "stdf.lot.meta"
    And 1 JSON message should be published to ActiveMQ queue "stdf.lot.summary"
