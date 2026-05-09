package se.andaluscalendar.productservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.andaluscalendar.productservice.client.FakeStoreClient;
import se.andaluscalendar.productservice.client.FakeStoreProductDto;
import se.andaluscalendar.productservice.dto.ProductResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private FakeStoreClient fakeStoreClient;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Test/ getAllProducts maps external DTO list to response list")
    void whenGetAllProducts_thenMapFieldsToProductResponse() {
        FakeStoreProductDto first = new FakeStoreProductDto(
                1L, "Backpack", new BigDecimal("39.99"), "Travel backpack", "img-1"
        );
        FakeStoreProductDto second = new FakeStoreProductDto(
                2L, "Headphones", new BigDecimal("59.50"), "Noise cancelling", "img-2"
        );
        when(fakeStoreClient.getAllProducts()).thenReturn(List.of(first, second));

        List<ProductResponse> responses = productService.getAllProducts();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.getFirst().id());
        assertEquals("Backpack", responses.getFirst().title());
        assertEquals(new BigDecimal("39.99"), responses.getFirst().price());
        assertEquals("Travel backpack", responses.getFirst().description());
        assertEquals("img-1", responses.getFirst().imageUrl());
    }

    @Test
    @DisplayName("Test/ getProductById maps external DTO to response DTO")
    void whenGetProductById_thenMapFieldsToProductResponse() {
        FakeStoreProductDto dto = new FakeStoreProductDto(
                10L, "Keyboard", new BigDecimal("89.00"), "Mechanical keyboard", "img-10"
        );
        when(fakeStoreClient.getProductById(10L)).thenReturn(dto);

        ProductResponse response = productService.getProductById(10L);

        assertEquals(10L, response.id());
        assertEquals("Keyboard", response.title());
        assertEquals(new BigDecimal("89.00"), response.price());
        assertEquals("Mechanical keyboard", response.description());
        assertEquals("img-10", response.imageUrl());
    }
}
