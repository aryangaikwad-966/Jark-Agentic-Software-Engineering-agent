package com.example.Jark.dto.rollback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of a workspace rollback operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackResult {

    private Long remediationId;
    private String triggerReason;
    private boolean restorationVerified;
    private int filesRestored;
    private int filesDeleted;
    private String workspacePath;
    private String snapshotPath;
    private String timestamp;
    private boolean success;
    private String errorMessage;
}
