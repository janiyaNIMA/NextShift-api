package com.unishift.unishift_backend.gamification.controller;

import com.unishift.unishift_backend.gamification.dto.ElementTierResponse;
import com.unishift.unishift_backend.gamification.dto.GamificationSummary;
import com.unishift.unishift_backend.gamification.dto.PointTransactionResponse;
import com.unishift.unishift_backend.gamification.service.GamificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me/gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping
    public ResponseEntity<GamificationSummary> getMyGamification(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        GamificationSummary summary = gamificationService.getStudentSummary(userDetails.getUsername());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<PointTransactionResponse>> getMyTransactions(
        @AuthenticationPrincipal UserDetails userDetails,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PointTransactionResponse> transactions = gamificationService.getStudentTransactions(
            userDetails.getUsername(),
            pageable
        );
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<ElementTierResponse>> getActiveTiers() {
        List<ElementTierResponse> tiers = gamificationService.getAllActiveTiers();
        return ResponseEntity.ok(tiers);
    }
}
