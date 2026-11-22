package com.unishift.unishift_backend.gamification.service;

import com.unishift.unishift_backend.common.exception.ConflictException;
import com.unishift.unishift_backend.common.exception.ForbiddenException;
import com.unishift.unishift_backend.common.exception.ResourceNotFoundException;
import com.unishift.unishift_backend.gamification.dto.ElementTierResponse;
import com.unishift.unishift_backend.gamification.dto.GamificationSummary;
import com.unishift.unishift_backend.gamification.dto.PointTransactionResponse;
import com.unishift.unishift_backend.gamification.entity.ElementTier;
import com.unishift.unishift_backend.gamification.entity.PointTransaction;
import com.unishift.unishift_backend.gamification.entity.StudentGamification;
import com.unishift.unishift_backend.gamification.entity.WorkSession;
import com.unishift.unishift_backend.gamification.repository.ElementTierRepository;
import com.unishift.unishift_backend.gamification.repository.PointTransactionRepository;
import com.unishift.unishift_backend.gamification.repository.StudentGamificationRepository;
import com.unishift.unishift_backend.gamification.strategy.ElementTierResolver;
import com.unishift.unishift_backend.gamification.strategy.WorkPointCalculator;
import com.unishift.unishift_backend.user.User;
import com.unishift.unishift_backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GamificationService {

    private final StudentGamificationRepository gamificationRepository;
    private final ElementTierRepository tierRepository;
    private final PointTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WorkPointCalculator pointCalculator;
    private final ElementTierResolver tierResolver;

    public GamificationService(
        StudentGamificationRepository gamificationRepository,
        ElementTierRepository tierRepository,
        PointTransactionRepository transactionRepository,
        UserRepository userRepository,
        WorkPointCalculator pointCalculator,
        ElementTierResolver tierResolver
    ) {
        this.gamificationRepository = gamificationRepository;
        this.tierRepository = tierRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.pointCalculator = pointCalculator;
        this.tierResolver = tierResolver;
    }

    @Transactional
    public StudentGamification getOrCreateGamification(User student) {
        return gamificationRepository.findByStudentId(student.getId())
            .orElseGet(() -> gamificationRepository.save(new StudentGamification(student)));
    }

    @Transactional(readOnly = true)
    public GamificationSummary getStudentSummary(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        if (!student.isStudent()) {
            throw new ForbiddenException("Only registered students have a gamification profile");
        }

        StudentGamification gamification = gamificationRepository.findByStudentId(student.getId())
            .orElseGet(() -> new StudentGamification(student));

        int totalPoints = gamification.getTotalPoints();
        ElementTier currentTier = gamification.getCurrentTier();

        // If tier was null but points qualify (or vice versa), reconcile against database
        ElementTier resolvedCurrent = tierResolver.resolveTier(totalPoints);
        ElementTier effectiveCurrent = resolvedCurrent != null ? resolvedCurrent : currentTier;

        // Resolve next tier
        ElementTier nextTier = tierRepository
            .findFirstByActiveTrueAndRequiredPointsGreaterThanOrderByRequiredPointsAsc(totalPoints)
            .orElse(null);

        int pointsToNextTier = 0;
        double progressPercent = 0.0;

        if (nextTier != null) {
            pointsToNextTier = Math.max(0, nextTier.getRequiredPoints() - totalPoints);
            int currentThreshold = effectiveCurrent != null ? effectiveCurrent.getRequiredPoints() : 0;
            int nextThreshold = nextTier.getRequiredPoints();
            int span = nextThreshold - currentThreshold;
            if (span > 0) {
                progressPercent = ((double) (totalPoints - currentThreshold) / span) * 100.0;
                progressPercent = Math.min(100.0, Math.max(0.0, Math.round(progressPercent * 10.0) / 10.0));
            }
        } else if (effectiveCurrent != null) {
            // Reached highest tier
            pointsToNextTier = 0;
            progressPercent = 100.0;
        }

        return new GamificationSummary(
            totalPoints,
            gamification.getTotalVerifiedMinutes(),
            ElementTierResponse.fromEntity(effectiveCurrent),
            ElementTierResponse.fromEntity(nextTier),
            pointsToNextTier,
            progressPercent
        );
    }

    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> getStudentTransactions(String studentEmail, Pageable pageable) {
        User student = userRepository.findByEmail(studentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentEmail));

        if (!student.isStudent()) {
            throw new ForbiddenException("Only students have access to point transactions");
        }

        return transactionRepository.findByStudentIdOrderByCreatedAtDesc(student.getId(), pageable)
            .map(PointTransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ElementTierResponse> getAllActiveTiers() {
        return tierRepository.findByActiveTrueOrderByRequiredPointsAsc().stream()
            .map(ElementTierResponse::fromEntity)
            .toList();
    }

    /**
     * Atomically award points for an employer-verified work session.
     * Guaranteed idempotent and safe against double counting.
     */
    @Transactional
    public int awardPointsForWorkSession(WorkSession workSession, User verifiedBy) {
        User student = workSession.getBooking().getStudent();
        StudentGamification gamification = getOrCreateGamification(student);

        String idempotencyKey = "ws-award-" + workSession.getId();
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new ConflictException("Points have already been awarded for work session " + workSession.getId());
        }

        // Calculate newly eligible points including partial-hour carryover
        long previousMinutes = gamification.getTotalVerifiedMinutes();
        long newMinutes = previousMinutes + workSession.getVerifiedMinutes();

        int previousPoints = pointCalculator.calculatePoints(previousMinutes);
        int newPoints = pointCalculator.calculatePoints(newMinutes);
        int pointsDelta = newPoints - previousPoints;

        // Perform work session transition
        workSession.confirm(verifiedBy);

        // Record point transaction ledger entry if points were earned
        if (pointsDelta > 0) {
            PointTransaction tx = new PointTransaction(
                student,
                workSession,
                pointsDelta,
                "VERIFIED_WORK_HOURS",
                idempotencyKey
            );
            transactionRepository.save(tx);
        }

        // Resolve and update current tier
        int updatedTotalPoints = gamification.getTotalPoints() + pointsDelta;
        ElementTier newTier = tierResolver.resolveTier(updatedTotalPoints);
        gamification.addVerifiedWork(pointsDelta, workSession.getVerifiedMinutes(), newTier);
        gamificationRepository.save(gamification);

        return pointsDelta;
    }
}
