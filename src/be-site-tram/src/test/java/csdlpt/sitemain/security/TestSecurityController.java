package csdlpt.sitemain.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSecurityController {

    @GetMapping("/api/categories/test-public")
    public String publicEndpoint() {
        return "public";
    }

    @GetMapping("/api/protected")
    public String protectedEndpoint() {
        return "protected";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin-only")
    public String adminOnly() {
        return "admin";
    }
}
