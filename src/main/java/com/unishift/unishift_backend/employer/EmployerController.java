package com.unishift.unishift_backend.employer;

import com.unishift.unishift_backend.booking.BookingService;
import com.unishift.unishift_backend.booking.dto.BookingResponse;
import com.unishift.unishift_backend.job.JobService;
import com.unishift.unishift_backend.job.dto.JobResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employer")
public class EmployerController {

    private final JobService jobService;
    private final BookingService bookingService;

    public EmployerController(JobService jobService, BookingService bookingService) {
        this.jobService = jobService;
        this.bookingService = bookingService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getMyJobs(@AuthenticationPrincipal UserDetails userDetails) {
        List<JobResponse> jobs = jobService.getEmployerJobs(userDetails.getUsername());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/jobs/{id}/bookings")
    public ResponseEntity<List<BookingResponse>> getJobBookings(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<BookingResponse> bookings = bookingService.getBookingsForJob(id, userDetails.getUsername());
        return ResponseEntity.ok(bookings);
    }
}
