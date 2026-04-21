package hu.blackbelt.matchreplay.catalog;

import java.util.List;

public record CatalogMove(String key, String label, String category, List<String> tags) {}
