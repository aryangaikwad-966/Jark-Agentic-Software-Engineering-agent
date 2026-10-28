package com.example.Jark.dto.rollback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot metadata preserving the state of a repository workspace prior to remediation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceSnapshot {

    private Long remediationId;
    private String workspacePath;
    private String snapshotPath;
    private String createdAt;
    private int fileCount;
    
    @Builder.Default
    private Map<String, String> fileChecksums = new HashMap<>();
    
    private String gitCommitSha;
    private String gitBranch;
}
