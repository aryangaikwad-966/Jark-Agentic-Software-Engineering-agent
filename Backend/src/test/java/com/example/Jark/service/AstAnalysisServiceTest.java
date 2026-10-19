package com.example.Jark.service;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.ast.FieldInfo;
import com.example.Jark.dto.ast.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstAnalysisServiceTest {

    private AstAnalysisService astAnalysisService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        astAnalysisService = new AstAnalysisService();
    }

    @Test
    void extractAllClasses_shouldExtractClassWithAnnotationsAndMethods() throws IOException {
        // Arrange — create a fake Spring controller source file
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("UserController.java"),
                """
                package com.example;

                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;

                @RestController
                public class UserController {

                    @GetMapping("/users")
                    public String listUsers() {
                        return "users";
                    }

                    @PostMapping("/users")
                    public String createUser(String name) {
                        return "created";
                    }
                }
                """);

        // Act
        List<ClassInfo> classes = astAnalysisService.extractAllClasses(tempDir);

        // Assert
        assertEquals(1, classes.size());
        ClassInfo classInfo = classes.get(0);
        assertEquals("UserController", classInfo.getClassName());
        assertEquals("com.example", classInfo.getPackageName());
        assertEquals("com.example.UserController", classInfo.getFullyQualifiedName());
        assertEquals("CLASS", classInfo.getClassType());
        assertEquals("public", classInfo.getAccessModifier());
        assertTrue(classInfo.getAnnotations().contains("RestController"));
        assertEquals(2, classInfo.getMethods().size());

        MethodInfo listUsers = classInfo.getMethods().stream()
                .filter(m -> m.getName().equals("listUsers"))
                .findFirst().orElseThrow();
        assertEquals("String", listUsers.getReturnType());
        assertTrue(listUsers.getAnnotations().contains("GetMapping"));
        assertTrue(listUsers.getParameterTypes().isEmpty());

        MethodInfo createUser = classInfo.getMethods().stream()
                .filter(m -> m.getName().equals("createUser"))
                .findFirst().orElseThrow();
        assertTrue(createUser.getAnnotations().contains("PostMapping"));
        assertEquals(List.of("String"), createUser.getParameterTypes());
        assertEquals(List.of("name"), createUser.getParameterNames());
    }

    @Test
    void extractAllClasses_shouldExtractServiceClass() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("UserService.java"),
                """
                package com.example;

                import org.springframework.stereotype.Service;

                @Service
                public class UserService {

                    private final String config;

                    public UserService(String config) {
                        this.config = config;
                    }

                    public String findUser(Long id) {
                        return "user-" + id;
                    }
                }
                """);

        List<ClassInfo> classes = astAnalysisService.extractAllClasses(tempDir);

        assertEquals(1, classes.size());
        ClassInfo svc = classes.get(0);
        assertEquals("UserService", svc.getClassName());
        assertTrue(svc.getAnnotations().contains("Service"));
        assertEquals(1, svc.getMethods().size()); // constructor not counted as MethodDeclaration

        // Verify field extraction
        assertFalse(svc.getFields().isEmpty());
        FieldInfo configField = svc.getFields().stream()
                .filter(f -> f.getName().equals("config"))
                .findFirst().orElseThrow();
        assertEquals("String", configField.getType());
        assertTrue(configField.isFinal());
    }

    @Test
    void findClassesWithAnnotation_shouldFilterByAnnotation() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("ControllerA.java"),
                """
                package com.example;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                public class ControllerA {}
                """);

        Files.writeString(srcDir.resolve("ServiceA.java"),
                """
                package com.example;
                import org.springframework.stereotype.Service;
                @Service
                public class ServiceA {}
                """);

        Files.writeString(srcDir.resolve("PlainClass.java"),
                """
                package com.example;
                public class PlainClass {}
                """);

        // Act
        List<ClassInfo> controllers = astAnalysisService
                .findClassesWithAnnotation(tempDir, "RestController");
        List<ClassInfo> services = astAnalysisService
                .findClassesWithAnnotation(tempDir, "Service");

        // Assert
        assertEquals(1, controllers.size());
        assertEquals("ControllerA", controllers.get(0).getClassName());

        assertEquals(1, services.size());
        assertEquals("ServiceA", services.get(0).getClassName());
    }

    @Test
    void findMethodsWithAnnotation_shouldReturnClassesWithMatchingMethods() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("MyController.java"),
                """
                package com.example;
                import org.springframework.web.bind.annotation.GetMapping;
                public class MyController {
                    @GetMapping
                    public String index() { return "ok"; }
                    public String helper() { return "nope"; }
                }
                """);

        List<ClassInfo> results = astAnalysisService
                .findMethodsWithAnnotation(tempDir, "GetMapping");

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getMethods().size());
        assertEquals("index", results.get(0).getMethods().get(0).getName());
    }

    @Test
    void findClassByName_shouldReturnMatchingClass() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Foo.java"),
                """
                package com.example;
                public class Foo {}
                """);
        Files.writeString(srcDir.resolve("Bar.java"),
                """
                package com.example;
                public class Bar {}
                """);

        List<ClassInfo> results = astAnalysisService.findClassByName(tempDir, "Bar");

        assertEquals(1, results.size());
        assertEquals("Bar", results.get(0).getClassName());
    }

    @Test
    void findMethodByName_shouldReturnClassesContainingMethod() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Alpha.java"),
                """
                package com.example;
                public class Alpha {
                    public void doWork() {}
                    public void other() {}
                }
                """);
        Files.writeString(srcDir.resolve("Beta.java"),
                """
                package com.example;
                public class Beta {
                    public void unrelated() {}
                }
                """);

        List<ClassInfo> results = astAnalysisService.findMethodByName(tempDir, "doWork");

        assertEquals(1, results.size());
        assertEquals("Alpha", results.get(0).getClassName());
        assertEquals(1, results.get(0).getMethods().size());
    }

    @Test
    void extractAllClasses_shouldHandleInterfacesAndEnums() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("MyInterface.java"),
                """
                package com.example;
                public interface MyInterface {
                    void doSomething();
                }
                """);

        Files.writeString(srcDir.resolve("Status.java"),
                """
                package com.example;
                public enum Status {
                    ACTIVE, INACTIVE;
                }
                """);

        List<ClassInfo> classes = astAnalysisService.extractAllClasses(tempDir);

        assertEquals(2, classes.size());

        ClassInfo iface = classes.stream()
                .filter(c -> c.getClassName().equals("MyInterface"))
                .findFirst().orElseThrow();
        assertEquals("INTERFACE", iface.getClassType());

        ClassInfo enumType = classes.stream()
                .filter(c -> c.getClassName().equals("Status"))
                .findFirst().orElseThrow();
        assertEquals("ENUM", enumType.getClassType());
    }

    @Test
    void extractAllClasses_shouldHandleInheritance() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Child.java"),
                """
                package com.example;
                public class Child extends Parent implements Runnable {
                }
                """);

        List<ClassInfo> classes = astAnalysisService.extractAllClasses(tempDir);

        ClassInfo child = classes.stream()
                .filter(c -> c.getClassName().equals("Child"))
                .findFirst().orElseThrow();
        assertEquals("Parent", child.getSuperClass());
        assertTrue(child.getImplementedInterfaces().contains("Runnable"));
    }

    @Test
    void extractAllClasses_shouldReturnEmptyForNoJavaFiles() {
        List<ClassInfo> results = astAnalysisService.extractAllClasses(tempDir);
        assertTrue(results.isEmpty());
    }

    @Test
    void extractAllClasses_shouldRecordLineNumbers() throws IOException {
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Lined.java"),
                """
                package com.example;

                public class Lined {
                    public void method() {
                        // body
                    }
                }
                """);

        List<ClassInfo> classes = astAnalysisService.extractAllClasses(tempDir);
        ClassInfo lined = classes.get(0);

        assertTrue(lined.getStartLine() > 0);
        assertTrue(lined.getEndLine() >= lined.getStartLine());

        MethodInfo method = lined.getMethods().get(0);
        assertTrue(method.getStartLine() > 0);
        assertTrue(method.getEndLine() >= method.getStartLine());
    }
}
