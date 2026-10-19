package com.example.VoxCode.service.rag;

import com.example.VoxCode.dto.rag.CodeChunk;
import com.example.VoxCode.dto.rag.DocumentType;
import com.example.VoxCode.security.RepositoryContentSanitizer;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses and chunks repository source code, documentation, configuration, and test files
 * into semantically coherent CodeChunks for the RAG pipeline.
 */
@Slf4j
@Service
public class DocumentParserService {

    private static final Set<String> EXCLUDED_DIRS = Set.of(
            ".git", "target", "build", ".idea", ".vscode", "node_modules", ".gradle", "bin"
    );

    private static final int DEFAULT_WINDOW_LINES = 40;
    private static final int DEFAULT_OVERLAP_LINES = 8;

    private final JavaParser javaParser;
    private final RepositoryContentSanitizer contentSanitizer;

    public DocumentParserService() {
        this(new RepositoryContentSanitizer());
    }

    @Autowired
    public DocumentParserService(RepositoryContentSanitizer contentSanitizer) {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.javaParser = new JavaParser(config);
        this.contentSanitizer = contentSanitizer;
    }

    /**
     * Parses an entire workspace into discrete semantic CodeChunks.
     *
     * @param repositoryId identifier of the repository
     * @param workspacePath root directory of the repository
     * @return list of parsed CodeChunks
     */
    public List<CodeChunk> parseRepository(Long repositoryId, Path workspacePath) {
        if (workspacePath == null || !Files.exists(workspacePath)) {
            return Collections.emptyList();
        }

        List<CodeChunk> chunks = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(workspacePath)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !isExcluded(workspacePath, path))
                    .toList();

            for (Path file : files) {
                try {
                    chunks.addAll(parseFile(repositoryId, workspacePath, file));
                } catch (Exception e) {
                    log.warn("Failed to parse file {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error walking workspace directory {}: {}", workspacePath, e.getMessage());
        }

        log.info("Parsed {} chunks from repository {} at {}", chunks.size(), repositoryId, workspacePath);
        return chunks;
    }

    /**
     * Parses a single file into chunks based on its document classification.
     */
    public List<CodeChunk> parseFile(Long repositoryId, Path workspacePath, Path file) throws IOException {
        String relativePath = workspacePath.relativize(file).toString().replace('\\', '/');
        String fileName = fileName(file, relativePath).toLowerCase();

        if (fileName.endsWith(".java")) {
            return parseJavaFile(repositoryId, relativePath, file);
        } else if (fileName.endsWith(".md") || fileName.endsWith(".markdown")) {
            return parseMarkdownFile(repositoryId, relativePath, file);
        } else if (isConfigFile(fileName, relativePath)) {
            return parseConfigFile(repositoryId, relativePath, file);
        } else {
            return parseGenericTextFile(repositoryId, relativePath, file);
        }
    }

    private List<CodeChunk> parseJavaFile(Long repositoryId, String relativePath, Path file) throws IOException {
        List<CodeChunk> chunks = new ArrayList<>();
        String currentFileName = fileName(file, relativePath);
        String content = sanitize(currentFileName, Files.readString(file, StandardCharsets.UTF_8));
        boolean isTest = relativePath.contains("/test/") || currentFileName.endsWith("Test.java");
        DocumentType docType = isTest ? DocumentType.TEST : DocumentType.SOURCE_CODE;

        ParseResult<CompilationUnit> parseResult = javaParser.parse(content);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            return chunkTextByLines(repositoryId, relativePath, content, docType, null, null, null);
        }

        CompilationUnit cu = parseResult.getResult().get();
        for (TypeDeclaration<?> type : cu.getTypes()) {
            String className = type.getFullyQualifiedName().orElse(type.getNameAsString());
            String classAnnotations = type.getAnnotations().stream()
                    .map(AnnotationExpr::getNameAsString)
                    .collect(Collectors.joining(", "));

            // Class Header chunk
            int headerStart = type.getBegin().map(p -> p.line).orElse(1);
            int firstMethodLine = findFirstMethodLine(type).orElse(type.getEnd().map(p -> p.line).orElse(headerStart));
            String headerContent = extractLines(content, headerStart, firstMethodLine);

            chunks.add(createChunk(
                    repositoryId, docType, relativePath, className, null,
                    classAnnotations.isEmpty() ? null : classAnnotations,
                    new int[]{headerStart, firstMethodLine}, headerContent
            ));

            // Method chunks
            for (BodyDeclaration<?> member : type.getMembers()) {
                if (member instanceof MethodDeclaration method) {
                    chunks.add(buildCallableChunk(repositoryId, docType, relativePath, className, method, content));
                } else if (member instanceof ConstructorDeclaration ctor) {
                    chunks.add(buildCallableChunk(repositoryId, docType, relativePath, className, ctor, content));
                }
            }
        }

        // Standalone or file-level comments
        for (Comment comment : cu.getAllComments()) {
            if (comment.isOrphan() && comment.getContent().strip().length() > 30) {
                int start = comment.getBegin().map(p -> p.line).orElse(1);
                int end = comment.getEnd().map(p -> p.line).orElse(start);
                chunks.add(createChunk(
                        repositoryId, DocumentType.COMMENT, relativePath, null, null,
                        "comment", new int[]{start, end}, comment.getContent()
                ));
            }
        }

