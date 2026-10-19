package com.example.Jark.service;

import com.example.Jark.dto.ast.ClassInfo;
import com.example.Jark.dto.ast.FieldInfo;
import com.example.Jark.dto.ast.MethodInfo;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Extracts deterministic structural intelligence from Java source files
 * using JavaParser AST analysis. This service provides the foundation
 * for agent tools that query repository structure.
 *
 * <p>All methods are read-only and do not modify source code.</p>
 */
@Slf4j
@Service
public class AstAnalysisService {

    private final JavaParser parser;

    public AstAnalysisService() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.parser = new JavaParser(config);
    }

    /**
     * Extracts all class/interface/enum metadata from Java source files
     * found under the given workspace path.
     *
     * @param workspacePath root directory of the cloned repository
     * @return list of ClassInfo representing every discovered type
     */
    public List<ClassInfo> extractAllClasses(Path workspacePath) {
        List<Path> javaFiles = findJavaFiles(workspacePath);
        List<ClassInfo> results = new ArrayList<>();

        for (Path file : javaFiles) {
            try {
                List<ClassInfo> classes = parseFile(file, workspacePath);
                results.addAll(classes);
            } catch (Exception e) {
                log.warn("Failed to parse file {}: {}", file, e.getMessage());
            }
        }

        log.info("Extracted {} classes from {} Java files in {}",
                results.size(), javaFiles.size(), workspacePath);
        return results;
    }

    /**
     * Finds all classes annotated with a specific annotation name.
     *
     * @param workspacePath root directory of the cloned repository
     * @param annotationName simple name of the annotation (e.g. "RestController")
     * @return list of ClassInfo matching the annotation
     */
    public List<ClassInfo> findClassesWithAnnotation(Path workspacePath, String annotationName) {
        return extractAllClasses(workspacePath).stream()
                .filter(c -> c.getAnnotations().stream()
                        .anyMatch(a -> a.equals(annotationName)
                                || a.endsWith("." + annotationName)))
                .toList();
    }

    /**
     * Finds all methods annotated with a specific annotation across
     * all classes in the workspace.
     *
     * @param workspacePath root directory of the cloned repository
     * @param annotationName simple name of the annotation (e.g. "GetMapping")
     * @return list of ClassInfo containing only matching methods
     */
    public List<ClassInfo> findMethodsWithAnnotation(Path workspacePath, String annotationName) {
        List<ClassInfo> allClasses = extractAllClasses(workspacePath);
        List<ClassInfo> results = new ArrayList<>();

        for (ClassInfo classInfo : allClasses) {
            List<MethodInfo> matchingMethods = classInfo.getMethods().stream()
                    .filter(m -> m.getAnnotations().stream()
                            .anyMatch(a -> a.equals(annotationName)
                                    || a.endsWith("." + annotationName)))
                    .toList();

            if (!matchingMethods.isEmpty()) {
                ClassInfo filtered = ClassInfo.builder()
                        .packageName(classInfo.getPackageName())
                        .className(classInfo.getClassName())
                        .fullyQualifiedName(classInfo.getFullyQualifiedName())
                        .filePath(classInfo.getFilePath())
                        .classType(classInfo.getClassType())
                        .accessModifier(classInfo.getAccessModifier())
                        .annotations(classInfo.getAnnotations())
                        .implementedInterfaces(classInfo.getImplementedInterfaces())
                        .superClass(classInfo.getSuperClass())
                        .methods(matchingMethods)
                        .fields(classInfo.getFields())
                        .imports(classInfo.getImports())
                        .startLine(classInfo.getStartLine())
                        .endLine(classInfo.getEndLine())
                        .build();
                results.add(filtered);
            }
        }
        return results;
    }

    /**
     * Finds a specific class by its simple name.
     *
     * @param workspacePath root directory of the cloned repository
     * @param className simple class name (e.g. "UserService")
     * @return list of ClassInfo matching the name (may be multiple in different packages)
     */
    public List<ClassInfo> findClassByName(Path workspacePath, String className) {
        return extractAllClasses(workspacePath).stream()
                .filter(c -> c.getClassName().equals(className))
                .toList();
    }

    /**
     * Finds a specific method by name across all classes.
     *
     * @param workspacePath root directory of the cloned repository
     * @param methodName method name to search for
     * @return list of ClassInfo containing matching methods
     */
    public List<ClassInfo> findMethodByName(Path workspacePath, String methodName) {
        List<ClassInfo> allClasses = extractAllClasses(workspacePath);
        List<ClassInfo> results = new ArrayList<>();

        for (ClassInfo classInfo : allClasses) {
            List<MethodInfo> matchingMethods = classInfo.getMethods().stream()
                    .filter(m -> m.getName().equals(methodName))
                    .toList();

            if (!matchingMethods.isEmpty()) {
                ClassInfo filtered = ClassInfo.builder()
                        .packageName(classInfo.getPackageName())
                        .className(classInfo.getClassName())
                        .fullyQualifiedName(classInfo.getFullyQualifiedName())
                        .filePath(classInfo.getFilePath())
                        .classType(classInfo.getClassType())
                        .accessModifier(classInfo.getAccessModifier())
                        .annotations(classInfo.getAnnotations())
                        .implementedInterfaces(classInfo.getImplementedInterfaces())
                        .superClass(classInfo.getSuperClass())
                        .methods(matchingMethods)
                        .fields(classInfo.getFields())
                        .imports(classInfo.getImports())
                        .startLine(classInfo.getStartLine())
                        .endLine(classInfo.getEndLine())
                        .build();
                results.add(filtered);
            }
        }
        return results;
    }

    // ── Internal parsing logic ──────────────────────────────────────────

    /**
     * Parses a single Java file and extracts ClassInfo for all types declared in it.
     */
    List<ClassInfo> parseFile(Path file, Path workspacePath) throws IOException {
        String source = Files.readString(file);
        var parseResult = parser.parse(source);

        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            log.warn("Parse unsuccessful for {}: {}", file, parseResult.getProblems());
            return Collections.emptyList();
        }

        CompilationUnit cu = parseResult.getResult().get();
        String relativePath = workspacePath.relativize(file).toString();
        List<String> imports = extractImports(cu);

        List<ClassInfo> results = new ArrayList<>();

        // Process class/interface declarations
        for (ClassOrInterfaceDeclaration decl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            results.add(buildClassInfo(decl, cu, relativePath, imports));
        }

        // Process enum declarations
        for (EnumDeclaration decl : cu.findAll(EnumDeclaration.class)) {
            results.add(buildEnumInfo(decl, cu, relativePath, imports));
        }

        return results;
    }

    private ClassInfo buildClassInfo(ClassOrInterfaceDeclaration decl,
                                     CompilationUnit cu,
                                     String relativePath,
                                     List<String> imports) {
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        String className = decl.getNameAsString();
        String fqn = packageName.isEmpty() ? className : packageName + "." + className;
        String classType = decl.isInterface() ? "INTERFACE" : "CLASS";

        List<String> annotations = decl.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        List<String> interfaces = decl.getImplementedTypes().stream()
                .map(ClassOrInterfaceType::getNameAsString)
                .toList();

        String superClass = decl.getExtendedTypes().stream()
                .findFirst()
                .map(ClassOrInterfaceType::getNameAsString)
                .orElse(null);

        String accessModifier = extractAccessModifier(decl);

        List<MethodInfo> methods = decl.getMethods().stream()
                .map(this::buildMethodInfo)
                .toList();

        List<FieldInfo> fields = decl.getFields().stream()
                .flatMap(this::buildFieldInfos)
                .toList();

        int startLine = decl.getBegin().map(p -> p.line).orElse(0);
        int endLine = decl.getEnd().map(p -> p.line).orElse(0);

        return ClassInfo.builder()
                .packageName(packageName)
                .className(className)
                .fullyQualifiedName(fqn)
                .filePath(relativePath)
                .classType(classType)
                .accessModifier(accessModifier)
                .annotations(annotations)
                .implementedInterfaces(interfaces)
                .superClass(superClass)
                .methods(methods)
                .fields(fields)
                .imports(imports)
                .startLine(startLine)
                .endLine(endLine)
                .build();
    }

    private ClassInfo buildEnumInfo(EnumDeclaration decl,
                                    CompilationUnit cu,
                                    String relativePath,
                                    List<String> imports) {
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        String className = decl.getNameAsString();
        String fqn = packageName.isEmpty() ? className : packageName + "." + className;

        List<String> annotations = decl.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        String accessModifier = extractAccessModifier(decl);

        List<MethodInfo> methods = decl.getMethods().stream()
                .map(this::buildMethodInfo)
                .toList();

        List<FieldInfo> fields = decl.getFields().stream()
                .flatMap(this::buildFieldInfos)
                .toList();

        int startLine = decl.getBegin().map(p -> p.line).orElse(0);
        int endLine = decl.getEnd().map(p -> p.line).orElse(0);

        return ClassInfo.builder()
                .packageName(packageName)
                .className(className)
                .fullyQualifiedName(fqn)
                .filePath(relativePath)
                .classType("ENUM")
                .accessModifier(accessModifier)
                .annotations(annotations)
                .implementedInterfaces(Collections.emptyList())
                .superClass(null)
                .methods(methods)
                .fields(fields)
                .imports(imports)
                .startLine(startLine)
                .endLine(endLine)
                .build();
    }

    private MethodInfo buildMethodInfo(MethodDeclaration method) {
        List<String> paramTypes = method.getParameters().stream()
                .map(p -> p.getType().asString())
                .toList();

        List<String> paramNames = method.getParameters().stream()
                .map(p -> p.getNameAsString())
                .toList();

        List<String> annotations = method.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        List<String> methodCalls = new ArrayList<>();
        method.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).forEach(mc -> {
            // Get the name of the method being called (note: this is just the simple name, not the FQN)
            methodCalls.add(mc.getNameAsString());
        });

        int startLine = method.getBegin().map(p -> p.line).orElse(0);
        int endLine = method.getEnd().map(p -> p.line).orElse(0);

        return MethodInfo.builder()
                .name(method.getNameAsString())
                .returnType(method.getType().asString())
                .parameterTypes(paramTypes)
                .parameterNames(paramNames)
                .annotations(annotations)
                .methodCalls(methodCalls)
                .accessModifier(extractAccessModifier(method))
                .isStatic(method.isStatic())
                .isAbstract(method.isAbstract())
                .startLine(startLine)
                .endLine(endLine)
                .build();
    }

    private Stream<FieldInfo> buildFieldInfos(FieldDeclaration field) {
        List<String> annotations = field.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        return field.getVariables().stream().map(var -> {
            int lineNumber = var.getBegin().map(p -> p.line).orElse(0);
            return FieldInfo.builder()
                    .name(var.getNameAsString())
                    .type(var.getType().asString())
                    .accessModifier(extractAccessModifier(field))
                    .annotations(annotations)
                    .isStatic(field.isStatic())
                    .isFinal(field.isFinal())
                    .lineNumber(lineNumber)
                    .build();
        });
    }

    private List<String> extractImports(CompilationUnit cu) {
        return cu.getImports().stream()
                .map(ImportDeclaration::getNameAsString)
                .toList();
    }

    private String extractAccessModifier(com.github.javaparser.ast.body.BodyDeclaration<?> node) {
        if (node instanceof com.github.javaparser.ast.body.TypeDeclaration<?> td) {
            if (td.hasModifier(Modifier.Keyword.PUBLIC)) return "public";
            if (td.hasModifier(Modifier.Keyword.PROTECTED)) return "protected";
            if (td.hasModifier(Modifier.Keyword.PRIVATE)) return "private";
        } else if (node instanceof MethodDeclaration md) {
            if (md.hasModifier(Modifier.Keyword.PUBLIC)) return "public";
            if (md.hasModifier(Modifier.Keyword.PROTECTED)) return "protected";
            if (md.hasModifier(Modifier.Keyword.PRIVATE)) return "private";
        } else if (node instanceof FieldDeclaration fd) {
            if (fd.hasModifier(Modifier.Keyword.PUBLIC)) return "public";
            if (fd.hasModifier(Modifier.Keyword.PROTECTED)) return "protected";
            if (fd.hasModifier(Modifier.Keyword.PRIVATE)) return "private";
        }
        return "package-private";
    }

    /**
     * Recursively finds all .java files under the given path,
     * focusing on src/main/java and src/test/java directories.
     */
    List<Path> findJavaFiles(Path workspacePath) {
        // Prefer standard Maven source directories if they exist
        Path mainJava = workspacePath.resolve("src/main/java");
        Path testJava = workspacePath.resolve("src/test/java");

        List<Path> searchRoots = new ArrayList<>();
        if (Files.isDirectory(mainJava)) {
            searchRoots.add(mainJava);
        }
        if (Files.isDirectory(testJava)) {
            searchRoots.add(testJava);
        }

        // Fallback: scan the entire workspace if no standard layout found
        if (searchRoots.isEmpty()) {
            searchRoots.add(workspacePath);
        }

        List<Path> javaFiles = new ArrayList<>();
        for (Path root : searchRoots) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(javaFiles::add);
            } catch (IOException e) {
                log.warn("Failed to walk directory {}: {}", root, e.getMessage());
            }
        }
        return javaFiles;
    }
}
