//package com.example.distribution_backernd.controller;
//
//import com.example.distribution_backernd.model.LocationLog;
//import com.example.distribution_backernd.model.User;
//import com.example.distribution_backernd.repository.LocationLogRepository;
//import com.example.distribution_backernd.repository.UserRepository;
//import com.example.distribution_backernd.security.JwtUtil;
//import com.example.distribution_backernd.service.LocationStreamService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.time.ZonedDateTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.http.MediaType;
//
//@WebMvcTest(ManagerLocationController.class)
//class ManagerLocationControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private LocationLogRepository logRepo;
//
//    @MockitoBean
//    private UserRepository userRepo;
//
//    @MockitoBean
//    private LocationStreamService streamService;
//
//    @MockitoBean
//    private JwtUtil jwtUtil;
//
//    @Test
//    @WithMockUser(roles = "MANAGER")
//    void shouldReturnHistoryRangeWithParsedDates() throws Exception {
//        when(logRepo.findHistoryRange(eq(1), any(ZonedDateTime.class), any(ZonedDateTime.class)))
//                .thenReturn(List.of(new LocationLog(), new LocationLog()));
//
//        mockMvc.perform(get("/api/manager/locations/history")
//                        .param("userId", "1")
//                        .param("start", "2026-07-29T10:00:00Z")
//                        .param("end", "2026-07-29T12:00:00Z"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2));
//    }
//
//    @Test
//    @WithMockUser(roles = "MANAGER")
//    void shouldReturnActiveDriversList() throws Exception {
//        when(logRepo.findActiveUserIds()).thenReturn(List.of(1, 5, 9));
//
//        mockMvc.perform(get("/api/manager/locations/active"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(3))
//                .andExpect(jsonPath("$[0]").value(1));
//    }
//
//    @Test
//    @WithMockUser(roles = "MANAGER")
//    void shouldReturnSseEmitterForStream() throws Exception {
//        SseEmitter emitter = new SseEmitter();
//        when(streamService.createStream(1)).thenReturn(emitter);
//
//        MvcResult mvcResult = mockMvc.perform(get("/api/manager/locations/stream")
//                        .param("userId", "1"))
//                .andExpect(status().isOk())
//                .andExpect(request().asyncStarted())
//                .andReturn();
//
//        emitter.complete();
//
//        mockMvc.perform(asyncDispatch(mvcResult))
//                .andExpect(status().isOk())
//                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
//    }
//
//    @Test
//    @WithMockUser(roles = "MANAGER")
//    void shouldRegisterWorkerSuccessfully() throws Exception {
//        String payload = """
//                {
//                    "name": "John Doe",
//                    "phoneNumber": "+123456789"
//                }
//                """;
//
//        mockMvc.perform(post("/api/manager/locations/register-worker")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(payload))
//                .andExpect(status().isOk())
//                .andExpect(content().string(org.hamcrest.Matchers.containsString("Driver registered successfully")));
//
//        verify(userRepo).save(any(User.class));
//    }
//
//    @Test
//    @WithMockUser(roles = "MANAGER")
//    void shouldRejectWorkerRegistrationWhenNameIsMissing() throws Exception {
//        String payload = """
//                {
//                    "name": "   ",
//                    "phoneNumber": "+123456789"
//                }
//                """;
//
//        mockMvc.perform(post("/api/manager/locations/register-worker")
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(payload))
//                .andExpect(status().isBadRequest())
//                .andExpect(content().string("Driver name is required."));
//    }
//}