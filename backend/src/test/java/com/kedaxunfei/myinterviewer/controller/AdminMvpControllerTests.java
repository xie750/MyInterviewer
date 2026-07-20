package com.kedaxunfei.myinterviewer.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminMvpControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminInterviewListRequiresAdminRole() throws Exception {
        String userToken = loginAndExtractToken("user");
        String adminToken = loginAndExtractToken("admin");

        mockMvc.perform(get("/api/admin/interviews").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/interviews").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].username").value("admin"))
                .andExpect(jsonPath("$.data[0].totalScore").value(78));
    }

    @Test
    void adminCanListUsersAndUpdateNormalUserStatus() throws Exception {
        String userToken = loginAndExtractToken("user");
        String adminToken = loginAndExtractToken("admin");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));

        mockMvc.perform(patch("/api/admin/users/3/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("disabled"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));
    }

    @Test
    void adminCanManagePostureEventsAndThresholds() throws Exception {
        String userToken = loginAndExtractToken("user");
        String adminToken = loginAndExtractToken("admin");

        mockMvc.perform(get("/api/admin/posture-events").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/posture-events")
                        .param("eventType", "LOW_LIGHT")
                        .param("page", "1")
                        .param("pageSize", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].sessionId").value(100))
                .andExpect(jsonPath("$.data.items[0].username").value("admin"));

        mockMvc.perform(get("/api/posture-thresholds").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(7)));

        mockMvc.perform(get("/api/admin/posture-thresholds").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(putThreshold(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventType").value("LOW_LIGHT"))
                .andExpect(jsonPath("$.data.warningThreshold").value(50));
    }

    @Test
    void adminCanManageVirtualHumanAssets() throws Exception {
        String userToken = loginAndExtractToken("user");
        String adminToken = loginAndExtractToken("admin");

        mockMvc.perform(get("/api/admin/virtual-humans").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/virtual-humans")
                        .param("keyword", "stern")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].assetKey").value("stern-panel"));

        mockMvc.perform(post("/api/admin/virtual-humans")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assetKey":"demo-coach",
                                  "name":"演示教练",
                                  "description":"用于最终演示的素材配置",
                                  "imageUrl":"https://example.com/demo.png",
                                  "accentColor":"#0f766e",
                                  "badge":"演示",
                                  "enabled":true,
                                  "sortOrder":90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetKey").value("demo-coach"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    private org.springframework.test.web.servlet.RequestBuilder putThreshold(String adminToken) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/admin/posture-thresholds/1")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "eventType":"LOW_LIGHT",
                          "displayName":"画面亮度偏低",
                          "description":"测试调整阈值",
                          "warningThreshold":50,
                          "criticalThreshold":25,
                          "enabled":true,
                          "sortOrder":10
                        }
                        """);
    }

    private String loginAndExtractToken(String username) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        return response.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }
}
