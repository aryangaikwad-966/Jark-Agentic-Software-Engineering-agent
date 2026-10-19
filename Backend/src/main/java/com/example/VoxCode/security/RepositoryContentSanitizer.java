package com.example.VoxCode.security;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/** Removes repository secrets and hostile instructions before content reaches an LLM. */
@Component
public class RepositoryContentSanitizer {

    private static final String REDACTED_SECRET = "[REDACTED_SECRET]";
    private static final String REDACTED_INSTRUCTION = "[REDACTED_UNTRUSTED_INSTRUCTION]";

    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?im)(^\\s*(?:export\\s+)?(?:[A-Z][A-Z0-9_]*(?:API_KEY|TOKEN|SECRET|PASSWORD|PASSWD|PRIVATE_KEY)|"
                    + "api[_-]?key|access[_-]?token|client[_-]?secret|password|passwd)\\s*[:=]\\s*)([^\\s#\\r\\n]+)");
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?im)(\\\"(?:api[_-]?key|access[_-]?token|client[_-]?secret|password|private[_-]?key)\\\"\\s*:\\s*\\\")(.*?)(\\\")");
    private static final Pattern PROMPT_INJECTION = Pattern.compile(
            "(?im)^.*(?:ignore\\s+(?:all\\s+)?(?:previous|prior)\\s+instructions|"
                    + "system\\s+prompt|you\\s+are\\s+(?:chatgpt|an?\\s+ai)|"
                    + "do\\s+not\\s+follow\\s+(?:the\\s+)?(?:rules|instructions)|"
                    + "reveal\\s+(?:your\\s+)?(?:secrets|instructions)).*$" );

    public String sanitizeForLlm(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String redacted = SECRET_ASSIGNMENT.matcher(content).replaceAll("$1" + REDACTED_SECRET);
        redacted = JSON_SECRET.matcher(redacted).replaceAll("$1" + REDACTED_SECRET + "$3");
        return PROMPT_INJECTION.matcher(redacted).replaceAll(REDACTED_INSTRUCTION);
    }

    public boolean isSensitiveFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        return normalized.equals(".env") || normalized.startsWith(".env.")
                || normalized.endsWith(".pem") || normalized.endsWith(".p12")
                || normalized.endsWith(".pfx") || normalized.contains("secret")
                || normalized.contains("credential") || normalized.contains("private_key");
    }
}
