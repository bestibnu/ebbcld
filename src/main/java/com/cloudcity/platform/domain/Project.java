package com.cloudcity.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project extends CreatedUpdatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "monthly_budget", precision = 14, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "budget_warning_threshold", precision = 5, scale = 2)
    private BigDecimal budgetWarningThreshold;

    public UUID getId() {
        return id;
    }

    public Org getOrg() {
        return org;
    }

    public void setOrg(Org org) {
        this.org = org;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    public void setBudgetWarningThreshold(BigDecimal budgetWarningThreshold) {
        this.budgetWarningThreshold = budgetWarningThreshold;
    }
}
