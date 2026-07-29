package com.example.distribution_backernd.controller;

import com.example.distribution_backernd.configuration.ProjectConfig;
import com.example.distribution_backernd.model.LocationLog;
import com.example.distribution_backernd.repository.LocationLogRepository;
import com.example.distribution_backernd.security.JwtUtil;
import com.example.distribution_backernd.service.LocationStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverLocationController.class)
@Import(ProjectConfig.class)
class DriverLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationLogRepository logRepo;

    @MockitoBean
    private LocationStreamService streamService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // 👇 ADD THIS: Mock the DataSource so ProjectConfig can satisfy its dependency
    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Test
    @WithMockUser(roles = "DRIVER")
    void shouldLogLocationAndBroadcastStream() throws Exception {
        String requestBody = """
                {
                    "userId": 1,
                    "latitude": 52.2297,
                    "longitude": 21.0122
                }
                """;

        LocationLog savedLog = new LocationLog();
        savedLog.setId(100);
        savedLog.setUserId(1);

        when(logRepo.save(any(LocationLog.class))).thenReturn(savedLog);

        mockMvc.perform(post("/api/driver/locations/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        verify(logRepo).save(any(LocationLog.class));
        verify(streamService).broadcastLocation(any(LocationLog.class));
    }
}