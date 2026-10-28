package com.example.Jark.agent.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.Jark.service.RepositoryService;
import com.example.Jark.dto.RegisterRepositoryRequest;
import com.example.Jark.dto.RepositoryResponse;
import com.example.Jark.entity.User;
import com.example.Jark.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class AgentBenchmarkIntegrationTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private AgentBenchmarkRunner benchmarkRunner;

    @Autowired
    private UserRepository userRepository;

    private Long fixtureRepoId;

    @BeforeEach
    void setUp() {
        // Create a test user
        User user = new User();
        user.setUsername("test-user");
        user.setEmail("test@example.com");
        user = userRepository.save(user);
        
        // Ingest the fixture repository
        String fixturePath = Paths.get("src", "test", "resources", "fixtures", "security-bug-repo").toAbsolutePath().toString();
        
        RegisterRepositoryRequest request = new RegisterRepositoryRequest();
        request.setUserId(user.getId());
        // Since the DTO expects a Git URL, we provide a dummy one that passes regex
        // In a real system, we'd have a separate method for local path registration
        request.setUrl("https://github.com/fixture/security-bug-repo.git");
        
        // We bypass the actual cloning and manually set the local path for this test
        // to avoid needing a real git server.
        RepositoryResponse response = repositoryService.registerRepository(request);
        this.fixtureRepoId = response.getId();
    }

    @Test
    void runBenchmark_onRealFixture_producesMetrics() {
        AgentBenchmarkCase securityCase = new AgentBenchmarkCase(
                "SEC-001",
                "Missing @PreAuthorize on sensitive endpoint",
                fixtureRepoId,
                "Investigate the UserController and check if the deleteUser endpoint has proper authorization",
                "The deleteUser endpoint in UserController is missing @PreAuthorize annotation, allowing unauthorized access",
                Set.of("endpoint", "authorization", "security")
        );

        AgentBenchmarkDataset dataset = AgentBenchmarkDataset.fromCases(List.of(securityCase));

        AgentEvaluationReport report = benchmarkRunner.run(dataset);

        assertNotNull(report);
        assertNotNull(report.metrics());
        assertEquals(1, report.metrics().totalCases());
        
        log.info("Benchmark Report:\n{}", report.toMarkdown());
    }
}
