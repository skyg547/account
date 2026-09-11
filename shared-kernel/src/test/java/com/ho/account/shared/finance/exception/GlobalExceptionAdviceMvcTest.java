package com.ho.account.shared.finance.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.shared.finance.dto.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

class GlobalExceptionAdviceMvcTest {

    private static final String INPUT_MARKER = "synthetic-private-input";
    private static final String CAUSE_MARKER = "synthetic-private-cause";
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private FixtureUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        useCase = mock(FixtureUseCase.class);
        // A serializable timestamp prevents an advice write failure from masquerading as a fallback 400.
        mockMvc = MockMvcBuilders.standaloneSetup(new FixtureController(useCase))
                .setControllerAdvice(new FixtureAdvice())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                // A fixture validator exercises MVC validation without adding a provider dependency.
                .setValidator(new Validator() {
                    @Override
                    public boolean supports(Class<?> type) {
                        return type == FixtureRequest.class;
                    }

                    @Override
                    public void validate(Object target, Errors errors) {
                        if (((FixtureRequest) target).name().isBlank()) {
                            errors.rejectValue("name", "required", "Name is required");
                        }
                    }
                })
                .build();
    }

    @Test
    void missingRequiredQueryIsBadRequestBeforeTheUseCase() throws Exception {
        MvcResult result = mockMvc.perform(get("/request-errors/search")).andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(MissingServletRequestParameterException.class);
        verifyNoInteractions(useCase);
        assertFailure(result, 400, "Bad request");
    }

    @Test
    void malformedJsonIsBadRequestWithoutEchoingThePayload() throws Exception {
        MvcResult result = mockMvc.perform(post("/request-errors/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + INPUT_MARKER + "\"")).andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(HttpMessageNotReadableException.class);
        verifyNoInteractions(useCase);
        assertFailure(result, 400, "Bad request");
    }

    @Test
    void unsupportedMethodPreservesAllowWithoutCallingTheUseCase() throws Exception {
        MvcResult result = mockMvc.perform(put("/request-errors/search")
                .param("name", INPUT_MARKER)).andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(HttpRequestMethodNotSupportedException.class);
        verifyNoInteractions(useCase);
        assertFailure(result, 405, "Method not allowed");
        HttpRequestMethodNotSupportedException exception =
                (HttpRequestMethodNotSupportedException) result.getResolvedException();
        assertThat(result.getResponse().getHeaders(HttpHeaders.ALLOW))
                .containsExactlyElementsOf(exception.getHeaders().get(HttpHeaders.ALLOW));
        assertThat(result.getResponse().getHeader(HttpHeaders.ALLOW)).contains("GET");
    }

    @Test
    void unsupportedPostMediaTypePreservesAcceptWithoutCallingTheUseCase() throws Exception {
        MvcResult result = mockMvc.perform(post("/request-errors/body")
                .contentType(MediaType.TEXT_PLAIN).content(INPUT_MARKER)).andReturn();

        assertUnsupportedMediaType(result);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCEPT_PATCH)).isNull();
    }

    @Test
    void unsupportedPatchMediaTypeAlsoPreservesAcceptPatch() throws Exception {
        MvcResult result = mockMvc.perform(patch("/request-errors/body")
                .contentType(MediaType.TEXT_PLAIN).content(INPUT_MARKER)).andReturn();

        assertUnsupportedMediaType(result);
        HttpMediaTypeNotSupportedException exception =
                (HttpMediaTypeNotSupportedException) result.getResolvedException();
        assertThat(result.getResponse().getHeaders(HttpHeaders.ACCEPT_PATCH))
                .containsExactlyElementsOf(exception.getHeaders().get(HttpHeaders.ACCEPT_PATCH));
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCEPT_PATCH))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void validationRetainsItsExisting400Message() throws Exception {
        MvcResult result = mockMvc.perform(post("/request-errors/body")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}")).andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);
        verifyNoInteractions(useCase);
        assertFailure(result, 400, "name: Name is required");
    }

    @Test
    void knownIllegalArgumentRetainsItsExisting400Message() throws Exception {
        when(useCase.execute("invalid")).thenThrow(new IllegalArgumentException("Invalid amount"));

        MvcResult result = mockMvc.perform(get("/request-errors/search").param("name", "invalid")).andReturn();

        assertFailure(result, 400, "Invalid amount");
        verify(useCase).execute("invalid");
    }

    @Test
    void knownMissingResourceRetainsItsExisting404Message() throws Exception {
        when(useCase.execute("missing")).thenThrow(new ResourceNotFoundException("Fixture", 7));

        MvcResult result = mockMvc.perform(get("/request-errors/search").param("name", "missing")).andReturn();

        assertFailure(result, 404, "Fixture not found: 7");
        verify(useCase).execute("missing");
    }

    @Test
    void subclassSpecific409TakesPrecedenceOverInheritedCatchAll500() throws Exception {
        when(useCase.execute("conflict")).thenThrow(new FixtureConflictException());

        MvcResult result = mockMvc.perform(get("/request-errors/search").param("name", "conflict")).andReturn();

        assertFailure(result, 409, "Fixture conflict");
        verify(useCase).execute("conflict");
    }

    @Test
    void unexpectedFailureKeepsItsGeneric500WithoutMessageOrCause() throws Exception {
        when(useCase.execute("unexpected"))
                .thenThrow(new IllegalStateException(INPUT_MARKER, new RuntimeException(CAUSE_MARKER)));

        MvcResult result = mockMvc.perform(get("/request-errors/search")
                .param("name", "unexpected")).andReturn();

        assertFailure(result, 500, "Internal server error");
        verify(useCase).execute("unexpected");
    }

    @Test
    void validJsonReachesTheUseCaseAndSerializesTheSuccessEnvelope() throws Exception {
        when(useCase.execute("valid")).thenReturn("accepted");

        MvcResult result = mockMvc.perform(post("/request-errors/body")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"valid\"}")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResolvedException()).isNull();
        JsonNode response = assertEnvelope(result);
        assertThat(response.get("success").booleanValue()).isTrue();
        assertThat(response.get("message").textValue()).isEqualTo("SUCCESS");
        assertThat(response.get("data").textValue()).isEqualTo("accepted");
        verify(useCase).execute("valid");
    }

    private void assertUnsupportedMediaType(MvcResult result) throws Exception {
        assertThat(result.getResolvedException()).isInstanceOf(HttpMediaTypeNotSupportedException.class);
        verifyNoInteractions(useCase);
        assertFailure(result, 415, "Unsupported media type");
        HttpMediaTypeNotSupportedException exception =
                (HttpMediaTypeNotSupportedException) result.getResolvedException();
        assertThat(result.getResponse().getHeaders(HttpHeaders.ACCEPT))
                .containsExactlyElementsOf(exception.getHeaders().get(HttpHeaders.ACCEPT));
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCEPT))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    private void assertFailure(MvcResult result, int status, String message) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        JsonNode response = assertEnvelope(result);
        assertThat(response.get("success").booleanValue()).isFalse();
        assertThat(response.get("message").textValue()).isEqualTo(message);
        assertThat(response.get("data").isNull()).isTrue();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(INPUT_MARKER, CAUSE_MARKER);
    }

    private JsonNode assertEnvelope(MvcResult result) throws Exception {
        assertThat(MediaType.parseMediaType(result.getResponse().getContentType())
                .isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> fields = new ArrayList<>();
        response.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("success", "data", "message", "timestamp");
        assertThat(response.get("success").isBoolean()).isTrue();
        assertThat(response.get("message").isTextual()).isTrue();
        assertThat(response.get("timestamp").isTextual()).isTrue();
        assertThat(LocalDateTime.parse(response.get("timestamp").textValue())).isNotNull();
        return response;
    }

    interface FixtureUseCase {
        String execute(String name);
    }

    record FixtureRequest(String name) {
    }

    @RestController
    static class FixtureController {
        private final FixtureUseCase useCase;

        FixtureController(FixtureUseCase useCase) {
            this.useCase = useCase;
        }

        @GetMapping("/request-errors/search")
        ApiResponse<String> search(@RequestParam("name") String name) {
            return ApiResponse.success(useCase.execute(name));
        }

        @RequestMapping(value = "/request-errors/body", method = {RequestMethod.POST, RequestMethod.PATCH},
                consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<String> body(@Valid @RequestBody FixtureRequest request) {
            return ApiResponse.success(useCase.execute(request.name()));
        }
    }

    static class FixtureConflictException extends IllegalStateException {
    }

    @RestControllerAdvice
    static class FixtureAdvice extends GlobalExceptionAdvice {
        @ExceptionHandler(FixtureConflictException.class)
        ResponseEntity<ApiResponse<Void>> handleConflict(FixtureConflictException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure("Fixture conflict"));
        }
    }
}
