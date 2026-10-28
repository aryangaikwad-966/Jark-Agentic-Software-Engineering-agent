package com.example.Jark.agent.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Dataset of benchmark cases for agent evaluation.
 * Contains real Java/Spring issue cases to test investigation capabilities.
 */
public record AgentBenchmarkDataset(List<AgentBenchmarkCase> cases) {

    public AgentBenchmarkDataset {
        cases = List.copyOf(cases);
    }

    /**
     * Creates a default benchmark dataset with common Java/Spring issue cases.
     * These are representative cases that the agent should be able to investigate.
     */
    public static AgentBenchmarkDataset createDefault() {
        List<AgentBenchmarkCase> cases = new ArrayList<>();
        
        // Case 1: Missing @PreAuthorize on sensitive endpoint
        cases.add(new AgentBenchmarkCase(
                "SEC-001",
                "Missing @PreAuthorize on sensitive REST endpoint",
                1L, // Repository ID - would be configured for test repositories
                "Investigate the UserController and check if the deleteUser endpoint has proper authorization",
                "The deleteUser endpoint in UserController is missing @PreAuthorize annotation, allowing unauthorized access",
                Set.of("endpoint", "authorization", "security")
        ));
        
        // Case 2: SQL injection vulnerability in query
        cases.add(new AgentBenchmarkCase(
                "SEC-002",
                "SQL injection in custom query",
                1L,
                "Investigate the UserRepository for potential SQL injection vulnerabilities",
                "The UserRepository uses native query with concatenation instead of parameterized binding",
                Set.of("query", "sql", "vulnerability", "parameter")
        ));
        
        // Case 3: Missing validation on input
        cases.add(new AgentBenchmarkCase(
                "VAL-001",
                "Missing input validation on user registration",
                1L,
                "Investigate the UserRegistrationService for input validation",
                "The UserRegistrationService does not validate email format before processing",
                Set.of("validation", "email", "input")
        ));
        
        // Case 4: Exception handling issues
        cases.add(new AgentBenchmarkCase(
                "ERR-001",
                "Improper exception handling in service layer",
                1L,
                "Investigate the OrderService for exception handling issues",
                "The OrderService catches Exception without specific handling, potentially hiding errors",
                Set.of("exception", "handling", "error")
        ));
        
        // Case 5: Dependency cycle detection
        cases.add(new AgentBenchmarkCase(
                "ARCH-001",
                "Circular dependency between services",
                1L,
                "Investigate the service layer for circular dependencies",
                "UserService and OrderService have a circular dependency through direct method calls",
                Set.of("dependency", "cycle", "circular")
        ));
        
        return new AgentBenchmarkDataset(cases);
    }

    /**
     * Creates a dataset from a custom list of cases.
     */
    public static AgentBenchmarkDataset fromCases(List<AgentBenchmarkCase> cases) {
        return new AgentBenchmarkDataset(cases);
    }

    /**
     * Returns the number of cases in the dataset.
     */
    public int size() {
        return cases.size();
    }

    /**
     * Returns true if the dataset is empty.
     */
    public boolean isEmpty() {
        return cases.isEmpty();
    }
}
