package com.ho.account.masterdata.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.masterdata.core.application.command.ProductCommand;
import com.ho.account.masterdata.core.application.port.in.ProductUseCase;
import com.ho.account.masterdata.core.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductPayloadValidationControllerTest {

    private ProductUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(ProductUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(useCase))
                .setControllerAdvice(new MasterDataExceptionHandler())
                .build();
    }

    @Test
    void createNameBoundaryUsesDomainRulesAndReturnsSanitizedError() throws Exception {
        when(useCase.createProduct(any(ProductCommand.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0, ProductCommand.class).toEntity();
            product.validateBusinessFields();
            return product;
        });

        createWithName("P".repeat(200))
                .andExpect(status().isOk());
        createWithName("P".repeat(201))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product name must be at most 200 characters."));
        createWithName("   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product name is required."));
    }

    @Test
    void updateMayOmitNameButPassesExplicitBlankToDomain() throws Exception {
        when(useCase.updateProduct(eq(42L), any(ProductCommand.class))).thenReturn(new Product());

        mockMvc.perform(put("/api/basic/products/{id}", 42L)
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/basic/products/{id}", 42L)
                        .header("X-Auth-Roles", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductCommand> captured = ArgumentCaptor.forClass(ProductCommand.class);
        verify(useCase, org.mockito.Mockito.times(2)).updateProduct(eq(42L), captured.capture());
        assertThat(captured.getAllValues().get(0).name()).isNull();
        assertThat(captured.getAllValues().get(1).name()).isEqualTo("   ");
    }

    private org.springframework.test.web.servlet.ResultActions createWithName(String name) throws Exception {
        String body = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "productCode", "P-913", "name", name, "productType", "SERVICE", "price", 0));
        return mockMvc.perform(post("/api/basic/products")
                .header("X-Auth-Roles", "ROLE_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
