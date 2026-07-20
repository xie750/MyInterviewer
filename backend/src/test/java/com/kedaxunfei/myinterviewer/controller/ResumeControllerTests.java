package com.kedaxunfei.myinterviewer.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = {"/schema-test.sql", "/data-test.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResumeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void parseResumeRequiresLogin() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Java Spring Boot 项目经验".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resumes/parse").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void parseResumeRejectsUnsupportedFileType() throws Exception {
        String token = loginAndExtractToken("user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "bad".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resumes/parse")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("仅支持")));
    }

    @Test
    void parseTextResumeReturnsStructuredContext() throws Exception {
        String token = loginAndExtractToken("user");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                MediaType.TEXT_PLAIN_VALUE,
                """
                        张三 Java 后端工程师，熟悉 Java、Spring Boot、MySQL、Redis、Docker。
                        主导智能面试平台项目，负责登录鉴权、权限隔离、面试会话和报告生成服务设计，接口延迟降低 30%。
                        """.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/resumes/parse")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary", containsString("技能关键词")))
                .andExpect(jsonPath("$.data.skills", hasSize(5)))
                .andExpect(jsonPath("$.data.projects[0]", containsString("智能面试平台项目")));
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
