package com.unishift.unishift_backend.gamification.config;

import com.unishift.unishift_backend.gamification.entity.ElementTier;
import com.unishift.unishift_backend.gamification.repository.ElementTierRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GamificationDataInitializer {

    private record TierSeed(int order, String code, String name, String symbol, int massNumber, int points) {}

    private static final List<TierSeed> SEED_TIERS = List.of(
        new TierSeed(1, "H-1", "Hydrogen", "H", 1, 10),
        new TierSeed(2, "He-4", "Helium", "He", 4, 40),
        new TierSeed(3, "Li-7", "Lithium", "Li", 7, 70),
        new TierSeed(4, "Be-9", "Beryllium", "Be", 9, 90),
        new TierSeed(5, "B-11", "Boron", "B", 11, 110),
        new TierSeed(6, "C-12", "Carbon", "C", 12, 120),
        new TierSeed(7, "C-14", "Carbon", "C", 14, 140),
        new TierSeed(8, "N-15", "Nitrogen", "N", 15, 150),
        new TierSeed(9, "O-16", "Oxygen", "O", 16, 160),
        new TierSeed(10, "F-19", "Fluorine", "F", 19, 190),
        new TierSeed(11, "Ne-20", "Neon", "Ne", 20, 200),
        new TierSeed(12, "Na-23", "Sodium", "Na", 23, 230),
        new TierSeed(13, "Mg-24", "Magnesium", "Mg", 24, 240),
        new TierSeed(14, "Al-27", "Aluminium", "Al", 27, 270),
        new TierSeed(15, "Si-28", "Silicon", "Si", 28, 280),
        new TierSeed(16, "P-31", "Phosphorus", "P", 31, 310),
        new TierSeed(17, "S-32", "Sulfur", "S", 32, 320),
        new TierSeed(18, "Cl-35", "Chlorine", "Cl", 35, 350),
        new TierSeed(19, "Ar-40", "Argon", "Ar", 40, 400)
    );

    @Bean
    public ApplicationRunner initializeElementTiers(ElementTierRepository tierRepository) {
        return args -> {
            for (TierSeed seed : SEED_TIERS) {
                if (tierRepository.findByTierCode(seed.code()).isEmpty()) {
                    ElementTier tier = new ElementTier(
                        seed.code(),
                        seed.name(),
                        seed.symbol(),
                        seed.massNumber(),
                        seed.points(),
                        seed.order(),
                        true
                    );
                    tierRepository.save(tier);
                }
            }
        };
    }
}
