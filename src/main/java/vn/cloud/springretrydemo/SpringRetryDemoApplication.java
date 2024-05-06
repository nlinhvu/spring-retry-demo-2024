package vn.cloud.springretrydemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import vn.cloud.springretrydemo.hello.HelloServiceClient;

@SpringBootApplication
@EnableRetry
public class SpringRetryDemoApplication {

	private static final Logger log = LoggerFactory.getLogger(SpringRetryDemoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringRetryDemoApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(HelloServiceClient helloServiceClient) {
		return args -> {
			String hello = helloServiceClient.hello("name", 99);
			log.info("Result: %s".formatted(hello));
		};
	}
}
