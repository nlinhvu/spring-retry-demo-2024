package vn.cloud.springretrydemo.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import vn.cloud.springretrydemo.common.utils.DateTimeUtils;
import vn.cloud.springretrydemo.configuration.annotation.InternalRetryable;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class HelloServiceClient {
    private static final Logger log = LoggerFactory.getLogger(HelloServiceClient.class);

    @InternalRetryable(recover = "recover2", noRetryFor = {IllegalArgumentException.class})
    public String hello(String name, int age) {
        log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
        throw new IllegalArgumentException("Invalid arg");
    }

    @Recover
    public int recover1() {
        log.info("Fallback triggerred in recover1!!!");
        return 1;
    }

    @Recover
    public String recover2(RuntimeException exception, String name, int age) {
        log.info("Fallback triggerred in recover2!!!");
        log.info("Name: %s, Age: %d".formatted(name, age));
        log.info("Exception: %s".formatted(exception.getClass().getCanonicalName()));
        return "Hello World backup!";
    }
}
