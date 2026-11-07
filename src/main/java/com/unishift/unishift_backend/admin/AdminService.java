package com.unishift.unishift_backend.admin;

import com.unishift.unishift_backend.auth.dto.UserResponse;
import com.unishift.unishift_backend.booking.BookingRepository;
import com.unishift.unishift_backend.booking.BookingStatus;
import com.unishift.unishift_backend.common.exception.BadRequestException;
import com.unishift.unishift_backend.common.exception.ResourceNotFoundException;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.job.JobStatus;
import com.unishift.unishift_backend.job.dto.JobResponse;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import com.unishift.unishift_backend.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final BookingRepository bookingRepository;

    public AdminService(
        UserRepository userRepository,
        JobRepository jobRepository,
        BookingRepository bookingRepository
    ) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getPendingEmployers() {
        return userRepository.findByRoleAndApprovedFalse(UserRole.EMPLOYER).stream()
            .map(UserResponse::fromEntity)
            .toList();
    }

    @Transactional
    public UserResponse approveEmployer(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (!user.isEmployer()) {
            throw new BadRequestException("User is not an employer: " + id);
        }

        user.approve();
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getPendingJobs() {
        return jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.PENDING).stream()
            .map(job -> {
                long confirmed = bookingRepository.countByJobIdAndStatus(job.getId(), BookingStatus.CONFIRMED);
                return JobResponse.fromEntity(job, confirmed);
            })
            .toList();
    }

    @Transactional
    public JobResponse approveJob(Long id) {
        Job job = jobRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));

        if (job.getStatus() != JobStatus.PENDING) {
            throw new BadRequestException("Only pending jobs can be approved. Current status: " + job.getStatus());
        }

        job.publish();
        Job updated = jobRepository.save(job);
        long confirmed = bookingRepository.countByJobIdAndStatus(updated.getId(), BookingStatus.CONFIRMED);
        return JobResponse.fromEntity(updated, confirmed);
    }
}
