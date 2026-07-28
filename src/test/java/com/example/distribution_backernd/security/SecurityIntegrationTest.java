package com.example.distribution_backernd.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldRejectUnauthenticatedAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/manager/locations/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessToProtectedEndpointWithValidToken() throws Exception {
        String token = jwtUtil.generateToken("boss");

        mockMvc.perform(get("/api/manager/locations/hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowDriverAccessToDriverEndpoint() throws Exception {
        String token = jwtUtil.generateToken("driver1");

        mockMvc.perform(get("/api/driver/locations/hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyDriverAccessToManagerEndpoint() throws Exception {
        String token = jwtUtil.generateToken("driver1");

        mockMvc.perform(get("/api/manager/locations/hello")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyManagerAccessToDriverEndpoint() throws Exception {
        String token = jwtUtil.generateToken("boss");

        mockMvc.perform(get("/api/driver/deliveries")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}