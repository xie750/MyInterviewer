package com.kedaxunfei.myinterviewer.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PositionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enabledPositionListRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/positions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enabledPositionListReturnsOnlyEnabledPositions() throws Exception {
        String token = loginAndExtractToken("user");

        mockMvc.perform(get("/api/positions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Java 后端工程师"))
                .andExpect(jsonPath("$.data[1].name").value("前端工程师"));
    }

    @Test
    void adminPositionListRejectsUserRole() throws Exception {
        String token = loginAndExtractToken("user");

        mockMvc.perform(get("/api/admin/positions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPositionListReturnsAllPositions() throws Exception {
        String token = loginAndExtractToken("admin");

        mockMvc.perform(get("/api/admin/positions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    void adminCanCreateUpdateAndDisablePosition() throws Exception {
        String token = loginAndExtractToken("admin");

        MvcResult createResult = mockMvc.perform(post("/api/admin/positions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试工程师",
                                  "description": "质量保障岗位",
                                  "techStack": "Testing, Automation",
                                  "difficulty": "初级",
                                  "promptTemplate": "考察测试思维",
                                  "enabled": true,
                                  "sortOrder": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name").value("测试工程师"))
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        String id = response.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(put("/api/admin/positions/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "测试开发工程师",
                                  "description": "自动化测试岗位",
                                  "techStack": "Java, Selenium",
                                  "difficulty": "中级",
                                  "promptTemplate": "考察测试开发能力",
                                  "enabled": true,
                                  "sortOrder": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试开发工程师"))
                .andExpect(jsonPath("$.data.sortOrder").value(6));

        mockMvc.perform(patch("/api/admin/positions/" + id + "/enabled")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
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
