package se.andaluscalendar.productservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.andaluscalendar.productservice.exception.ProductNotFoundException;
import se.andaluscalendar.productservice.exception.UpstreamServiceException;

import java.util.List;

@Component
public class FakeStoreClient {
    private final WebClient webClient;
    private final String fakeStoreBaseUrl;

    public FakeStoreClient(WebClient fakeStoreWebClient, @Value("${fakestore.base-url}") String fakeStoreBaseUrl) {
        this.webClient = fakeStoreWebClient;
        this.fakeStoreBaseUrl = fakeStoreBaseUrl;
    }

    public List<FakeStoreProductDto> getAllProducts() {
        return webClient.get()
                .uri(fakeStoreBaseUrl + "/products")
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapUpstreamError)
                .bodyToFlux(FakeStoreProductDto.class)
                .collectList()
                .blockOptional()
                .orElse(List.of());
    }

    public FakeStoreProductDto getProductById(Long productId) {
        return webClient.get()
                .uri(fakeStoreBaseUrl + "/products/{id}", productId)
                .retrieve()
                .onStatus(status -> status.value() == 404, response ->
                        Mono.error(new ProductNotFoundException("Product with id " + productId + " was not found")))
                .onStatus(HttpStatusCode::isError, this::mapUpstreamError)
                .bodyToMono(FakeStoreProductDto.class)
                .blockOptional()
                .orElseThrow(() -> new UpstreamServiceException("Empty response from FakeStore API"));
    }

    private Mono<? extends Throwable> mapUpstreamError(org.springframework.web.reactive.function.client.ClientResponse response) {
        return Mono.error(new UpstreamServiceException(
                "FakeStore API returned status " + response.statusCode().value()
        ));
    }
}
