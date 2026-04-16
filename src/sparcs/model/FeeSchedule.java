package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fee Schedule Model
 */
public class FeeSchedule {
    private int feeId;
    private BigDecimal ratePerHour;
    private BigDecimal ratePerDay;
    private int gracePeriodMinutes;
    private LocalDateTime effectiveDate;
    private boolean isActive;
    private Integer createdBy;

    public FeeSchedule() {}

    public FeeSchedule(BigDecimal ratePerHour, BigDecimal ratePerDay) {
        this.ratePerHour = ratePerHour;
        this.ratePerDay = ratePerDay;
        this.gracePeriodMinutes = 15;
        this.isActive = true;
    }

    // Getters and Setters
    public int getFeeId() { return feeId; }
    public void setFeeId(int feeId) { this.feeId = feeId; }

    public BigDecimal getRatePerHour() { return ratePerHour; }
    public void setRatePerHour(BigDecimal ratePerHour) { this.ratePerHour = ratePerHour; }

    public BigDecimal getRatePerDay() { return ratePerDay; }
    public void setRatePerDay(BigDecimal ratePerDay) { this.ratePerDay = ratePerDay; }

    public int getGracePeriodMinutes() { return gracePeriodMinutes; }
    public void setGracePeriodMinutes(int gracePeriodMinutes) { this.gracePeriodMinutes = gracePeriodMinutes; }

    public LocalDateTime getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDateTime effectiveDate) { this.effectiveDate = effectiveDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    @Override
    public String toString() {
        return "FeeSchedule{" +
                "feeId=" + feeId +
                ", ratePerHour=" + ratePerHour +
                ", ratePerDay=" + ratePerDay +
                ", gracePeriodMinutes=" + gracePeriodMinutes +
                ", isActive=" + isActive +
                '}';
    }
}
