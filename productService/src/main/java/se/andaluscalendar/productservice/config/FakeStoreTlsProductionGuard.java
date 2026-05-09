package se.andaluscalendar.productservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class FakeStoreTlsProductionGuard {

    private final boolean fakeStoreSslInsecure;
    private final Environment environment;

    public FakeStoreTlsProductionGuard(
            @Value("${fakestore.ssl.insecure:false}") boolean fakeStoreSslInsecure,
            Environment environment) {
        this.fakeStoreSslInsecure = fakeStoreSslInsecure;
        this.environment = environment;
    }

    @PostConstruct
    void enforceProductionTlsPolicy() {
        FakeStoreTlsPolicy.assertNotInsecureInProduction(
                fakeStoreSslInsecure,
                Arrays.asList(environment.getActiveProfiles()));
    }
}
