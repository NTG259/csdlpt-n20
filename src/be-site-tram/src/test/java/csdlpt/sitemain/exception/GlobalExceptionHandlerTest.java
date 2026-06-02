package csdlpt.sitemain.exception;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

class GlobalExceptionHandlerTest {

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnValidationErrorForInvalidRequestBody() throws Exception {
        String requestBody = """
                {
                  "hoTen": "Nguyen Van A",
                  "email": "invalid-email",
                  "soDienThoai": "0123456789",
                  "matKhau": "123",
                  "maKhuVuc": "KV01",
                  "diaChi": "123 Demo Street",
                  "ngaySinh": "2000-01-01",
                  "gioiTinh": "Nam",
                  "cccd": "123456789012"
                }
                """;

        mockMvc.perform(post("/test/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasItem("email: Email không đúng định dạng")))
                .andExpect(jsonPath("$.details", hasItem("matKhau: Mật khẩu phải từ 6 đến 72 ký tự")));
    }

    @Test
    void shouldReturnValidationErrorForConstraintViolation() {
        Set<ConstraintViolation<QueryParamRequest>> violations = validator.validate(new QueryParamRequest(0));
        ResponseEntity<?> response = new GlobalExceptionHandler()
                .handleConstraintViolationException(new ConstraintViolationException(violations));

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof csdlpt.sitemain.common.ErrorResponse);

        csdlpt.sitemain.common.ErrorResponse body =
                (csdlpt.sitemain.common.ErrorResponse) response.getBody();

        assertFalse(body.success());
        assertEquals("VALIDATION_ERROR", body.errorCode());
        assertTrue(body.details().contains("page: must be greater than or equal to 1"));
    }

    @Test
    void shouldReturnBusinessErrorWhenBusinessExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("Email đã tồn tại trong hệ thống"));
    }

    @Test
    void shouldReturnDuplicatePhoneWhenDataIntegrityViolationContainsPhone() throws Exception {
        mockMvc.perform(get("/test/data-integrity-phone"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_PHONE"))
                .andExpect(jsonPath("$.message").value("Số điện thoại đã tồn tại trong hệ thống"));
    }

    @Test
    void shouldReturnInternalErrorForUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Đã xảy ra lỗi nội bộ. Vui lòng thử lại sau."));
    }

    @RestController
    @Validated
    static class TestController {

        @PostMapping("/test/register")
        ResponseEntity<?> register(@Valid @RequestBody RegisterTestRequest req) {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/test/business")
        String business() {
            throw new DuplicateEmailException();
        }

        @GetMapping("/test/data-integrity-phone")
        String dataIntegrityPhone() {
            throw new DataIntegrityViolationException("Violation of UNIQUE KEY constraint on so_dien_thoai");
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("boom");
        }
    }

    record RegisterTestRequest(
            @Email(message = "Email không đúng định dạng") String email,
            @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự") String matKhau
    ) {}

    record QueryParamRequest(@Min(1) int page) {
    }
}
