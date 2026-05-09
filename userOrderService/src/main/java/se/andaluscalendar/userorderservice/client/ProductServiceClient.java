package se.andaluscalendar.userorderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class ProductServiceClient {
    private final RestClient restClient;
    private final String productServiceBaseUrl;

    public ProductServiceClient(RestClient.Builder restClientBuilder,
                                @Value("${product-service.base-url}") String productServiceBaseUrl) {
        this.restClient = restClientBuilder.build();
        this.productServiceBaseUrl = productServiceBaseUrl;
    }

    public BigDecimal fetchProductPrice(Long productId) {
        ProductLookupResponse product;
        try {
            product = restClient.get()
                    .uri(productServiceBaseUrl + "/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductLookupResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Product with id " + productId + " was not found");
        } catch (HttpServerErrorException | ResourceAccessException e) {
            throw new IllegalStateException("Product service is unavailable");
        }

        if (product == null) {
            throw new IllegalStateException("Product service returned empty response");
        }

        if (product.price() == null) {
            throw new IllegalStateException("Product service returned invalid price");
        }
        return product.price();
    }

    private record ProductLookupResponse(
            Long id,
            BigDecimal price
    ) {
    }
}
