package se.andaluscalendar.productservice.config;

/**
 * Ensures FakeStore HTTPS is not run with certificate verification disabled in production.
 */
public final class FakeStoreTlsPolicy {

    private FakeStoreTlsPolicy() {
    }

    /**
     * @param fakeStoreSslInsecure when true, TLS certificate verification to FakeStore is disabled
     * @param activeProfiles       Spring active profiles (e.g. {@code prod})
     * @throws IllegalStateException if insecure mode is on while {@code prod} is active
     */
    public static void assertNotInsecureInProduction(
            boolean fakeStoreSslInsecure,
            Iterable<String> activeProfiles) {
        if (!fakeStoreSslInsecure) {
            return;
        }
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                throw new IllegalStateException(
                        "fakestore.ssl.insecure must not be enabled when the 'prod' profile is active. "
                                + "Use a current JDK 21 distribution (e.g. Eclipse Temurin, Amazon Corretto) so the "
                                + "default trust store validates https://fakestoreapi.com. "
                                + "Do not disable TLS certificate verification in production.");
            }
        }
    }
}
