package se.andaluscalendar.productservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FakeStoreTlsProductionGuardSpringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FakeStoreTlsProductionGuard.class);

    @Test
    void whenProdAndInsecure_contextFailsToStart() {
        contextRunner
                .withPropertyValues(
                        "fakestore.ssl.insecure=true",
                        "spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void whenProdAndSecure_contextStarts() {
        contextRunner
                .withPropertyValues(
                        "fakestore.ssl.insecure=false",
                        "spring.profiles.active=prod")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void whenDevAndInsecure_contextStarts() {
        contextRunner
                .withPropertyValues(
                        "fakestore.ssl.insecure=true",
                        "spring.profiles.active=dev")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
