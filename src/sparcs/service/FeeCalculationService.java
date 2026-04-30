package service;

import dao.FeeScheduleDAO;
import model.FeeSchedule;
import model.ParkingTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Fee Calculation Service
 * Handles parking fee calculations
 */
public class FeeCalculationService {
    private FeeScheduleDAO feeScheduleDAO;

    public FeeCalculationService() {
        this.feeScheduleDAO = new FeeScheduleDAO();
    }

    /**
     * Calculate parking fee for a transaction
     */
    public BigDecimal calculateFee(ParkingTransaction transaction) throws SQLException {
        if (transaction.getEntryTime() == null) {
            return BigDecimal.ZERO;
        }

        LocalDateTime exitTime = transaction.getExitTime() != null ? 
            transaction.getExitTime() : LocalDateTime.now();

        long durationMinutes = ChronoUnit.MINUTES.between(transaction.getEntryTime(), exitTime);

        Optional<FeeSchedule> feeSchedule = feeScheduleDAO.findCurrentActive();
        if (!feeSchedule.isPresent()) {
            return BigDecimal.ZERO;
        }

        FeeSchedule schedule = feeSchedule.get();
        
        if (durationMinutes <= schedule.getGracePeriodMinutes()) {
            return BigDecimal.ZERO;
        }

        long billableMinutes = durationMinutes - schedule.getGracePeriodMinutes();
        long billableHours = (long) Math.ceil((double) billableMinutes / 60.0);

        BigDecimal fee = schedule.getRatePerHour().multiply(BigDecimal.valueOf(billableHours));

        if (fee.compareTo(schedule.getRatePerDay()) > 0) {
            fee = schedule.getRatePerDay();
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate duration in minutes between two times
     */
    public static long calculateDurationMinutes(LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null) {
            return 0;
        }
        LocalDateTime exit = exitTime != null ? exitTime : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(entryTime, exit);
    }

    /**
     * Get current fee schedule
     */
    public Optional<FeeSchedule> getCurrentFeeSchedule() throws SQLException {
        return feeScheduleDAO.findCurrentActive();
    }

    /**
     * Get fee per hour
     */
    public BigDecimal getFeePerHour() throws SQLException {
        Optional<FeeSchedule> schedule = feeScheduleDAO.findCurrentActive();
        if (schedule.isPresent()) {
            return schedule.get().getRatePerHour();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get grace period in minutes
     */
    public int getGracePeriodMinutes() throws SQLException {
        Optional<FeeSchedule> schedule = feeScheduleDAO.findCurrentActive();
        if (schedule.isPresent()) {
            return schedule.get().getGracePeriodMinutes();
        }
        return 0;
    }
}
