package se.andaluscalendar.productservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FakeStoreTlsPolicyTest {

    @Test
    @DisplayName("secure mode with prod profile is allowed")
    void whenSecureAndProd_thenNoException() {
        assertDoesNotThrow(() -> FakeStoreTlsPolicy.assertNotInsecureInProduction(
                false, List.of("prod")));
    }

    @Test
    @DisplayName("insecure mode without prod profile is allowed (dev-only escape hatch)")
    void whenInsecureAndDev_thenNoException() {
        assertDoesNotThrow(() -> FakeStoreTlsPolicy.assertNotInsecureInProduction(
                true, List.of("dev", "local")));
    }

    @Test
    @DisplayName("insecure mode with prod profile is forbidden")
    void whenInsecureAndProd_thenThrows() {
        assertThrows(
                IllegalStateException.class,
                () -> FakeStoreTlsPolicy.assertNotInsecureInProduction(true, List.of("prod")));
    }

    @Test
    @DisplayName("insecure mode with mixed profiles including prod is forbidden")
    void whenInsecureAndMixedProfiles_thenThrows() {
        assertThrows(
                IllegalStateException.class,
                () -> FakeStoreTlsPolicy.assertNotInsecureInProduction(
                        true, List.of("dev", "prod")));
    }
}
