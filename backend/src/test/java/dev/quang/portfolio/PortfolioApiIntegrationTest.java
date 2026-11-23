package dev.quang.portfolio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicPortfolioAndProtectedAdminCrudFlow() throws Exception {
        // 1. Public health & seeded portfolio
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nguyễn Kim Quang"))
                .andExpect(jsonPath("$.projects.length()").value(6));

        // 2. Protected admin route rejects anonymous request
        mockMvc.perform(get("/api/admin/messages"))
                .andExpect(status().isUnauthorized());

        // 3. Public contact submission succeeds
        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "HR Recruiter",
                                  "email": "hr@example.com",
                                  "subject": "Java Backend Intern",
                                  "message": "We would love to interview you."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("HR Recruiter"));

        // 4. Login with admin credentials -> JWT
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "secret-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("token").asText();

        // 5. Admin reads contact messages
        mockMvc.perform(get("/api/admin/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("hr@example.com"));

        // 6. Admin creates and deletes a project
        MvcResult createProject = mockMvc.perform(post("/api/admin/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Cloud Gateway Demo",
                                  "category": "fullstack",
                                  "featured": false,
                                  "year": "2026",
                                  "tagline": { "vi": "API Gateway demo", "en": "API Gateway demo" },
                                  "description": { "vi": "Mô tả", "en": "Description" },
                                  "highlights": { "vi": ["Rate limit"], "en": ["Rate limit"] },
                                  "stack": ["Java", "Spring Cloud"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Spring Cloud Gateway Demo"))
                .andReturn();

        String createdId = objectMapper.readTree(createProject.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/admin/projects/" + createdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 7. Tag-node tree public read + admin CRUD
        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("sec-hero"))
                .andExpect(jsonPath("$[0].children").isArray());

        MvcResult createNode = mockMvc.perform(post("/api/admin/nodes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": "sec-hero",
                                  "tag": "text",
                                  "attrs": { "bgColor": "#ecfdf5", "textColor": "#047857" },
                                  "contentVi": "<p>Xin chào <span class=\\"inline-tag\\" data-color=\\"emerald\\">Java</span></p>",
                                  "contentEn": "<p>Hello <span class=\\"inline-tag\\" data-color=\\"emerald\\">Java</span></p>"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value("sec-hero"))
                .andExpect(jsonPath("$.attrs.bgColor").value("#ecfdf5"))
                .andReturn();

        String createdNodeId = objectMapper.readTree(createNode.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(put("/api/admin/nodes/" + createdNodeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": "sec-hero",
                                  "tag": "heading",
                                  "attrs": { "level": "h3", "textColor": "#0f172a" },
                                  "contentVi": "Đã cập nhật",
                                  "contentEn": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("heading"));

        MvcResult insertedIcon = mockMvc.perform(post("/api/admin/nodes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": "sec-hero",
                                  "beforeId": "%s",
                                  "tag": "icon",
                                  "attrs": { "icon": "github", "colStart": 2, "colSpan": 3, "minHeight": 96 }
                                }
                                """.formatted(createdNodeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attrs.icon").value("github"))
                .andReturn();
        JsonNode icon = objectMapper.readTree(insertedIcon.getResponse().getContentAsString());
        String iconId = icon.get("id").asText();
        int insertIndex = icon.get("sortOrder").asInt();
        String childrenPath = "$[0].children";

        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(childrenPath + "[" + insertIndex + "].id").value(iconId))
                .andExpect(jsonPath(childrenPath + "[" + (insertIndex + 1) + "].id").value(createdNodeId));

        mockMvc.perform(put("/api/admin/nodes/reorder")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "items": [
                                  { "id": "%s", "parentId": "sec-hero", "sortOrder": %d, "clearGridPosition": true },
                                  { "id": "%s", "parentId": "sec-hero", "sortOrder": %d }
                                ] }
                                """.formatted(iconId, insertIndex + 1, createdNodeId, insertIndex)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(childrenPath + "[" + insertIndex + "].id").value(createdNodeId))
                .andExpect(jsonPath(childrenPath + "[" + (insertIndex + 1) + "].attrs.colStart").doesNotExist())
                .andExpect(jsonPath(childrenPath + "[" + (insertIndex + 1) + "].attrs.colSpan").value(3))
                .andExpect(jsonPath(childrenPath + "[" + (insertIndex + 1) + "].attrs.minHeight").value(96));

        mockMvc.perform(delete("/api/admin/nodes/" + iconId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/nodes/" + createdNodeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        JsonNode historyCard = objectMapper.readTree("""
                {
                  "id": "history-test-card", "parentId": "sec-hero", "tag": "card", "sortOrder": 900,
                  "attrs": { "responsive": { "phone": { "minHeight": 120 } } },
                  "contentVi": "", "contentEn": ""
                }
                """);
        JsonNode historyChild = objectMapper.readTree("""
                {
                  "id": "history-test-child", "parentId": "history-test-card", "tag": "text", "sortOrder": 0,
                  "attrs": {}, "contentVi": "Mẫu", "contentEn": "Example"
                }
                """);
        var createChange = objectMapper.createObjectNode();
        createChange.putArray("expected");
        createChange.set("replacement", objectMapper.createArrayNode().add(historyChild).add(historyCard));

        mockMvc.perform(put("/api/admin/nodes/changes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createChange.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].children[?(@.id == 'history-test-card')].children[0].id")
                        .value(org.hamcrest.Matchers.contains("history-test-child")));

        var partialUndo = objectMapper.createObjectNode();
        partialUndo.set("expected", objectMapper.createArrayNode().add(historyCard));
        partialUndo.putArray("replacement");
        mockMvc.perform(put("/api/admin/nodes/changes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partialUndo.toString()))
                .andExpect(status().isConflict());

        var undo = objectMapper.createObjectNode();
        undo.set("expected", objectMapper.createArrayNode().add(historyChild).add(historyCard));
        undo.putArray("replacement");
        mockMvc.perform(put("/api/admin/nodes/changes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(undo.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].children[?(@.id == 'history-test-card')]").isEmpty());

        mockMvc.perform(put("/api/admin/nodes/changes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(undo.toString()))
                .andExpect(status().isConflict());
    }
}
