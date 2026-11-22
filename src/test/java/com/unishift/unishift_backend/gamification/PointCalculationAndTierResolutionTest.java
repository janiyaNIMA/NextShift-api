package com.unishift.unishift_backend.gamification;

import com.unishift.unishift_backend.gamification.entity.ElementTier;
import com.unishift.unishift_backend.gamification.repository.ElementTierRepository;
import com.unishift.unishift_backend.gamification.strategy.ThresholdElementTierResolver;
import com.unishift.unishift_backend.gamification.strategy.WholeHourWorkPointCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class PointCalculationAndTierResolutionTest {

    private WholeHourWorkPointCalculator calculator;
    private ElementTierRepository tierRepository;
    private ThresholdElementTierResolver resolver;

    @BeforeEach
    void setUp() {
        calculator = new WholeHourWorkPointCalculator();
        tierRepository = Mockito.mock(ElementTierRepository.class);
        resolver = new ThresholdElementTierResolver(tierRepository);
    }

    @Test
    void testPointCalculationRules() {
        // 0 minutes -> 0 points
        assertEquals(0, calculator.calculatePoints(0));

        // 59 minutes -> 0 points (do not round up)
        assertEquals(0, calculator.calculatePoints(59));

        // 60 minutes -> 1 point
        assertEquals(1, calculator.calculatePoints(60));

        // 90 minutes -> 1 point (never round partial hours up)
        assertEquals(1, calculator.calculatePoints(90));

        // 120 minutes -> 2 points
        assertEquals(2, calculator.calculatePoints(120));

        // Negative duration -> 0 points
        assertEquals(0, calculator.calculatePoints(-15));
    }

    @Test
    void testTierResolutionRules() {
        ElementTier h1 = new ElementTier("H-1", "Hydrogen", "H", 1, 10, 1, true);
        ElementTier he4 = new ElementTier("He-4", "Helium", "He", 4, 40, 2, true);
        ElementTier c12 = new ElementTier("C-12", "Carbon", "C", 12, 120, 6, true);
        ElementTier c14 = new ElementTier("C-14", "Carbon", "C", 14, 140, 7, true);
        ElementTier n15 = new ElementTier("N-15", "Nitrogen", "N", 15, 150, 8, true);
        ElementTier ar40 = new ElementTier("Ar-40", "Argon", "Ar", 40, 400, 19, true);

        // 0 and 9 points -> unranked (null)
        assertNull(resolver.resolveTier(0));
        assertNull(resolver.resolveTier(9));

        // 10 -> H-1
        when(tierRepository.findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(10))
            .thenReturn(Optional.of(h1));
        assertEquals("H-1", resolver.resolveTier(10).getTierCode());

        // 40 -> He-4
        when(tierRepository.findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(40))
            .thenReturn(Optional.of(he4));
        assertEquals("He-4", resolver.resolveTier(40).getTierCode());

        // 139 -> C-12
        when(tierRepository.findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(139))
            .thenReturn(Optional.of(c12));
        assertEquals("C-12", resolver.resolveTier(139).getTierCode());

        // 140 -> C-14
        when(tierRepository.findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(140))
            .thenReturn(Optional.of(c14));
        assertEquals("C-14", resolver.resolveTier(140).getTierCode());

        // 150 -> N-15
        when(tierRepository.findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(150))
            .thenReturn(Optional.of(n15));
        assertEquals("N-15", resolver.resolveTier(150).getTierCode());

        // Above 400 -> Ar-40 (highest configured tier)
        when(tierRepository.findFirstByActiveTrueAndRequiredPointsLessThanEqualOrderByRequiredPointsDesc(450))
            .thenReturn(Optional.of(ar40));
        assertEquals("Ar-40", resolver.resolveTier(450).getTierCode());
    }
}
