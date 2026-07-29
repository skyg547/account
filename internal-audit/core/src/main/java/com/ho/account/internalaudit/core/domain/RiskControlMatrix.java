package com.ho.account.internalaudit.core.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
public class RiskControlMatrix {
    
    @Id
    private UUID rcmId;
    
    private String processName;
    private String riskDescription;
    private String controlActivity;
    private String controlOwner;
    private boolean isKeyControl;
    
    @Version
    private Long version;

    protected RiskControlMatrix() {}

    public RiskControlMatrix(String processName, String riskDescription, String controlActivity, String controlOwner, boolean isKeyControl) {
        this.rcmId = UUID.randomUUID();
        this.processName = processName;
        this.riskDescription = riskDescription;
        this.controlActivity = controlActivity;
        this.controlOwner = controlOwner;
        this.isKeyControl = isKeyControl;
    }

    public void updateControlOwner(String newOwner) {
        if (newOwner == null || newOwner.isBlank()) {
            throw new IllegalArgumentException("Control owner cannot be empty");
        }
        this.controlOwner = newOwner;
    }

    public UUID getRcmId() { return rcmId; }
    public String getProcessName() { return processName; }
    public String getRiskDescription() { return riskDescription; }
    public String getControlActivity() { return controlActivity; }
    public String getControlOwner() { return controlOwner; }
    public boolean isKeyControl() { return isKeyControl; }
}
