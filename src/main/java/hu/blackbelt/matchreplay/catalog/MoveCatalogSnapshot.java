package hu.blackbelt.matchreplay.catalog;

import java.util.List;
import java.util.Optional;

public record MoveCatalogSnapshot(
        int version,
        List<CatalogPosition> positions,
        List<CatalogMove> moves,
        List<CatalogTransition> transitions,
        List<CatalogRecommendedResponse> recommendedResponses) {

    public static MoveCatalogSnapshot empty() {
        return new MoveCatalogSnapshot(0, List.of(), List.of(), List.of(), List.of());
    }

    public Optional<CatalogMove> findMove(String key) {
        if (key == null) return Optional.empty();
        return moves.stream().filter(m -> m.key().equals(key)).findFirst();
    }

    public boolean isMoveKnown(String key) {
        return findMove(key).isPresent();
    }

    public List<CatalogTransition> transitionsFrom(String positionKey) {
        if (positionKey == null || positionKey.isBlank()) return List.of();
        return transitions.stream()
                .filter(t -> t.fromPosition().equals(positionKey))
                .toList();
    }

    /** Filters moves by label, key, category, or tags. Empty query returns all moves. */
    public List<CatalogMove> search(String query) {
        if (query == null || query.isBlank()) return moves;
        String lower = query.toLowerCase();
        return moves.stream()
                .filter(m -> m.label().toLowerCase().contains(lower)
                        || m.key().toLowerCase().contains(lower)
                        || (m.category() != null && m.category().toLowerCase().contains(lower))
                        || m.tags().stream().anyMatch(t -> t.toLowerCase().contains(lower)))
                .toList();
    }
}
