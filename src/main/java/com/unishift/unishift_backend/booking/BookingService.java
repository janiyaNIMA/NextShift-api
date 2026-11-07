package com.unishift.unishift_backend.booking;

import com.unishift.unishift_backend.booking.dto.BookingResponse;
import com.unishift.unishift_backend.common.exception.ConflictException;
import com.unishift.unishift_backend.common.exception.ForbiddenException;
import com.unishift.unishift_backend.common.exception.ResourceNotFoundException;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.job.JobService;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobService jobService;

    public BookingService(
        BookingRepository bookingRepository,
        JobRepository jobRepository,
        UserRepository userRepository,
        JobService jobService
    ) {
        this.bookingRepository = bookingRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.jobService = jobService;
    }

    @Transactional
    public BookingResponse createBooking(Long jobId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        if (!student.isStudent()) {
            throw new ForbiddenException("Only registered students can book jobs");
        }

        if (!student.isActive()) {
            throw new ForbiddenException("Student account is inactive");
        }

        Job job = jobRepository.findByIdWithLock(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.isEligibleForBooking(Instant.now())) {
            throw new ConflictException("Job is not currently eligible for booking");
        }

        if (bookingRepository.existsByJobIdAndStudentId(jobId, student.getId())) {
            throw new ConflictException("Student has already booked this vacancy");
        }

        long confirmedCount = bookingRepository.countByJobIdAndStatus(jobId, BookingStatus.CONFIRMED);
        if (confirmedCount >= job.getCapacity()) {
            throw new ConflictException("Job booking capacity has been reached");
        }

        Booking booking = new Booking(job, student, BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        return BookingResponse.fromEntity(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getStudentBookings(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        if (!student.isStudent()) {
            throw new ForbiddenException("Only students can access their bookings feed");
        }

        return bookingRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
            .map(BookingResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForJob(Long jobId, String employerEmail) {
        Job job = jobService.getJobOwnedByEmployer(jobId, employerEmail);

        return bookingRepository.findByJobIdAndStatusOrderByCreatedAtDesc(job.getId(), BookingStatus.CONFIRMED).stream()
            .map(BookingResponse::fromEntity)
            .toList();
    }
}
