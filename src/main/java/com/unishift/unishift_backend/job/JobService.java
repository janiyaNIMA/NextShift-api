package com.unishift.unishift_backend.job;

import com.unishift.unishift_backend.booking.BookingRepository;
import com.unishift.unishift_backend.booking.BookingStatus;
import com.unishift.unishift_backend.common.exception.BadRequestException;
import com.unishift.unishift_backend.common.exception.ForbiddenException;
import com.unishift.unishift_backend.common.exception.ResourceNotFoundException;
import com.unishift.unishift_backend.job.dto.CreateJobRequest;
import com.unishift.unishift_backend.job.dto.JobResponse;
import com.unishift.unishift_backend.recommendation.JobRecommendationStrategy;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final JobRecommendationStrategy recommendationStrategy;

    public JobService(
        JobRepository jobRepository,
        UserRepository userRepository,
        BookingRepository bookingRepository,
        JobRecommendationStrategy recommendationStrategy
    ) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.recommendationStrategy = recommendationStrategy;
    }

    @Transactional
    public JobResponse createJob(String employerEmail, CreateJobRequest request) {
        User employer = userRepository.findByEmail(employerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerEmail));

        if (!employer.isEmployer()) {
            throw new ForbiddenException("Only employers can create job vacancies");
        }

        if (!employer.isApproved()) {
            throw new ForbiddenException("Employer account has not been approved by an administrator");
        }

        if (!employer.isActive()) {
            throw new ForbiddenException("Employer account is inactive");
        }

        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Job end time must be after start time");
        }

        Job job = new Job(
            employer,
            request.title().trim(),
            request.description().trim(),
            request.category().trim(),
            request.location().trim(),
            request.payAmount(),
            request.payUnit().trim(),
            request.startsAt(),
            request.endsAt(),
            request.capacity()
        );

        Job savedJob = jobRepository.save(job);
        return JobResponse.fromEntity(savedJob, 0);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getPublishedJobs(String studentEmail, String category, String location, Pageable pageable) {
        Long studentId = null;
        if (studentEmail != null) {
            studentId = userRepository.findByEmail(studentEmail).map(User::getId).orElse(null);
        }

        Page<Job> jobPage = recommendationStrategy.getRecommendedJobs(studentId, category, location, pageable);
        return jobPage.map(job -> {
            long confirmedBookings = bookingRepository.countByJobIdAndStatus(job.getId(), BookingStatus.CONFIRMED);
            return JobResponse.fromEntity(job, confirmedBookings);
        });
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findByIdWithEmployer(id)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));

        if (!job.isEligibleForBooking(Instant.now())) {
            throw new ResourceNotFoundException("Job not eligible or no longer available: " + id);
        }

        long confirmedBookings = bookingRepository.countByJobIdAndStatus(job.getId(), BookingStatus.CONFIRMED);
        if (confirmedBookings >= job.getCapacity()) {
            throw new ResourceNotFoundException("Job capacity has been reached: " + id);
        }

        return JobResponse.fromEntity(job, confirmedBookings);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getEmployerJobs(String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerEmail));

        return jobRepository.findByEmployerIdOrderByCreatedAtDesc(employer.getId()).stream()
            .map(job -> {
                long confirmedBookings = bookingRepository.countByJobIdAndStatus(job.getId(), BookingStatus.CONFIRMED);
                return JobResponse.fromEntity(job, confirmedBookings);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public Job getJobOwnedByEmployer(Long jobId, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Employer not found: " + employerEmail));

        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (!job.getEmployer().getId().equals(employer.getId())) {
            throw new ForbiddenException("You are not authorized to view details or bookings for this job vacancy");
        }

        return job;
    }
}
