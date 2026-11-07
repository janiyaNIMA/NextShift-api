package com.unishift.unishift_backend.booking;

import com.unishift.unishift_backend.common.exception.ConflictException;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import com.unishift.unishift_backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ConcurrentBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void concurrentBookingsForLastCapacitySlotAreSafelyHandled() throws Exception {
        User employer = new User("Owner", "owner@concurrent.com", "hash", UserRole.EMPLOYER, true, true);
        userRepository.save(employer);

        Instant startsAt = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant endsAt = startsAt.plus(4, ChronoUnit.HOURS);
        Job job = new Job(
            employer,
            "Urgent Shift",
            "Single urgent vacancy",
            "Event",
            "Auditorium",
            new BigDecimal("50.00"),
            "TOTAL",
            startsAt,
            endsAt,
            1
        );
        job.publish();
        Job savedJob = jobRepository.save(job);

        int numberOfStudents = 5;
        List<User> students = new ArrayList<>();
        for (int i = 0; i < numberOfStudents; i++) {
            User student = new User("Student " + i, "student" + i + "@concurrent.edu", "hash", UserRole.STUDENT, true, true);
            students.add(userRepository.save(student));
        }

        ExecutorService executor = Executors.newFixedThreadPool(numberOfStudents);
        CountDownLatch readyLatch = new CountDownLatch(numberOfStudents);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (User student : students) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    bookingService.createBooking(savedJob.getId(), student.getEmail());
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(numberOfStudents - 1);

        long confirmedInDb = bookingRepository.countByJobIdAndStatus(savedJob.getId(), BookingStatus.CONFIRMED);
        assertThat(confirmedInDb).isEqualTo(1);
    }
}
