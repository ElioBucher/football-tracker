package ch.elio.football;

import ch.elio.football.model.Match;
import ch.elio.football.model.MatchResponse;
import ch.elio.football.model.ScorersResponse;
import ch.elio.football.model.StandingsResponse;
import ch.elio.football.model.TeamDetailResponse;
import ch.elio.football.model.TeamResponse;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FootballService {

    @ConfigProperty(name = "football.api.key")
    String apiKey;

    @Inject
    @RestClient
    FootballClient footballClient;

    @Inject
    FuzzySearchService fuzzySearch;

    public List<Match> getLiveMatches() {
        try {
            return footballClient.getMatches(apiKey, "IN_PLAY").matches;
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Match> getTodayMatches() {
        try {
            MatchResponse scheduled = footballClient.getMatches(apiKey, "SCHEDULED");
            MatchResponse timed = footballClient.getMatches(apiKey, "TIMED");
            List<Match> result = new ArrayList<>();
            if (scheduled.matches != null) result.addAll(scheduled.matches);
            if (timed.matches != null) result.addAll(timed.matches);
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    @CacheResult(cacheName = "standings-cache")
    public StandingsResponse getStandings(String competitionId) {
        try {
            return footballClient.getStandings(apiKey, competitionId);
        } catch (Exception e) {
            return new StandingsResponse();
        }
    }

    @CacheResult(cacheName = "scorers-cache")
    public ScorersResponse getScorers(String competitionId) {
        try {
            return footballClient.getScorers(apiKey, competitionId, 10);
        } catch (Exception e) {
            return new ScorersResponse();
        }
    }

    @CacheResult(cacheName = "matchday-cache")
    public List<Match> getMatchdayMatches(String competitionId, int matchday) {
        try {
            return footballClient.getCompetitionMatches(apiKey, competitionId, matchday, null).matches;
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<TeamResponse.Team> searchTeams(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String queryTrimmed = query.trim();
        String queryLower = queryTrimmed.toLowerCase();

        // Schritt 1: API-seitige Suche mit dem Suchbegriff — liefert bereits gefilterte Treffer
        List<TeamResponse.Team> apiResults = new ArrayList<>();
        try {
            TeamResponse response = footballClient.searchTeams(apiKey, queryTrimmed);
            if (response != null && response.teams != null) {
                apiResults.addAll(response.teams);
            }
        } catch (Exception e) {
            // API-Fehler: mit leerer Liste weiterarbeiten
        }

        // Schritt 2: Falls API keine oder wenige Ergebnisse zurückgibt (z.B. bei Tippfehlern),
        // Fallback auf Fuzzy-Suche über alle verfügbaren Teams
        if (apiResults.size() < 3) {
            try {
                TeamResponse allTeams = footballClient.searchTeams(apiKey, "");
                if (allTeams != null && allTeams.teams != null) {
                    List<TeamResponse.Team> fuzzyResults = allTeams.teams.stream()
                            .filter(t -> {
                                if (t.name == null) return false;
                                String nameLower = t.name.toLowerCase();
                                String shortLower = t.shortName != null ? t.shortName.toLowerCase() : "";
                                double sim = Math.max(
                                        fuzzySearch.similarity(queryLower, nameLower),
                                        fuzzySearch.similarity(queryLower, shortLower)
                                );
                                return sim >= 0.35;
                            })
                            .collect(Collectors.toList());

                    // Zusammenführen: API-Ergebnisse zuerst, Fuzzy-Treffer hinzufügen wenn noch nicht enthalten
                    for (TeamResponse.Team fuzzyTeam : fuzzyResults) {
                        boolean alreadyPresent = apiResults.stream().anyMatch(t -> t.id == fuzzyTeam.id);
                        if (!alreadyPresent) {
                            apiResults.add(fuzzyTeam);
                        }
                    }
                }
            } catch (Exception e) {
                // Fuzzy-Fallback fehlgeschlagen — mit API-Ergebnissen weiterfahren
            }
        }

        // Schritt 3: Sortieren nach Relevanz (beste Übereinstimmung zuerst)
        return apiResults.stream()
                .sorted(Comparator.comparingDouble((TeamResponse.Team t) -> {
                    String nameLower = t.name != null ? t.name.toLowerCase() : "";
                    String shortLower = t.shortName != null ? t.shortName.toLowerCase() : "";
                    return -Math.max(
                            fuzzySearch.similarity(queryLower, nameLower),
                            fuzzySearch.similarity(queryLower, shortLower)
                    );
                }))
                .limit(10)
                .collect(Collectors.toList());
    }

    public TeamDetailResponse getTeamDetail(int teamId) {
        try {
            return footballClient.getTeamDetail(apiKey, teamId);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Match> getTeamMatches(int teamId, String status) {
        try {
            return footballClient.getTeamMatches(apiKey, teamId, status, 10).matches;
        } catch (Exception e) {
            return List.of();
        }
    }

    public Match getMatchDetail(int matchId) {
        try {
            return footballClient.getMatch(apiKey, matchId);
        } catch (Exception e) {
            return null;
        }
    }
}