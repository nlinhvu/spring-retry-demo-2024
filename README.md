# Spring Retry & RestClient in Spring Boot 3 - A Declarative Style for Retry, Exponential Backoff, Fallback(Recovery)
> This repo is used in this Youtube video: https://youtu.be/pGN6tp3Hij8

> **Noted:** `Spring Retry` supports both [**Imperative Style**](https://github.com/spring-projects/spring-retry?tab=readme-ov-file#features-and-api) 
> and [**Declarative Style**](https://github.com/spring-projects/spring-retry?tab=readme-ov-file#declarative-retry),
> we will only demo **Declarative Style** in this repo.

> **Noted:** There are two types of Retry in `Spring Retry`: [**Stateless Retry**](https://github.com/spring-projects/spring-retry?tab=readme-ov-file#stateless-retry)
> and [**Stateful Retry**](https://github.com/spring-projects/spring-retry?tab=readme-ov-file#stateful-retry).
> 
> **Stateless Retry** when **no** transaction needed, usually in a simple request call with `RestClient`/`RestTemplate`. 
> This is usually used along with `@Retryable`.
> 
> **Stateful Retry** when transaction needed, usually in a database update with `Hibernate`.
> This is usually used along with `@CircuitBreaker`.
> 
> Since we're going to use Spring Retry & RestClient, we're going to stick with **Stateless Retry** 
> in this repo.


## 1. Prerequisite to enable `@Retryable`

### 1.1. Add Dependencies 
Not only spring-retry dependency itself, in order to use Spring Retry in a `Declarative` way, we need to add `AOP dependency` in `runtimeClasspath`:
```groovy
implementation 'org.springframework.retry:spring-retry'
runtimeOnly 'org.springframework.boot:spring-boot-starter-aop'
```

### 1.2. Add `@EnableRetry` in one of our `@Configuration` classes
```java
@SpringBootApplication
@EnableRetry
public class SpringRetryDemoApplication {
}
```
`@EnableRetry` will import `RetryConfiguration` for us that create a `AnnotationAwareRetryOperationsInterceptor` as our `AOP advice`. 
This interceptor works as an entry point for processing retries of our methods. 

### 1.3. (Optional) Print log in `application.properties` (or `application.yml`)
```properties
logging.level.org.springframework.retry=debug
```

## 2. Getting Started with `Spring Retry`

Let's create a `@Retryable` method in `hello.HelloServiceClient` class:
```java
@Retryable
public String hello() {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    return "Hello World!";
}
```

And call it from `ApplicationRunner` bean:
```java
@Bean
ApplicationRunner applicationRunner(HelloServiceClient helloServiceClient) {
    return args -> {
        String hello = helloServiceClient.hello();
        log.info("Result: %s".formatted(hello));
    };
}
```

> By default, `@Retryable` will attempt 3 times (1 first attempt + 2 retries) for all exceptions are thrown
> and fallback to the `@Recover` method that has the same return type in the same class.

Let's check that by updating our `hello method` throwing an exception:
```java
@Retryable
public String hello() {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

### 2.1. Recovery
Add a `@Recover` method, but the return type is `int`:
```java
@Recover
public int recover1() {
    log.info("Fallback triggerred in recover1!!!");
    return 1;
}
```

Add another `@Recover` method, but the return type is `String` same as `hello method`:
```java
@Recover
public String recover2() {
    log.info("Fallback triggerred in recover2!!!");
    return "Hello World backup!";
}
```

We can also include method arguments for `@Recover` methods, but the order must follow its `@Retryable` method:
```java
@Retryable
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```
```java
@Recover
public String recover2(String name, int age) {
    log.info("Fallback triggerred in recover2!!!");
    log.info("Name: %s, Age: %d".formatted(name, age));
    return "Hello World backup!";
}
```

If we want to refer to the exception of `@Retryable` method in `@Recover` method, we can add it as the first argument of `@Recover` method:
```java
@Recover
public String recover2(RuntimeException exception, String name, int age) {
    log.info("Fallback triggerred in recover2!!!");
    log.info("Name: %s, Age: %d".formatted(name, age));
    log.info("Exception: %s".formatted(exception.getClass().getCanonicalName()));
    return "Hello World backup!";
}
```

> **Good Practice:** always _explicitly_ declare a `@Recover` method for a `@Retryable` method.
```java
@Retryable(recover = "recover2")
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

### 2.2. Handle Exceptions
We can specify which exception we'd like to retry or not by `retryFor` or `noRetryFor` respectively.
For example, we don't want to retry for IllegalArgumentException since it's not a retryable exception:
```java
@Retryable(recover = "recover2", noRetryFor = {IllegalArgumentException.class})
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

We also have a `notRecoverable` to skip the `@Recover` method, in the example above, even though we disabled retry
but it still fell back to `"recover2" method`. Now it throws the exception instead:
```java
@Retryable(recover = "recover2", noRetryFor = {IllegalArgumentException.class}, notRecoverable = {IllegalArgumentException.class})
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

### 2.3. Retry Policy & Backoff Policy
We can set the number of retry by `maxAttempts`, it's always a **good practice** to explicitly set it (default is 3):
```java
@Retryable(maxAttempts = 5)
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

We can set a `canonical backoff` by using `backoff`. (by default the delay is 1000)
> Delay is a time period between the finished time of (n)th retry and the start time of (n+1)th retry
> 
> [---(n)th retry---][-------delay-------][---(n+1)th retry---]

```java
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 2000))
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

We can set a `random backoff` by using `delay` and `maxDelay` together
```java
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000, maxDelay = 3000))
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

We can set a `exponential backoff` by using `delay` and `multiplier` together
```java
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2.0))
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

