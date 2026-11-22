package com.unishift.unishift_backend.gamification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishift.unishift_backend.booking.Booking;
import com.unishift.unishift_backend.booking.BookingRepository;
import com.unishift.unishift_backend.booking.BookingStatus;
import com.unishift.unishift_backend.gamification.entity.ElementTier;
import com.unishift.unishift_backend.gamification.repository.ElementTierRepository;
import com.unishift.unishift_backend.gamification.repository.PointTransactionRepository;
import com.unishift.unishift_backend.gamification.repository.StudentGamificationRepository;
import com.unishift.unishift_backend.gamification.repository.WorkSessionRepository;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.job.JobStatus;
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

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class GamificationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WorkSessionRepository workSessionRepository;

    @Autowired
    private PointTransactionRepository transactionRepository;

    @Autowired
    private StudentGamificationRepository gamificationRepository;

    @Autowired
    private ElementTierRepository tierRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        transactionRepository.deleteAll();
        workSessionRepository.deleteAll();
        gamificationRepository.deleteAll();
        bookingRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();

        // Seed H-1 tier if not present
        if (tierRepository.findByTierCode("H-1").isEmpty()) {
            tierRepository.save(new ElementTier("H-1", "Hydrogen", "H", 1, 10, 1, true));
        }
        if (tierRepository.findByTierCode("He-4").isEmpty()) {
            tierRepository.save(new ElementTier("He-4", "Helium", "He", 4, 40, 2, true));
        }
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
        User employer = new User(
            "Employer " + email,
            email,
            passwordEncoder.encode("Password123!"),
            UserRole.EMPLOYER,
            true,
            true
        );
        userRepository.save(employer);

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

    @Test
    void testFullWorkSessionAndGamificationWorkflow() throws Exception {
        String studentEmail = "stud1@example.com";
        String employerEmail = "emp1@example.com";

        String studentToken = getStudentToken(studentEmail);
        String employerToken = getEmployerToken(employerEmail);

        User student = userRepository.findByEmail(studentEmail).orElseThrow();
        User employer = userRepository.findByEmail(employerEmail).orElseThrow();

        // Create published job
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = start.plus(4, ChronoUnit.HOURS);
        Job job = new Job(
            employer,
            "Campus Lab Assistant",
            "Assist lab operations",
            "Science",
            "Science Building",
            new BigDecimal("20.00"),
            "HOURLY",
            start,
            end,
            5
        );
        job.publish();
        jobRepository.save(job);

        // Create confirmed booking
        Booking booking = new Booking(job, student, BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 1. Initial Gamification Summary (Unranked / Starter)
        mockMvc.perform(get("/api/v1/students/me/gamification")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPoints", is(0)))
            .andExpect(jsonPath("$.pointsToNextTier", is(10)))
            .andExpect(jsonPath("$.progressPercent", is(0.0)));

        // 2. Student Submits Work Session (2 hours = 120 minutes)
        Instant workStart = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant workEnd = workStart.plus(2, ChronoUnit.HOURS);

        String submitJson = """
            {
                "startedAt": "%s",
                "endedAt": "%s",
                "notes": "Completed shift duties"
            }
            """.formatted(workStart.toString(), workEnd.toString());

        String sessionResponse = mockMvc.perform(post("/api/v1/bookings/" + booking.getId() + "/work-sessions")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(submitJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.verifiedMinutes", is(120)))
            .andExpect(jsonPath("$.status", is("SUBMITTED")))
            .andReturn().getResponse().getContentAsString();

        long sessionId = objectMapper.readTree(sessionResponse).get("id").asLong();

        // 3. Student tries to confirm own work session (must be forbidden)
        mockMvc.perform(post("/api/v1/employer/work-sessions/" + sessionId + "/confirm")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        // 4. Another employer tries to confirm work session (must be forbidden)
        String otherEmployerToken = getEmployerToken("otheremp@example.com");
        mockMvc.perform(post("/api/v1/employer/work-sessions/" + sessionId + "/confirm")
                .header("Authorization", "Bearer " + otherEmployerToken))
            .andExpect(status().isForbidden());

        // 5. Owning employer confirms work session (points awarded)
        mockMvc.perform(post("/api/v1/employer/work-sessions/" + sessionId + "/confirm")
                .header("Authorization", "Bearer " + employerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // 6. Verify Gamification Summary after 2 hours (120 min = 2 points, 8 points to H-1, 20% progress)
        mockMvc.perform(get("/api/v1/students/me/gamification")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPoints", is(2)))
            .andExpect(jsonPath("$.pointsToNextTier", is(8)))
            .andExpect(jsonPath("$.progressPercent", is(20.0)));

        // 7. Verify Point Transactions
        mockMvc.perform(get("/api/v1/students/me/gamification/transactions")
                .header("Authorization", "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].pointsDelta", is(2)))
            .andExpect(jsonPath("$.content[0].reason", is("VERIFIED_WORK_HOURS")));

        // 8. Confirm duplicate confirmation is prevented
        mockMvc.perform(post("/api/v1/employer/work-sessions/" + sessionId + "/confirm")
                .header("Authorization", "Bearer " + employerToken))
            .andExpect(status().isConflict());
    }
}
