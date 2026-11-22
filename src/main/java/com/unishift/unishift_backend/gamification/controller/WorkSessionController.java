package com.unishift.unishift_backend.gamification.controller;

import com.unishift.unishift_backend.gamification.dto.RejectWorkSessionRequest;
import com.unishift.unishift_backend.gamification.dto.SubmitWorkSessionRequest;
import com.unishift.unishift_backend.gamification.dto.WorkSessionResponse;
import com.unishift.unishift_backend.gamification.service.WorkSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WorkSessionController {

    private final WorkSessionService workSessionService;

    public WorkSessionController(WorkSessionService workSessionService) {
        this.workSessionService = workSessionService;
    }

    @PostMapping("/api/v1/bookings/{bookingId}/work-sessions")
    public ResponseEntity<WorkSessionResponse> submitWorkSession(
        @PathVariable Long bookingId,
        @Valid @RequestBody SubmitWorkSessionRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        WorkSessionResponse response = workSessionService.submitWorkSession(
            bookingId,
            request,
            userDetails.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/bookings/{bookingId}/work-sessions")
    public ResponseEntity<List<WorkSessionResponse>> getWorkSessionsForBooking(
        @PathVariable Long bookingId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<WorkSessionResponse> response = workSessionService.getWorkSessionsForBooking(
            bookingId,
            userDetails.getUsername()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/employer/work-sessions/{sessionId}/confirm")
    public ResponseEntity<WorkSessionResponse> confirmWorkSession(
        @PathVariable Long sessionId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        WorkSessionResponse response = workSessionService.confirmWorkSession(
            sessionId,
            userDetails.getUsername()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/v1/employer/work-sessions/{sessionId}/reject")
    public ResponseEntity<WorkSessionResponse> rejectWorkSession(
        @PathVariable Long sessionId,
        @RequestBody(required = false) RejectWorkSessionRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        WorkSessionResponse response = workSessionService.rejectWorkSession(
            sessionId,
            request,
            userDetails.getUsername()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/employer/work-sessions/pending")
    public ResponseEntity<List<WorkSessionResponse>> getPendingWorkSessions(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<WorkSessionResponse> response = workSessionService.getPendingWorkSessionsForEmployer(
            userDetails.getUsername()
        );
        return ResponseEntity.ok(response);
    }
}
