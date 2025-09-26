Feature: User Registration

  Scenario: Successful user registration
    Given a <Full Name> is entered
    And a <Email> is entered
    And a <Password> is entered
    And a <Confirm Password> is entered
    And the Terms and Conditions are agreed to
    When the REGISTER button is clicked
    Then the user is redirected to the Login screen with the message "Registration successful! Please log in."

    Examples:
      | Full Name  | Email                 | Password   | Confirm Password | Terms and Conditions |
      | John Doe   | john.doe@example.com  | Passw0rd   | Passw0rd         | true                 |
      | Jane Smith | jane.smith@example.com| Secur3Pass | Secur3Pass       | true                 |

  Scenario: Missing mandatory fields
    Given a <Full Name> is entered
    And a <Email> is entered
    And a <Password> is entered
    And a <Confirm Password> is entered
    And the Terms and Conditions are agreed to
    When the REGISTER button is clicked
    Then the error message "This field is required!" is displayed

    Examples:
      | Full Name | Email                | Password | Confirm Password | Terms and Conditions |
      |           |                      |          |                  | false                |
      | John Doe  |                      |          |                  |                      |
      |           | john.doe@example.com |          |                  |                      |
      |           |                      | Passw0rd |                  |                      |
      |           |                      |          | Passw0rd         |                      |
      |           |                      |          |                  | true                 |

  Scenario: Invalid email format
    Given a <Full Name> is entered
    And a <Email> is entered
    And a <Password> is entered
    And a <Confirm Password> is entered
    And the Terms and Conditions are agreed to
    When the REGISTER button is clicked
    Then the error message "Please enter a valid email address." is displayed

    Examples:
      | Full Name | Email       | Password | Confirm Password | Terms and Conditions |
      | John Doe  | john.doe@   | Passw0rd | Passw0rd         | true                 |
      | Jane Smith| jane.smith  | Secur3Pass| Secur3Pass      | true                 |

  Scenario: Password does not meet criteria
    Given a <Full Name> is entered
    And a <Email> is entered
    And a <Password> is entered
    And a <Confirm Password> is entered
    And the Terms and Conditions are agreed to
    When the REGISTER button is clicked
    Then the error message "Password must be at least 8 characters long and include at least one uppercase letter, one lowercase letter, and one number." is displayed

    Examples:
      | Full Name | Email                 | Password  | Confirm Password | Terms and Conditions |
      | John Doe  | john.doe@example.com  | password  | password         | true                 |
      | Jane Smith| jane.smith@example.com| PASSW0RD  | PASSW0RD         | true                 |
      | John Doe  | john.doe@example.com  | PassWord  | PassWord         | true                 |

  Scenario: Password and Confirm Password do not match
    Given a <Full Name> is entered
    And a <Email> is entered
    And a <Password> is entered
    And a <Confirm Password> is entered
    And the Terms and Conditions are agreed to
    When the REGISTER button is clicked
    Then the error message "Passwords do not match." is displayed

    Examples:
      | Full Name | Email                 | Password  | Confirm Password | Terms and Conditions |
      | John Doe  | john.doe@example.com  | Passw0rd  | Passw1rd         | true                 |
      | Jane Smith| jane.smith@example.com| Secur3Pass| Secur3Pas        | true                 |

  Scenario: Email already in use
    Given a <Full Name> is entered
    And a <Email> is entered
    And a <Password> is entered
    And a <Confirm Password> is entered
    And the Terms and Conditions are agreed to
    When the REGISTER button is clicked
    Then the error message "This email is already registered." is displayed

    Examples:
      | Full Name | Email                | Password  | Confirm Password | Terms and Conditions |
      | John Doe  | used.email@example.com | Passw0rd | Passw0rd         | true                 |
      | Jane Smith| existing.email@example.com | Secur3Pass| Secur3Pass | true                 |