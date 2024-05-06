package vn.cloud.springretrydemo.configuration.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 100, multiplier = 2.0))
public @interface InternalRetryable {

    @AliasFor(annotation = Retryable.class, attribute = "recover")
    String recover() default "";

    @AliasFor(annotation = Retryable.class, attribute = "noRetryFor")
    Class<? extends Throwable>[] noRetryFor() default {};
}
