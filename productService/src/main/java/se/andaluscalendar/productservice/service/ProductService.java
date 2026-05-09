package se.andaluscalendar.productservice.service;

import org.springframework.stereotype.Service;
import se.andaluscalendar.productservice.client.FakeStoreClient;
import se.andaluscalendar.productservice.dto.ProductResponse;

import java.util.List;

@Service
public class ProductService {
    private final FakeStoreClient fakeStoreClient;

    public ProductService(FakeStoreClient fakeStoreClient) {
        this.fakeStoreClient = fakeStoreClient;
    }

    public List<ProductResponse> getAllProducts() {
        return fakeStoreClient.getAllProducts().stream()
                .map(product -> new ProductResponse(
                        product.id(),
                        product.title(),
                        product.price(),
                        product.description(),
                        product.image()
                ))
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        var product = fakeStoreClient.getProductById(id);
        return new ProductResponse(
                product.id(),
                product.title(),
                product.price(),
                product.description(),
                product.image()
        );
    }
}
