package com.unishift.unishift_backend.recommendation;

import com.unishift.unishift_backend.booking.BookingStatus;
import com.unishift.unishift_backend.job.Job;
import com.unishift.unishift_backend.job.JobRepository;
import com.unishift.unishift_backend.job.JobStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Primary
public class AllPublishedJobsStrategy implements JobRecommendationStrategy {

    private final JobRepository jobRepository;

    public AllPublishedJobsStrategy(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public Page<Job> getRecommendedJobs(Long studentId, String category, String location, Pageable pageable) {
        String normalizedCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String normalizedLocation = (location != null && !location.isBlank()) ? location.trim() : null;

        return jobRepository.findEligibleJobs(
            JobStatus.PUBLISHED,
            Instant.now(),
            normalizedCategory,
            normalizedLocation,
            BookingStatus.CONFIRMED,
            pageable
        );
    }
}
