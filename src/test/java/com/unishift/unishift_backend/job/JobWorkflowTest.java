package com.unishift.unishift_backend.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishift.unishift_backend.booking.BookingRepository;
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
public class JobWorkflowTest {

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

    private String getEmployerToken(boolean approved) throws Exception {
        User employer = new User(
            "Cafe Owner",
            "owner@cafe.com",
            passwordEncoder.encode("Password123!"),
            UserRole.EMPLOYER,
            true,
            approved
        );
        userRepository.save(employer);

        String loginJson = """
            {
                "email": "owner@cafe.com",
                "password": "Password123!"
            }
            """;
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void unapprovedEmployerCannotCreateJob() throws Exception {
        String token = getEmployerToken(false);

        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(4, ChronoUnit.HOURS);
        String createJobJson = """
            {
                "title": "Cashier",
                "description": "Cashier at counter",
                "category": "Retail",
                "location": "Bookstore",
                "payAmount": 20.00,
                "payUnit": "HOURLY",
                "startsAt": "%s",
                "endsAt": "%s",
                "capacity": 2
            }
            """.formatted(startsAt, endsAt);

        mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJobJson))
            .andExpect(status().isForbidden());
    }

    @Test
    void approvedEmployerCanCreateJobStartingInPendingState() throws Exception {
        String token = getEmployerToken(true);

        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(4, ChronoUnit.HOURS);
        String createJobJson = """
            {
                "title": "Cashier",
                "description": "Cashier at counter",
                "category": "Retail",
                "location": "Bookstore",
                "payAmount": 20.00,
                "payUnit": "HOURLY",
                "startsAt": "%s",
                "endsAt": "%s",
                "capacity": 2
            }
            """.formatted(startsAt, endsAt);

        mockMvc.perform(post("/api/v1/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJobJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andExpect(jsonPath("$.title", is("Cashier")));

        mockMvc.perform(get("/api/v1/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void publishedJobsAreVisibleInPublicFeedWithFiltersAndPagination() throws Exception {
        User employer = new User(
            "Library",
            "library@uni.edu",
            passwordEncoder.encode("Password123!"),
            UserRole.EMPLOYER,
            true,
            true
        );
        userRepository.save(employer);

        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(3, ChronoUnit.HOURS);

        Job job1 = new Job(
            employer,
            "Tutor",
            "Math tutoring",
            "Education",
            "Campus Hall",
            new BigDecimal("30.00"),
            "HOURLY",
            startsAt,
            endsAt,
            2
        );
        job1.publish();
        jobRepository.save(job1);

        Job job2 = new Job(
            employer,
            "Cleaner",
            "Evening cleanup",
            "Maintenance",
            "Science Lab",
            new BigDecimal("22.00"),
            "HOURLY",
            startsAt,
            endsAt,
            1
        );
        job2.publish();
        jobRepository.save(job2);

        mockMvc.perform(get("/api/v1/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements", is(2)));

        mockMvc.perform(get("/api/v1/jobs?category=Education"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title", is("Tutor")));

        mockMvc.perform(get("/api/v1/jobs?location=Science Lab"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title", is("Cleaner")));

        mockMvc.perform(get("/api/v1/jobs/" + job1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Tutor")))
            .andExpect(jsonPath("$.remainingCapacity", is(2)));
    }
}
