package hu.blackbelt.matchreplay.catalog;

public record CatalogTransition(String fromPosition, String move, String toPosition, int difficulty) {}