        return chunks;
    }

    private CodeChunk buildCallableChunk(
            Long repoId, DocumentType docType, String path, String className,
            CallableDeclaration<?> callable, String fullContent) {
        int start = callable.getBegin().map(p -> p.line).orElse(1);
        int end = callable.getEnd().map(p -> p.line).orElse(start);
        String annotations = callable.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .collect(Collectors.joining(", "));
        String callableContent = extractLines(fullContent, start, end);
        String name = callable.getNameAsString();

        return createChunk(
                repoId, docType, path, className, name,
                annotations.isEmpty() ? null : annotations,
                new int[]{start, end}, callableContent
        );
    }

    private List<CodeChunk> parseMarkdownFile(Long repositoryId, String relativePath, Path file) throws IOException {
        List<String> lines = List.of(sanitize(fileName(file, relativePath), Files.readString(file, StandardCharsets.UTF_8))
                .split("\\r?\\n", -1));
        List<CodeChunk> chunks = new ArrayList<>();

        int sectionStart = 1;
        String currentHeading = "Overview";
        StringBuilder sectionBuffer = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;

            if (line.startsWith("#")) {
                if (sectionBuffer.length() > 0) {
                    chunks.add(createChunk(
                            repositoryId, DocumentType.DOCUMENTATION, relativePath, null, null,
                            currentHeading, new int[]{sectionStart, lineNumber - 1}, sectionBuffer.toString()
                    ));
                    sectionBuffer.setLength(0);
                    sectionStart = lineNumber;
                }
                currentHeading = line.replaceAll("^#+\\s*", "").strip();
            }
            sectionBuffer.append(line).append("\n");
        }

        if (sectionBuffer.length() > 0) {
            chunks.add(createChunk(
                    repositoryId, DocumentType.DOCUMENTATION, relativePath, null, null,
                    currentHeading, new int[]{sectionStart, lines.size()}, sectionBuffer.toString()
            ));
        }

        return chunks;
    }

    private List<CodeChunk> parseConfigFile(Long repositoryId, String relativePath, Path file) throws IOException {
        String content = sanitize(fileName(file, relativePath), Files.readString(file, StandardCharsets.UTF_8));
        return chunkTextByLines(repositoryId, relativePath, content, DocumentType.CONFIGURATION, null, null, "config");
    }

    private List<CodeChunk> parseGenericTextFile(Long repositoryId, String relativePath, Path file) throws IOException {
        String content = sanitize(fileName(file, relativePath), Files.readString(file, StandardCharsets.UTF_8));
        return chunkTextByLines(repositoryId, relativePath, content, DocumentType.DOCUMENTATION, null, null, "text");
    }

    private List<CodeChunk> chunkTextByLines(
            Long repositoryId, String relativePath, String content,
            DocumentType docType, String className, String methodName, String symbolInfo) {
        List<CodeChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\r?\n", -1);
        int totalLines = lines.length;

        if (totalLines <= DEFAULT_WINDOW_LINES) {
            chunks.add(createChunk(repositoryId, docType, relativePath, className, methodName, symbolInfo,
                    new int[]{1, totalLines}, content));
            return chunks;
        }

        int start = 0;
        while (start < totalLines) {
            int end = Math.min(start + DEFAULT_WINDOW_LINES, totalLines);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(lines[i]).append("\n");
            }
            chunks.add(createChunk(repositoryId, docType, relativePath, className, methodName, symbolInfo,
                    new int[]{start + 1, end}, sb.toString()));
            if (end >= totalLines) {
                break;
            }
            start += (DEFAULT_WINDOW_LINES - DEFAULT_OVERLAP_LINES);
        }
        return chunks;
    }

    private CodeChunk createChunk(
            Long repositoryId, DocumentType docType, String filePath,
            String className, String methodName, String symbolInfo,
            int[] lineRange, String content) {
        int startLine = lineRange[0];
        int endLine = lineRange[1];
        String chunkId = generateId(repositoryId, filePath, startLine, content);
        return CodeChunk.builder()
                .id(chunkId)
                .repositoryId(repositoryId)
                .documentType(docType)
                .filePath(filePath)
                .className(className)
                .methodName(methodName)
                .symbolInfo(symbolInfo)
                .startLine(startLine)
                .endLine(endLine)
                .content(content)
                .build();
    }

    private Optional<Integer> findFirstMethodLine(TypeDeclaration<?> type) {
        return type.getMembers().stream()
                .filter(m -> m instanceof MethodDeclaration || m instanceof ConstructorDeclaration)
                .findFirst()
                .flatMap(BodyDeclaration::getBegin)
                .map(p -> p.line);
    }

    private String extractLines(String fullText, int startLine, int endLine) {
        String[] allLines = fullText.split("\r?\n", -1);
        int from = Math.max(0, startLine - 1);
        int to = Math.min(allLines.length, endLine);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(allLines[i]).append("\n");
        }
        return sb.toString();
    }

    private boolean isExcluded(Path workspaceRoot, Path path) {
        Path relative = workspaceRoot.relativize(path);
        for (Path part : relative) {
            if (EXCLUDED_DIRS.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isConfigFile(String fileName, String relativePath) {
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml")
                || fileName.endsWith(".properties") || fileName.equals("pom.xml")
                || fileName.endsWith(".json") || fileName.endsWith(".xml");
    }

    private String sanitize(String fileName, String content) {
        if (contentSanitizer.isSensitiveFileName(fileName)) {
            return "[REDACTED_SENSITIVE_FILE]";
        }
        return contentSanitizer.sanitizeForLlm(content);
    }

    private String fileName(Path file, String fallback) {
        Path leaf = file.getFileName();
        return leaf != null ? leaf.toString() : fallback;
    }

    private String generateId(Long repoId, String filePath, int startLine, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = String.format("%d:%s:%d:%s", repoId, filePath, startLine, content);
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            return String.format("%d_%s_%d", repoId, filePath.replace('/', '_'), startLine);
        }
    }
}
