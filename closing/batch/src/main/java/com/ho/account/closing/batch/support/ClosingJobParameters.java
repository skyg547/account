package com.ho.account.closing.batch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Technical validation for stable, identifying Closing job parameters. */
public final class ClosingJobParameters {

    private ClosingJobParameters() {
    }

    public static JobParametersValidator requiredDateAndPositiveId(String dateName, String idName) {
        return parameters -> {
            requireDate(parameters, dateName);
            requirePositiveLong(parameters, idName);
        };
    }

    public static LocalDate requireDate(JobParameters parameters, String name)
            throws JobParametersInvalidException {
        return requireDate(parameters == null ? null : parameters.getString(name), name);
    }

    public static LocalDate requireDate(String value, String name)
            throws JobParametersInvalidException {
        if (value == null || value.isBlank()) {
            throw new JobParametersInvalidException(name + " is required");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new JobParametersInvalidException(name + " must use ISO date format yyyy-MM-dd");
        }
    }

    public static Long requirePositiveLong(JobParameters parameters, String name)
            throws JobParametersInvalidException {
        return requirePositiveLong(parameters == null ? null : parameters.getLong(name), name);
    }

    public static Long requirePositiveLong(Long value, String name)
            throws JobParametersInvalidException {
        if (value == null || value <= 0) {
            throw new JobParametersInvalidException(name + " must be a positive long");
        }
        return value;
    }
}
