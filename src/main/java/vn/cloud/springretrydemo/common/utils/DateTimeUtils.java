package vn.cloud.springretrydemo.common.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateTimeUtils {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public static String zonedDateTime2String(ZonedDateTime time) {
        return Optional.ofNullable(time)
                .map(DATE_TIME_FORMATTER::format)
                .orElse(null);
    }
}
