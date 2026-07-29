package com.ho.account.internalaudit.core.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
public class DesignAssessment {
    
    @Id
    private UUID assessmentId;
    
    private UUID rcmId;
    private String evaluator;
    
    @Enumerated(EnumType.STRING)
    private AssessmentStatus status;
    
    private String evaluationNote;
    private boolean isEffectivelyDesigned;
    
    @Version
    private Long version;

    protected DesignAssessment() {}

    public DesignAssessment(UUID rcmId, String evaluator) {
        this.assessmentId = UUID.randomUUID();
        this.rcmId = rcmId;
        this.evaluator = evaluator;
        this.status = AssessmentStatus.DRAFT;
    }

    public void evaluate(String note, boolean isEffectivelyDesigned) {
        if (this.status == AssessmentStatus.APPROVED || this.status == AssessmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot evaluate when status is " + this.status);
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Evaluation note must be provided.");
        }
        this.evaluationNote = note;
        this.isEffectivelyDesigned = isEffectivelyDesigned;
        this.status = AssessmentStatus.IN_PROGRESS;
    }

    public void submitForApproval() {
        if (this.status != AssessmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS assessments can be submitted.");
        }
        this.status = AssessmentStatus.PENDING_APPROVAL;
    }

    public void approve() {
        if (this.status != AssessmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Assessment must be PENDING_APPROVAL to be approved.");
        }
        this.status = AssessmentStatus.APPROVED;
    }

    public void reject(String reason) {
        if (this.status != AssessmentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Assessment must be PENDING_APPROVAL to be rejected.");
        }
        this.status = AssessmentStatus.REJECTED;
        this.evaluationNote += " | REJECTION REASON: " + reason;
    }

    public UUID getAssessmentId() { return assessmentId; }
    public UUID getRcmId() { return rcmId; }
    public AssessmentStatus getStatus() { return status; }
    public boolean isEffectivelyDesigned() { return isEffectivelyDesigned; }
}
