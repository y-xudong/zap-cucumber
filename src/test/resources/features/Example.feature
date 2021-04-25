Feature: Active Scan - example.com

  Scenario: Traditional Spider / Passive Scan of Spider / Active Scan
    Given OWASP ZAP has started
    When traditional spider scraping has completed
    And active scan has completed
    Then produce HTML report
    And shut down ZAP
