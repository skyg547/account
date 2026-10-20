package com.ho.account.masterdata.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.model.Product;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductPriceHttpValidationTest {

    private ProductUseCase products;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        products = mock(ProductUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(products))
                .setControllerAdvice(new MasterDataDirectWritePolicy(), new MasterDataExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper().registerModule(new JavaTimeModule())))
                .build();
    }

    @ParameterizedTest(name = "{0}: {1} is rejected before the use case")
    @MethodSource("invalidPrices")
    void rejectsInvalidPriceBeforeUseCase(String path, String price) throws Exception {
        mockMvc.perform(request(path).content(body(price)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(products);
    }

    @ParameterizedTest(name = "{0}: {1} is accepted")
    @MethodSource("validPrices")
    void acceptsExactlyRepresentablePrice(String path, String price) throws Exception {
        when(products.createProduct(any(ProductCommand.class))).thenAnswer(invocation ->
                productFrom(invocation.getArgument(0)));
        when(products.updateProduct(any(Long.class), any(ProductCommand.class))).thenAnswer(invocation ->
                productFrom(invocation.getArgument(1)));

        mockMvc.perform(request(path).content(body(price)))
                .andExpect(status().isOk());
    }

    private static Stream<Arguments> invalidPrices() {
        return Stream.of("-0.0001", "1000000000000000", "1.23456")
                .flatMap(price -> Stream.of("POST", "PUT").map(path -> Arguments.of(path, price)));
    }

    private static Stream<Arguments> validPrices() {
        return Stream.of("0", "999999999999999.9999", "1.2345000")
                .flatMap(price -> Stream.of("POST", "PUT").map(path -> Arguments.of(path, price)));
    }

    private static MockHttpServletRequestBuilder request(String path) {
        return ("POST".equals(path) ? post("/api/basic/products") : put("/api/basic/products/1"))
                .header("X-Auth-Roles", "ROLE_ADMIN")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static String body(String price) {
        return """
                {"productCode":"P-912","name":"Test Product","price":%s,
                 "productType":"SERVICE","validFrom":"2026-11-01","validTo":"2026-11-30"}
                """.formatted(price);
    }

    private static Product productFrom(ProductCommand command) {
        Product product = new Product();
        product.setProductCode(command.productCode());
        product.setName(command.name());
        product.setPrice(command.price());
        product.setProductType(command.productType());
        product.setValidFrom(command.validFrom());
        product.setValidTo(command.validTo());
        return product;
    }
}