> **Noted:** Set the backoff too short can overwhelm our server, but set it too long can increase latency, affecting user/client experience.
> Some clients have their own response timeout, they'll obviously give up waiting for a response and receive an error instead.

It depends on our use-case, so let's set it to 100 milliseconds for our demo
```java
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 100, multiplier = 2.0))
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

### 2.4. Custom Annotations
`@Retryable` annotation can be used in custom composed annotations to create our own annotations with predefined behaviour.
The simplest way is just create **an annotation** (`@interface`) and copy our **using** `@Retryable`.
Let's create an annotation `configuration.annotation.InternalRetryable`:
```java
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 100, multiplier = 2.0))
public @interface InternalRetryable {
}
```
Then apply it into our methods:
```java
@InternalRetryable
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

But with that simple setting we **cannot** use other fields of `@Retryable`, for example: `recover`, `retryFor`, `notRecoverable`,... above.
So we have to list down fields we want to use, for example, we'd like to use `recover` and `noRetryFor`
```java
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 100, multiplier = 2.0))
public @interface InternalRetryable {

    @AliasFor(annotation = Retryable.class, attribute = "recover")
    String recover() default "";

    @AliasFor(annotation = Retryable.class, attribute = "noRetryFor")
    Class<? extends Throwable>[] noRetryFor() default {};
}
```
Then we can use them in `@InternalRetryable`
```java
@InternalRetryable(recover = "recover2", noRetryFor = {IllegalArgumentException.class})
public String hello(String name, int age) {
    log.info("Hello at %s".formatted(DateTimeUtils.zonedDateTime2String(ZonedDateTime.now(ZoneId.of("UTC")))));
    throw new IllegalArgumentException("Invalid arg");
}
```

We can create as many custom retry annotations as we want for each purpose. For example, we want `@RestClientRetryable`
for our ServiceClient's methods that use `RestClient`:
```java
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1.2))
public @interface RestClientRetryable {
}
```


### 2.5. Other Features
> For other features like `Listeners`, `Expressions`, `RuntimeConfigs`,... you can refer to the references at the end of this README.

## References
- `1.` **Spring Retry** in [**Spring Retry Github Repository**](https://github.com/spring-projects/spring-retry).
