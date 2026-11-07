package com.unishift.unishift_backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishift.unishift_backend.booking.BookingRepository;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        bookingRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void studentRegistrationSucceedsWithApprovedStatus() throws Exception {
        String json = """
            {
                "name": "Alice Student",
                "email": "alice@student.uni.edu",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.tokenType", is("Bearer")))
            .andExpect(jsonPath("$.user.email", is("alice@student.uni.edu")))
            .andExpect(jsonPath("$.user.role", is("STUDENT")))
            .andExpect(jsonPath("$.user.approved", is(true)))
            .andExpect(jsonPath("$.user.active", is(true)));
    }

    @Test
    void employerRegistrationSucceedsWithPendingApprovalStatus() throws Exception {
        String json = """
            {
                "name": "Bob Employer",
                "email": "bob@cafe.com",
                "password": "Password123!",
                "role": "EMPLOYER"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.user.email", is("bob@cafe.com")))
            .andExpect(jsonPath("$.user.role", is("EMPLOYER")))
            .andExpect(jsonPath("$.user.approved", is(false)))
            .andExpect(jsonPath("$.user.active", is(true)));
    }

    @Test
    void publicAdminRegistrationIsProhibited() throws Exception {
        String json = """
            {
                "name": "Malicious Admin",
                "email": "hacker@admin.com",
                "password": "Password123!",
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void duplicateEmailRegistrationReturnsConflict() throws Exception {
        String json = """
            {
                "name": "Alice",
                "email": "alice@example.com",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void loginWithValidCredentialsReturnsJwt() throws Exception {
        String registerJson = """
            {
                "name": "Alice",
                "email": "alice.login@example.com",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
            .andExpect(status().isCreated());

        String loginJson = """
            {
                "email": "alice.login@example.com",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.user.email", is("alice.login@example.com")));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String registerJson = """
            {
                "name": "Alice",
                "email": "alice.wrong@example.com",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
            .andExpect(status().isCreated());

        String loginJson = """
            {
                "email": "alice.wrong@example.com",
                "password": "WrongPassword!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void getCurrentUserProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserProfileWithValidTokenSucceeds() throws Exception {
        String registerJson = """
            {
                "name": "Alice",
                "email": "alice.me@example.com",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;

        String responseJson = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(responseJson);
        String token = root.get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("alice.me@example.com")))
            .andExpect(jsonPath("$.role", is("STUDENT")));
    }
}
