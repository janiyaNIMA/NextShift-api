package com.unishift.unishift_backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishift.unishift_backend.booking.BookingRepository;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import com.unishift.unishift_backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdminWorkflowTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private String getAdminToken() throws Exception {
        User admin = new User(
            "Admin",
            "admin@test.com",
            passwordEncoder.encode("AdminPassword123!"),
            UserRole.ADMIN,
            true,
            true
        );
        userRepository.save(admin);

        String loginJson = """
            {
                "email": "admin@test.com",
                "password": "AdminPassword123!"
            }
            """;
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private String getStudentToken() throws Exception {
        String registerJson = """
            {
                "name": "Student",
                "email": "student@uni.edu",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """;
        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void studentCannotAccessAdminEndpoints() throws Exception {
        String studentToken = getStudentToken();

        mockMvc.perform(get("/api/v1/admin/employers/pending")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAndApprovePendingEmployers() throws Exception {
        User pendingEmployer = new User(
            "Coffee Shop",
            "coffee@shop.com",
            passwordEncoder.encode("Password123!"),
            UserRole.EMPLOYER,
            true,
            false
        );
        userRepository.save(pendingEmployer);

        String adminToken = getAdminToken();

        mockMvc.perform(get("/api/v1/admin/employers/pending")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].email", is("coffee@shop.com")))
            .andExpect(jsonPath("$[0].approved", is(false)));

        mockMvc.perform(patch("/api/v1/admin/employers/" + pendingEmployer.getId() + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(pendingEmployer.getId().intValue())))
            .andExpect(jsonPath("$.approved", is(true)));

        mockMvc.perform(get("/api/v1/admin/employers/pending")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void adminCanListAndApprovePendingJobs() throws Exception {
        User employer = new User(
            "Library Cafe",
            "cafe@uni.edu",
            passwordEncoder.encode("Password123!"),
            UserRole.EMPLOYER,
            true,
            true
        );
        userRepository.save(employer);

        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(4, ChronoUnit.HOURS);
        Job job = new Job(
            employer,
            "Barista Shift",
            "Morning shift making coffee",
            "Hospitality",
            "Campus Cafe",
            new BigDecimal("25.00"),
            "HOURLY",
            startsAt,
            endsAt,
            2
        );
        jobRepository.save(job);

        String adminToken = getAdminToken();

        mockMvc.perform(get("/api/v1/admin/jobs/pending")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title", is("Barista Shift")))
            .andExpect(jsonPath("$[0].status", is("PENDING")));

        mockMvc.perform(patch("/api/v1/admin/jobs/" + job.getId() + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(job.getId().intValue())))
            .andExpect(jsonPath("$.status", is("PUBLISHED")));

        mockMvc.perform(get("/api/v1/admin/jobs/pending")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
