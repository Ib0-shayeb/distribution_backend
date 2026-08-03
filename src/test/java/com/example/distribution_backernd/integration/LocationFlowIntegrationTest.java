//package com.example.distribution_backernd.integration;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Testcontainers
//@TestPropertySource(properties = {
//        "spring.jpa.hibernate.ddl-auto=create-drop"
//})
//class LocationFlowIntegrationTest {
//    @Container
//    @ServiceConnection
//    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    void completeLocationTrackingFlow() throws Exception {
//        Integer driverId = 55;
//
//        String locationPayload = """
//            {
//                "userId": 55,
//                "latitude": 52.2297,
//                "longitude": 21.0122
//            }
//            """;
//
//        mockMvc.perform(post("/api/driver/locations/log")
//                        .with(user("driverUser").roles("DRIVER"))
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(locationPayload))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(get("/api/manager/locations/active")
//                        .with(user("managerUser").roles("MANAGER")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isArray())
//
//                .andExpect(jsonPath("$[?(@ == 55)]").exists());
//
//        String start = ZonedDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME);
//        String end = ZonedDateTime.now().plusMinutes(5).format(DateTimeFormatter.ISO_DATE_TIME);
//
//        mockMvc.perform(get("/api/manager/locations/history")
//                        .with(user("managerUser").roles("MANAGER"))
//                        .param("userId", driverId.toString())
//                        .param("start", start)
//                        .param("end", end))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].userId").value(55))
//                .andExpect(jsonPath("$[0].latitude").value(52.2297))
//                .andExpect(jsonPath("$[0].longitude").value(21.0122));
//    }
//}