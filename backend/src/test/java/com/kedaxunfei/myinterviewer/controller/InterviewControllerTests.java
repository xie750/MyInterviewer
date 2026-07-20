package com.kedaxunfei.myinterviewer.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.jayway.jsonpath.JsonPath;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class InterviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void styleListRequiresLoginAndReturnsEnabledStyles() throws Exception {
        mockMvc.perform(get("/api/interviewer-styles"))
                .andExpect(status().isUnauthorized());

        String token = loginAndExtractToken("user");
        mockMvc.perform(get("/api/interviewer-styles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("严厉压力面"))
                .andExpect(jsonPath("$.data[1].name").value("温和引导面"));
    }

    @Test
    void userCanCompleteTextInterviewAndReadReport() throws Exception {
        String token = loginAndExtractToken("user");

        MvcResult createResult = mockMvc.perform(post("/api/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"positionId\":1,\"styleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.messages", hasSize(1)))
                .andExpect(jsonPath("$.data.messages[0].content", containsString("Java 后端工程师")))
                .andReturn();

        String sessionId = extractId(createResult);

        mockMvc.perform(post("/api/interviews/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "我负责设计登录鉴权、权限隔离、异常处理和接口测试，并用监控指标观察上线效果。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(2))
                .andExpect(jsonPath("$.data.messages", hasSize(3)))
                .andExpect(jsonPath("$.data.messages[2].role").value("ASSISTANT"));

        mockMvc.perform(post("/api/interviews/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.report.totalScore", notNullValue()));

        mockMvc.perform(get("/api/interviews/" + sessionId + "/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary", containsString("Java 后端工程师")));
    }

    @Test
    void userCannotReadOtherUsersInterview() throws Exception {
        String token = loginAndExtractToken("user");

        mockMvc.perform(get("/api/interviews/100").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void userHistoryOnlyReturnsOwnInterviews() throws Exception {
        String token = loginAndExtractToken("user");

        mockMvc.perform(get("/api/interviews").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
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

    private String extractId(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.id").toString();
    }
}
