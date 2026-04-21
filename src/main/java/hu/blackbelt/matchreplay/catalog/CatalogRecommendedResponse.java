package hu.blackbelt.matchreplay.catalog;

import java.util.List;

public record CatalogRecommendedResponse(
        String whenOpponentPlays,
        String fromPosition,
        List<CatalogResponseEntry> responses) {}
