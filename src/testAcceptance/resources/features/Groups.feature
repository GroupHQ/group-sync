# Since these tests utilize the real event broker, do not run alongside other application instances
Feature: Groups
  GroupHQ allows anyone to join, leave, and view pre-made groups that are managed by the GroupHQ system.

  @GroupPolicy
  Scenario: Always have active groups
    Given any time
    When I request groups
    Then I should be given a list of at least 3 active groups

  @MemberPolicy
  Scenario: User joining group
    Given there is an active group
    When I try to join the group
    Then I should receive an event confirming my membership
    And the group's current member size should increase by one

  @MemberPolicy
  Scenario: User joining group more than once
    Given there is an active group
    And I am a member of the group
    When I try to join the group
    Then I should not be added to the group again

  @MemberPolicy
  Scenario: User joining multiple groups
    Given there is an active group
    And I am a member of the group
    And there is a second active group
    When I try to join the group
    Then I should not be added to the second group

  @MemberPolicy
  Scenario: User leaving a group
    Given there is an active group
    And I am a member of the group
    When I try to leave the group
    Then I should no longer be an active member of that group

  @MemberPolicy
  Scenario: User requesting another member to leave a group
    Given there is an active group
    And I am a member of the group
    When another user tries to remove me from the group
    Then I should be in the group only once