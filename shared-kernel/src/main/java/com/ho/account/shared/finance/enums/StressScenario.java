package com.ho.account.shared.finance.enums;

import lombok.Getter;

@Getter
public enum StressScenario {
    BASELINE("Baseline", "No additional stress", 1.0d, 1.0d),
    LIGHT_RECESSION("Light recession", "Moderate macroeconomic downturn", 1.25d, 1.10d),
    SEVERE_SHOCK("Severe shock", "Severe credit and collateral stress", 1.75d, 1.30d),
    SEVERE_CRISIS("Severe crisis", "Systemic financial crisis scenario", 2.00d, 1.50d),
    INTEREST_RATE_SHOCK("Interest rate shock", "Rapid rate increase and refinancing stress", 1.40d, 1.15d);

    private final String name;
    private final String description;
    private final double pdMultiplier;
    private final double lgdMultiplier;

    StressScenario(String name, String description, double pdMultiplier, double lgdMultiplier) {
        this.name = name;
        this.description = description;
        this.pdMultiplier = pdMultiplier;
        this.lgdMultiplier = lgdMultiplier;
    }
}
