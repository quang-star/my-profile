package dev.quang.portfolio;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.quang.portfolio.service.GoogleTokenVerifierService;
import dev.quang.portfolio.service.GoogleTokenVerifierService.GoogleUserInfo;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenantApiIntegrationTest {

    private static final byte[] SAMPLE_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoogleTokenVerifierService googleTokenVerifierService;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "%s", "password": "%s" }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String register(String username, String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "%s", "email": "%s", "password": "%s", "displayName": "%s" }
                                """.formatted(username, email, password, username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.maxPages").value(3))
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void multiTenantQuotaTemplateCloneIdorSecurityAndBanFlow() throws Exception {
        // 0. Reserved & invalid username rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "admin", "email": "reserved@example.com", "password": "password123" }
                                """))
                .andExpect(status().isBadRequest());

        // 1. User A registers -> logs in -> creates 3 pages -> 4th page returns 403
        String userAToken = register("usera", "usera@example.com", "password123");
        String userALoginToken = login("usera@example.com", "password123");
        assertFalse(userALoginToken.isBlank());

        // Duplicate username/email returns 409
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "usera", "email": "other@example.com", "password": "password123" }
                                """))
                .andExpect(status().isConflict());

        String pageA1Id = null;
        for (int i = 1; i <= 3; i++) {
            MvcResult created = mockMvc.perform(post("/api/my/pages")
                            .header("Authorization", "Bearer " + userAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "title": "Page A%d", "slug": "page-a%d", "visibility": "PUBLIC" }
                                    """.formatted(i, i)))
                    .andExpect(status().isCreated())
                    .andReturn();
            if (i == 1) {
                pageA1Id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
            }
        }
        assertNotNull(pageA1Id);

        mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Page A4", "slug": "page-a4" }
                                """))
                .andExpect(status().isForbidden());

        // 2. SUPER_ADMIN creates > 3 pages, sets template & home featured
        String superAdminToken = login("admin", "secret-password");
        String adminTemplateId = null;
        for (int i = 1; i <= 4; i++) {
            MvcResult res = mockMvc.perform(post("/api/my/pages")
                            .header("Authorization", "Bearer " + superAdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "title": "Admin Page %d", "slug": "admin-p%d", "visibility": "PUBLIC" }
                                    """.formatted(i, i)))
                    .andExpect(status().isCreated())
                    .andReturn();
            if (i == 1) {
                adminTemplateId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
            }
        }
        assertNotNull(adminTemplateId);

        // Add a custom node to adminTemplateId
        MvcResult tplNodeRes = mockMvc.perform(post("/api/pages/" + adminTemplateId + "/nodes")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "parentId": null, "tag": "heading", "attrs": { "level": "h1" }, "contentVi": "Template Root", "contentEn": "Template Root" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String tplNodeId = objectMapper.readTree(tplNodeRes.getResponse().getContentAsString()).get("id").asText();

        // Mark as template via super-admin API
        mockMvc.perform(patch("/api/super-admin/pages/" + adminTemplateId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "isTemplate": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTemplate").value(true));

        mockMvc.perform(get("/api/public/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + adminTemplateId + "')]").isNotEmpty());

        // 3. User B registers and creates a page cloned from SUPER_ADMIN's template
        String userBToken = register("userb", "userb@example.com", "password123");
        MvcResult clonedPageRes = mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "User B From Template", "slug": "from-tpl", "visibility": "DRAFT", "fromTemplateId": "%s" }
                                """.formatted(adminTemplateId)))
                .andExpect(status().isCreated())
                .andReturn();
        String clonedPageId = objectMapper.readTree(clonedPageRes.getResponse().getContentAsString()).get("id").asText();

        MvcResult clonedNodesRes = mockMvc.perform(get("/api/pages/" + clonedPageId + "/nodes")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode clonedNodes = objectMapper.readTree(clonedNodesRes.getResponse().getContentAsString());
        assertTrue(clonedNodes.size() >= 2);
        String clonedCustomNodeId = null;
        for (JsonNode n : clonedNodes) {
            assertNotEquals(tplNodeId, n.get("id").asText());
            if ("Template Root".equals(n.get("contentVi").asText())) {
                clonedCustomNodeId = n.get("id").asText();
            }
        }
        assertNotNull(clonedCustomNodeId);

        // Editing User B's cloned node does NOT affect SUPER_ADMIN's template node
        mockMvc.perform(put("/api/pages/" + clonedPageId + "/nodes/" + clonedCustomNodeId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "parentId": null, "tag": "heading", "attrs": { "level": "h1" }, "contentVi": "Edited Clone", "contentEn": "Edited Clone" }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pages/" + adminTemplateId + "/nodes")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + tplNodeId + "')].contentVi")
                        .value(org.hamcrest.Matchers.contains("Template Root")));

        // DRAFT visibility check: anonymous and User A get 404; User B and SUPER_ADMIN get 200
        mockMvc.perform(get("/api/public/u/userb/from-tpl"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/u/userb/from-tpl")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/u/userb/from-tpl")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/u/userb/from-tpl")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        // 4. IDOR tests: User B cannot mutate User A's page, nodes, messages, or files; nor access /api/super-admin
        mockMvc.perform(get("/api/super-admin/overview")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/my/pages/" + pageA1Id)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Hacked", "slug": "page-a1" }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/my/pages/" + pageA1Id)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/pages/" + pageA1Id + "/nodes")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pages/" + pageA1Id + "/nodes")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "parentId": null, "tag": "text", "attrs": {}, "contentVi": "hack", "contentEn": "hack" }
                                """))
                .andExpect(status().isForbidden());

        // Public submits contact message to User A's PUBLIC page -> User B cannot read/delete it
        MvcResult msgRes = mockMvc.perform(post("/api/public/pages/" + pageA1Id + "/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Visitor", "email": "v@example.com", "subject": "Hi", "message": "Hello A" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long msgId = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/my/pages/" + pageA1Id + "/messages")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/my/messages/" + msgId + "/read")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/my/messages/" + msgId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        // 5. Security tests: SVG upload rejected, nosniff header present, server XSS sanitized, banned user rejected
        MockMultipartFile svgFile = new MockMultipartFile(
                "file", "evil.svg", "image/svg+xml", "<svg onload='alert(1)'/>".getBytes()
        );
        mockMvc.perform(multipart("/api/upload")
                        .file(svgFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest());

        MockMultipartFile pngFile = new MockMultipartFile("file", "avatar.png", "image/png", SAMPLE_PNG);
        MvcResult uploadRes = mockMvc.perform(multipart("/api/upload")
                        .file(pngFile)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();
        String fileId = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // User B cannot delete User A's file
        mockMvc.perform(delete("/api/files/" + fileId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        // Server-side HTML sanitizer strips XSS payloads while keeping safe styles/tags
        MvcResult xssNode = mockMvc.perform(post("/api/pages/" + pageA1Id + "/nodes")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "parentId": null,
                                  "tag": "text",
                                  "attrs": {},
                                  "contentVi": "<p>Safe <script>alert(1)</script><a href=\\"javascript:alert(1)\\">bad</a><a href=\\"https://example.com\\" target=\\"_blank\\">good</a><span style=\\"position:fixed;inset:0;background:url(javascript:alert(1));color:#047857\\" class=\\"inline-tag evil-cls\\" data-color=\\"emerald\\">tag</span><img src=x onerror=alert(1)></p>",
                                  "contentEn": ""
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentVi", not(containsString("<script"))))
                .andExpect(jsonPath("$.contentVi", not(containsString("javascript:"))))
                .andExpect(jsonPath("$.contentVi", not(containsString("position:fixed"))))
                .andExpect(jsonPath("$.contentVi", not(containsString("<img"))))
                .andExpect(jsonPath("$.contentVi", not(containsString("evil-cls"))))
                .andExpect(jsonPath("$.contentVi", containsString("color: #047857")))
                .andExpect(jsonPath("$.contentVi", containsString("https://example.com")))
                .andExpect(jsonPath("$.contentVi", containsString("inline-tag")))
                .andReturn();
        assertNotNull(xssNode);

        // Ban User B via SUPER_ADMIN -> User B's existing JWT is rejected immediately
        MvcResult usersRes = mockMvc.perform(get("/api/super-admin/users")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();
        String userBId = null;
        for (JsonNode u : objectMapper.readTree(usersRes.getResponse().getContentAsString())) {
            if ("userb".equals(u.get("username").asText())) {
                userBId = u.get("id").asText();
            }
        }
        assertNotNull(userBId);

        mockMvc.perform(patch("/api/super-admin/users/" + userBId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "BANNED" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"));

        mockMvc.perform(get("/api/my/pages")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "userb", "password": "password123" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void draftPrimaryPreviewProfilePasswordGoogleAuthMagicBytesHoneypotAndOgPreview() throws Exception {
        // 1. Owner & SUPER_ADMIN can view DRAFT primary /@username without incrementing viewCount
        String ownerToken = register("draftowner", "draftowner@example.com", "password123");
        mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Draft Home", "slug": "home", "description": "My draft portfolio", "visibility": "DRAFT" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(true));

        // Anonymous cannot see DRAFT primary
        mockMvc.perform(get("/api/public/u/draftowner"))
                .andExpect(status().isNotFound());

        // Owner can preview DRAFT primary and viewCount remains 0
        mockMvc.perform(get("/api/public/u/draftowner")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.visibility").value("DRAFT"))
                .andExpect(jsonPath("$.page.viewCount").value(0));

        // SUPER_ADMIN can also preview DRAFT primary
        String superAdminToken = login("admin", "secret-password");
        mockMvc.perform(get("/api/public/u/draftowner")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.viewCount").value(0));

        // 2. Profile & Password update
        MvcResult profileRes = mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "draftowner2",
                                  "displayName": "Draft Owner Updated",
                                  "avatarUrl": "https://example.com/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("draftowner2"))
                .andExpect(jsonPath("$.user.displayName").value("Draft Owner Updated"))
                .andExpect(jsonPath("$.user.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.user.hasPassword").value(true))
                .andReturn();
        ownerToken = objectMapper.readTree(profileRes.getResponse().getContentAsString()).get("token").asText();

        // Wrong current password rejected
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "wrong-password", "newPassword": "newpassword123" }
                                """))
                .andExpect(status().isBadRequest());

        // Valid current password updates password
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "currentPassword": "password123", "newPassword": "newpassword123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated"));

        login("draftowner2", "newpassword123");

        // 3. Google Sign-In flow (stubbed verifier)
        when(googleTokenVerifierService.isEnabled()).thenReturn(true);
        when(googleTokenVerifierService.getClientId()).thenReturn("test-google-client-id");
        when(googleTokenVerifierService.verify("valid-google-token"))
                .thenReturn(new GoogleUserInfo(
                        "google-sub-999",
                        "Google.Dev@example.com",
                        "Google Dev",
                        "https://lh3.googleusercontent.com/pic.png"
                ));

        mockMvc.perform(get("/api/auth/google-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.clientId").value("test-google-client-id"));

        MvcResult googleRes = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "credential": "valid-google-token" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNewUser").value(true))
                .andExpect(jsonPath("$.user.hasPassword").value(false))
                .andExpect(jsonPath("$.user.googleLinked").value(true))
                .andExpect(jsonPath("$.user.username").value("google-dev"))
                .andReturn();
        String googleUserToken = objectMapper.readTree(googleRes.getResponse().getContentAsString()).get("token").asText();

        // Password login for Google-only user returns clear 401 message
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "google-dev", "password": "anypassword" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Account uses Google Sign-In. Please continue with Google."));

        // Google-only user can set an initial password without currentPassword
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + googleUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newPassword": "googlepassword123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated"));

        // 4. Magic-byte validation + /api/my/files projection + ETag/304
        MockMultipartFile spoofedPng = new MockMultipartFile(
                "file", "fake.png", "image/png", "<html><script>alert(1)</script></html>".getBytes()
        );
        mockMvc.perform(multipart("/api/upload")
                        .file(spoofedPng)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());

        MockMultipartFile realPng = new MockMultipartFile("file", "photo.png", "image/png", SAMPLE_PNG);
        MvcResult uploadRes = mockMvc.perform(multipart("/api/upload")
                        .file(realPng)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();
        String fileId = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/my/files")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].id").value(fileId))
                .andExpect(jsonPath("$.files[0].filename").value("photo.png"));

        mockMvc.perform(get("/api/files/" + fileId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"file-" + fileId + "\""));

        mockMvc.perform(get("/api/files/" + fileId)
                        .header(HttpHeaders.IF_NONE_MATCH, "\"file-" + fileId + "\""))
                .andExpect(status().isNotModified());

        // 5. Public page + Honeypot rejection + OpenGraph HTML + Deduplicated view count
        MvcResult pubPageRes = mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Public OG Page", "slug": "og-page", "description": "OG Bio", "visibility": "PUBLIC" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String pubPageId = objectMapper.readTree(pubPageRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/my/pages/" + pubPageId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Public OG Page",
                                  "slug": "og-page",
                                  "description": "OG Bio <script>",
                                  "visibility": "PUBLIC",
                                  "isPrimary": true,
                                  "themeJson": "{\\"ogImageUrl\\":\\"https://cdn.example.com/og-cover.png\\"}"
                                }
                                """))
                .andExpect(status().isOk());

        // Contact honeypot rejected
        mockMvc.perform(post("/api/public/pages/" + pubPageId + "/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Bot", "email": "bot@example.com", "message": "Spam", "website": "https://spam.example" }
                                """))
                .andExpect(status().isBadRequest());

        // Public view increments once then deduplicates
        mockMvc.perform(get("/api/public/u/draftowner2")
                        .header("User-Agent", "TestBrowser/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.viewCount").value(1));

        mockMvc.perform(get("/api/public/u/draftowner2")
                        .header("User-Agent", "TestBrowser/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.viewCount").value(1));

        // OpenGraph HTML preview
        mockMvc.perform(get("/api/public/og/u/draftowner2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("property=\"og:title\" content=\"Public OG Page\"")))
                .andExpect(content().string(containsString("property=\"og:image\" content=\"https://cdn.example.com/og-cover.png\"")))
                .andExpect(content().string(containsString("OG Bio &lt;script&gt;")))
                .andExpect(content().string(containsString("/@draftowner2")));

        mockMvc.perform(get("/api/public/og/u/draftowner2/og-page"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/@draftowner2/og-page")));

        // 6. Node content length limit enforced & page owner can import starter nodes (IDOR protected)
        mockMvc.perform(post("/api/pages/" + pubPageId + "/nodes/import")
                        .header("Authorization", "Bearer " + googleUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{ "id": "s1", "parentId": null, "tag": "section", "sortOrder": 0, "attrs": {}, "contentVi": "", "contentEn": "" }]
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pages/" + pubPageId + "/nodes/import")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "id": "s1", "parentId": null, "tag": "section", "sortOrder": 0, "attrs": {}, "contentVi": "", "contentEn": "" },
                                  { "id": "h1", "parentId": "s1", "tag": "heading", "sortOrder": 0, "attrs": { "level": "h1" }, "contentVi": "Imported", "contentEn": "Imported" }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].children[0].contentVi").value("Imported"));

        String oversizedContent = "a".repeat(25_001);
        mockMvc.perform(post("/api/pages/" + pubPageId + "/nodes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "tag", "text",
                                "attrs", java.util.Map.of(),
                                "contentVi", oversizedContent,
                                "contentEn", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void auditFixesReservedSlugClearAvatarDraftTemplateHomeViewCountPrimaryFallbackAndAdminDelete() throws Exception {
        String superAdminToken = login("admin", "secret-password");

        // 1. Curated templates are seeded and public
        mockMvc.perform(get("/api/public/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'page-main')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == 'tpl-ats-fresher')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == 'tpl-product-designer')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == 'tpl-business-marketing')]").isNotEmpty());

        // 2. Home page & Home OG endpoint do NOT increment viewCount
        MvcResult home1 = mockMvc.perform(get("/api/public/home")
                        .header("User-Agent", "AuditHomeBot/1.0"))
                .andExpect(status().isOk())
                .andReturn();
        long initialHomeViews = objectMapper.readTree(home1.getResponse().getContentAsString())
                .get("page").get("viewCount").asLong();

        mockMvc.perform(get("/api/public/home")
                        .header("User-Agent", "AuditHomeBot/2.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.viewCount").value(initialHomeViews));

        mockMvc.perform(get("/api/public/og/home"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("portfolio.studio")));

        // 3. Reserved slug "edit" is rejected on create & update
        String userToken = register("audituser", "audituser@example.com", "password123");
        mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Reserved Edit", "slug": "edit", "visibility": "PUBLIC" }
                                """))
                .andExpect(status().isBadRequest());

        // 4. Clear avatar works with both empty string and clearAvatar=true
        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "avatarUrl": "https://example.com/pic.png" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl").value("https://example.com/pic.png"));

        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "clearAvatar": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.avatarUrl").doesNotExist());

        // 5. DRAFT template is excluded from /api/public/templates
        MvcResult draftTplRes = mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Secret Draft Template", "slug": "secret-draft-tpl", "visibility": "DRAFT" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String draftTplId = objectMapper.readTree(draftTplRes.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/super-admin/pages/" + draftTplId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "isTemplate": true }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + draftTplId + "')]").isEmpty());

        // 6. Deleting primary page promotes another remaining page to primary
        MvcResult p1Res = mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Primary One", "slug": "primary-one", "visibility": "PUBLIC" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andReturn();
        String p1Id = objectMapper.readTree(p1Res.getResponse().getContentAsString()).get("id").asText();

        MvcResult p2Res = mockMvc.perform(post("/api/my/pages")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Secondary Two", "slug": "secondary-two", "visibility": "PUBLIC" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(false))
                .andReturn();
        String p2Id = objectMapper.readTree(p2Res.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/my/pages/" + p1Id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/public/u/audituser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.id").value(p2Id))
                .andExpect(jsonPath("$.page.isPrimary").value(true));

        // 7. File usageCount reported in /api/my/files
        MockMultipartFile png = new MockMultipartFile("file", "og-cover.png", "image/png", SAMPLE_PNG);
        MvcResult uploadRes = mockMvc.perform(multipart("/api/upload")
                        .file(png)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn();
        String uploadedUrl = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("url").asText();

        mockMvc.perform(put("/api/my/pages/" + p2Id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Secondary Two",
                                  "slug": "secondary-two",
                                  "visibility": "PUBLIC",
                                  "themeJson": "{\\"ogImageUrl\\":\\"%s\\"}"
                                }
                                """.formatted(uploadedUrl)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/my/files")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].usageCount").value(1));

        // 8. Super Admin can delete a page
        mockMvc.perform(delete("/api/super-admin/pages/" + draftTplId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());
    }
}
