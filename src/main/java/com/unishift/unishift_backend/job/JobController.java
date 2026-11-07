package com.unishift.unishift_backend.job;

import com.unishift.unishift_backend.job.dto.CreateJobRequest;
import com.unishift.unishift_backend.job.dto.JobResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<Page<JobResponse>> getPublishedJobs(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String location,
        @AuthenticationPrincipal UserDetails userDetails,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        Page<JobResponse> jobs = jobService.getPublishedJobs(userEmail, category, location, pageable);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        JobResponse job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody CreateJobRequest request
    ) {
        JobResponse response = jobService.createJob(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
