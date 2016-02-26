package io.pillopl.value;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * The calendar month an ERP billing run bills, written "2026_07" (the form of the monthly SQL and of the ERP files).
 */
public record BillingPeriod(YearMonth month) {

    public BillingPeriod {
        if (month == null) {
            throw new InvalidValueException("BillingPeriod", "is missing");
        }
    }

    public static BillingPeriod of(int month, int year) {
        if (month < 1 || month > 12) {
            throw new InvalidValueException("BillingPeriod", "month must be 1..12 but is " + month);
        }
        return new BillingPeriod(YearMonth.of(year, month));
    }

    /**
     * @param code "yyyy_MM"
     */
    public static BillingPeriod parse(String code) {
        if (code == null || !code.trim().matches("\\d{4}_\\d{2}")) {
            throw new InvalidValueException("BillingPeriod", "is not yyyy_MM: " + code);
        }
        try {
            return new BillingPeriod(YearMonth.parse(code.trim().replace('_', '-')));
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("BillingPeriod", "is not yyyy_MM: " + code);
        }
    }

    /**
     * The month before the day: the period the monthly run bills on the day.
     */
    public static BillingPeriod before(LocalDate day) {
        return new BillingPeriod(YearMonth.from(day).minusMonths(1));
    }

    public LocalDate getFirstDay() {
        return month.atDay(1);
    }

    public LocalDate getLastDay() {
        return month.atEndOfMonth();
    }

    public int getDays() {
        return month.lengthOfMonth();
    }

    public String getCode() {
        return String.format("%04d_%02d", month.getYear(), month.getMonthValue());
    }

    @Override
    public String toString() {
        return getCode();
    }
}
