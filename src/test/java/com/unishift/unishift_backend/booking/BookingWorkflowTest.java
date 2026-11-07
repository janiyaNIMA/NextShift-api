package com.unishift.unishift_backend.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class BookingWorkflowTest {

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

    private String getStudentToken(String email) throws Exception {
        String registerJson = """
            {
                "name": "Student %s",
                "email": "%s",
                "password": "Password123!",
                "role": "STUDENT"
            }
            """.formatted(email, email);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private String getEmployerToken(String email) throws Exception {
        if (!userRepository.existsByEmail(email)) {
            User employer = new User(
                "Employer " + email,
                email,
                passwordEncoder.encode("Password123!"),
                UserRole.EMPLOYER,
                true,
                true
            );
            userRepository.save(employer);
        }

        String loginJson = """
            {
                "email": "%s",
                "password": "Password123!"
            }
            """.formatted(email);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private Job createPublishedJob(User employer, int capacity) {
        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(4, ChronoUnit.HOURS);
        Job job = new Job(
            employer,
            "Lab Assistant",
            "Assist students in lab",
            "Academic",
            "Science Block",
            new BigDecimal("25.00"),
            "HOURLY",
            startsAt,
            endsAt,
            capacity
        );
        job.publish();
        return jobRepository.save(job);
    }

    @Test
    void studentCanBookEligibleJob() throws Exception {
        User employer = new User("Owner", "owner@lab.com", passwordEncoder.encode("Password123!"), UserRole.EMPLOYER, true, true);
        userRepository.save(employer);
        Job job = createPublishedJob(employer, 2);

        String studentToken = getStudentToken("student1@uni.edu");

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status", is("CONFIRMED")))
            .andExpect(jsonPath("$.jobId", is(job.getId().intValue())))
            .andExpect(jsonPath("$.studentEmail", is("student1@uni.edu")));

        mockMvc.perform(get("/api/v1/bookings/me")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].jobId", is(job.getId().intValue())));
    }

    @Test
    void duplicateBookingReturnsConflict() throws Exception {
        User employer = new User("Owner", "owner.dup@lab.com", passwordEncoder.encode("Password123!"), UserRole.EMPLOYER, true, true);
        userRepository.save(employer);
        Job job = createPublishedJob(employer, 2);

        String studentToken = getStudentToken("student.dup@uni.edu");

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void capacityExhaustionPreventsOverbooking() throws Exception {
        User employer = new User("Owner", "owner.cap@lab.com", passwordEncoder.encode("Password123!"), UserRole.EMPLOYER, true, true);
        userRepository.save(employer);
        Job job = createPublishedJob(employer, 1);

        String student1Token = getStudentToken("student1.cap@uni.edu");
        String student2Token = getStudentToken("student2.cap@uni.edu");

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + student1Token))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + student2Token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void employerCanViewBookingsForOwnJobOnly() throws Exception {
        User employer1 = new User("Owner1", "owner1@lab.com", passwordEncoder.encode("Password123!"), UserRole.EMPLOYER, true, true);
        userRepository.save(employer1);
        Job job = createPublishedJob(employer1, 5);

        String employer1Token = getEmployerToken("owner1@lab.com");
        String employer2Token = getEmployerToken("owner2@lab.com");
        String studentToken = getStudentToken("stud.emp@uni.edu");

        mockMvc.perform(post("/api/v1/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/employer/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + employer1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].studentEmail", is("stud.emp@uni.edu")));

        mockMvc.perform(get("/api/v1/employer/jobs/" + job.getId() + "/bookings")
                .header("Authorization", "Bearer " + employer2Token))
            .andExpect(status().isForbidden());
    }
}
