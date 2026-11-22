package com.unishift.unishift_backend.gamification.service;

import com.unishift.unishift_backend.booking.Booking;
import com.unishift.unishift_backend.booking.BookingRepository;
import com.unishift.unishift_backend.booking.BookingStatus;
import com.unishift.unishift_backend.common.exception.BadRequestException;
import com.unishift.unishift_backend.common.exception.ConflictException;
import com.unishift.unishift_backend.common.exception.ForbiddenException;
import com.unishift.unishift_backend.common.exception.ResourceNotFoundException;
import com.unishift.unishift_backend.gamification.dto.RejectWorkSessionRequest;
import com.unishift.unishift_backend.gamification.dto.SubmitWorkSessionRequest;
import com.unishift.unishift_backend.gamification.dto.WorkSessionResponse;
import com.unishift.unishift_backend.gamification.entity.WorkSession;
import com.unishift.unishift_backend.gamification.enums.WorkSessionStatus;
import com.unishift.unishift_backend.gamification.repository.WorkSessionRepository;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class WorkSessionService {

    private final WorkSessionRepository workSessionRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;

    public WorkSessionService(
        WorkSessionRepository workSessionRepository,
        BookingRepository bookingRepository,
        UserRepository userRepository,
        GamificationService gamificationService
    ) {
        this.workSessionRepository = workSessionRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
    }

    @Transactional
    public WorkSessionResponse submitWorkSession(Long bookingId, SubmitWorkSessionRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only submit work sessions for your own bookings");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Work sessions can only be submitted for active confirmed bookings");
        }

        Instant startedAt = request.startedAt();
        Instant endedAt = request.endedAt();

        if (startedAt == null || endedAt == null) {
            throw new BadRequestException("Start and end timestamps are required");
        }

        if (!startedAt.isBefore(endedAt)) {
            throw new BadRequestException("Shift start time must be before end time");
        }

        long minutes = Duration.between(startedAt, endedAt).toMinutes();
        if (minutes <= 0) {
            throw new BadRequestException("Work duration must be greater than zero minutes");
        }

        // Enforce plausible max shift duration (e.g. 24 hours / 1440 minutes)
        if (minutes > 1440) {
            throw new BadRequestException("Work duration cannot exceed 24 hours in a single session");
        }

        // Prevent multiple pending or confirmed work sessions for the same booking
        workSessionRepository.findByBookingIdAndStatus(bookingId, WorkSessionStatus.SUBMITTED).ifPresent(ws -> {
            throw new ConflictException("A work session is already pending employer confirmation for this booking");
        });
        workSessionRepository.findByBookingIdAndStatus(bookingId, WorkSessionStatus.CONFIRMED).ifPresent(ws -> {
            throw new ConflictException("A work session has already been confirmed and awarded for this booking");
        });

        WorkSession workSession = new WorkSession(
            booking,
            startedAt,
            endedAt,
            minutes,
            request.notes()
        );

        WorkSession saved = workSessionRepository.save(workSession);
        return WorkSessionResponse.fromEntity(saved);
    }

    @Transactional
    public WorkSessionResponse confirmWorkSession(Long sessionId, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerEmail));

        WorkSession workSession = workSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Work session not found: " + sessionId));

        User jobEmployer = workSession.getBooking().getJob().getEmployer();
        if (jobEmployer == null || !jobEmployer.getId().equals(employer.getId())) {
            throw new ForbiddenException("You can only confirm work sessions for jobs you posted");
        }

        if (workSession.getStatus() != WorkSessionStatus.SUBMITTED) {
            throw new ConflictException("Work session is not pending confirmation. Current status: " + workSession.getStatus());
        }

        gamificationService.awardPointsForWorkSession(workSession, employer);
        WorkSession saved = workSessionRepository.save(workSession);
        return WorkSessionResponse.fromEntity(saved);
    }

    @Transactional
    public WorkSessionResponse rejectWorkSession(Long sessionId, RejectWorkSessionRequest request, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerEmail));

        WorkSession workSession = workSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Work session not found: " + sessionId));

        User jobEmployer = workSession.getBooking().getJob().getEmployer();
        if (jobEmployer == null || !jobEmployer.getId().equals(employer.getId())) {
            throw new ForbiddenException("You can only reject work sessions for jobs you posted");
        }

        String reason = request != null ? request.reason() : null;
        workSession.reject(employer, reason);
        WorkSession saved = workSessionRepository.save(workSession);
        return WorkSessionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkSessionResponse> getWorkSessionsForBooking(Long bookingId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        boolean isStudent = booking.getStudent().getId().equals(user.getId());
        boolean isEmployer = booking.getJob().getEmployer() != null && booking.getJob().getEmployer().getId().equals(user.getId());

        if (!isStudent && !isEmployer && !user.getRole().name().equals("ADMIN")) {
            throw new ForbiddenException("You do not have access to work sessions for this booking");
        }

        return workSessionRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
            .map(WorkSessionResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkSessionResponse> getPendingWorkSessionsForEmployer(String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerEmail));

        return workSessionRepository.findByEmployerIdAndStatus(employer.getId(), WorkSessionStatus.SUBMITTED).stream()
            .map(WorkSessionResponse::fromEntity)
            .toList();
    }
}
