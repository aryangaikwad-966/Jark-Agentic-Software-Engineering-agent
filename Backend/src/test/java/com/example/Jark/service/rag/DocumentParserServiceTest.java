package com.example.Jark.service.rag;

import com.example.Jark.dto.rag.CodeChunk;
import com.example.Jark.dto.rag.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParserServiceTest {

    @TempDir
    private Path tempWorkspace;

    private DocumentParserService parserService;

    @BeforeEach
    void setUp() throws IOException {
        parserService = new DocumentParserService();

        // 1. Create a Java source file
        Path srcDir = tempWorkspace.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("PaymentController.java"), """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.PostMapping;

                /**
                 * Handles payment transactions.
                 */
                @RestController
                public class PaymentController {

                    private final PaymentService paymentService;

                    public PaymentController(PaymentService paymentService) {
                        this.paymentService = paymentService;
                    }

                    @PostMapping("/payments")
                    public String processPayment(String orderId) {
                        return paymentService.executePayment(orderId);
                    }
                }
                """);

        // 2. Create a Java test file
        Path testDir = tempWorkspace.resolve("src/test/java/com/example");
        Files.createDirectories(testDir);
        Files.writeString(testDir.resolve("PaymentControllerTest.java"), """
                package com.example;

                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertNotNull;

                class PaymentControllerTest {

                    @Test
                    void testProcessPayment() {
                        assertNotNull("ok");
                    }
                }
                """);

        // 3. Create a Markdown documentation file
        Files.writeString(tempWorkspace.resolve("README.md"), """
                # Jark System

                System overview and architecture documentation.

                ## Payment Processing

                Payments are handled through asynchronous gateways.
                """);

        // 4. Create a Configuration file
        Files.writeString(tempWorkspace.resolve("application.yml"), """
                server:
                  port: 8080
                spring:
                  application:
                    name: payment-app
                """);
    }

    @Test
    void parseRepository_extractsAllDocumentTypes() {
        List<CodeChunk> chunks = parserService.parseRepository(100L, tempWorkspace);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // Verify SOURCE_CODE chunks
        List<CodeChunk> sourceChunks = chunks.stream()
                .filter(c -> c.getDocumentType() == DocumentType.SOURCE_CODE)
                .toList();
        assertFalse(sourceChunks.isEmpty());
        assertTrue(sourceChunks.stream().anyMatch(c -> "com.example.PaymentController".equals(c.getClassName())));
        assertTrue(sourceChunks.stream().anyMatch(c -> "processPayment".equals(c.getMethodName())));

        // Verify TEST chunks
        List<CodeChunk> testChunks = chunks.stream()
                .filter(c -> c.getDocumentType() == DocumentType.TEST)
                .toList();
        assertFalse(testChunks.isEmpty());
        assertTrue(testChunks.stream().anyMatch(c -> "testProcessPayment".equals(c.getMethodName())));

        // Verify DOCUMENTATION chunks
        List<CodeChunk> docChunks = chunks.stream()
                .filter(c -> c.getDocumentType() == DocumentType.DOCUMENTATION)
                .toList();
        assertEquals(2, docChunks.size());
        assertTrue(docChunks.stream().anyMatch(c -> "Jark System".equals(c.getSymbolInfo())));
        assertTrue(docChunks.stream().anyMatch(c -> "Payment Processing".equals(c.getSymbolInfo())));

        // Verify CONFIGURATION chunks
        List<CodeChunk> configChunks = chunks.stream()
                .filter(c -> c.getDocumentType() == DocumentType.CONFIGURATION)
                .toList();
        assertFalse(configChunks.isEmpty());
        assertEquals("application.yml", configChunks.get(0).getFilePath());
    }

    @Test
    void parseJavaFile_capturesMethodLineNumbersAndAnnotations() throws IOException {
        Path controllerFile = tempWorkspace.resolve("src/main/java/com/example/PaymentController.java");
        List<CodeChunk> chunks = parserService.parseFile(100L, tempWorkspace, controllerFile);

        CodeChunk methodChunk = chunks.stream()
                .filter(c -> "processPayment".equals(c.getMethodName()))
                .findFirst()
                .orElseThrow();

        assertEquals("com.example.PaymentController", methodChunk.getClassName());
        assertTrue(methodChunk.getStartLine() > 1);
        assertTrue(methodChunk.getEndLine() >= methodChunk.getStartLine());
        assertTrue(methodChunk.getSymbolInfo().contains("PostMapping"));
        assertTrue(methodChunk.getContent().contains("executePayment(orderId)"));
    }
}
