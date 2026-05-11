package se.andaluscalendar.userorderservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProductServiceClientTest {

    private static final String AUTH_HEADER = "Bearer test-token";

    private MockRestServiceServer mockServer;
    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        productServiceClient = new ProductServiceClient(builder, "http://localhost:8082");
    }

    @Test
    @DisplayName("Test/ Fetch product price returns expected value")
    void whenProductExists_thenReturnPrice() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTH_HEADER))
                .andRespond(withSuccess("""
                        {"id":1,"price":19.99}
                        """, MediaType.APPLICATION_JSON));

        BigDecimal price = productServiceClient.fetchProductPrice(1L, AUTH_HEADER);

        assertEquals(new BigDecimal("19.99"), price);
        mockServer.verify();
    }

    @Test
    @DisplayName("Test/ 404 from product service maps to IllegalArgumentException")
    void whenProductNotFound_thenThrowIllegalArgumentException() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/99"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTH_HEADER))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                productServiceClient.fetchProductPrice(99L, AUTH_HEADER)
        );

        assertEquals("Product with id 99 was not found", ex.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("Test/ 5xx from product service maps to IllegalStateException")
    void whenProductServiceFails_thenThrowIllegalStateException() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTH_HEADER))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                productServiceClient.fetchProductPrice(1L, AUTH_HEADER)
        );

        assertEquals("Product service is unavailable", ex.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("Test/ Missing product price maps to IllegalStateException")
    void whenProductPriceIsMissing_thenThrowIllegalStateException() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", AUTH_HEADER))
                .andRespond(withSuccess("""
                        {"id":1}
                        """, MediaType.APPLICATION_JSON));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                productServiceClient.fetchProductPrice(1L, AUTH_HEADER)
        );

        assertEquals("Product service returned invalid price", ex.getMessage());
        mockServer.verify();
    }
}
