package com.unishift.unishift_backend.recommendation;

import com.unishift.unishift_backend.job.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobRecommendationStrategy {

    Page<Job> getRecommendedJobs(Long studentId, String category, String location, Pageable pageable);
}
