package com.unishift.unishift_backend.admin;

import com.unishift.unishift_backend.auth.dto.UserResponse;
import com.unishift.unishift_backend.job.dto.JobResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/employers/pending")
    public ResponseEntity<List<UserResponse>> getPendingEmployers() {
        List<UserResponse> employers = adminService.getPendingEmployers();
        return ResponseEntity.ok(employers);
    }

    @PatchMapping("/employers/{id}/approve")
    public ResponseEntity<UserResponse> approveEmployer(@PathVariable Long id) {
        UserResponse response = adminService.approveEmployer(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/pending")
    public ResponseEntity<List<JobResponse>> getPendingJobs() {
        List<JobResponse> jobs = adminService.getPendingJobs();
        return ResponseEntity.ok(jobs);
    }

    @PatchMapping("/jobs/{id}/approve")
    public ResponseEntity<JobResponse> approveJob(@PathVariable Long id) {
        JobResponse response = adminService.approveJob(id);
        return ResponseEntity.ok(response);
    }
}
