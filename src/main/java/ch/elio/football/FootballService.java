package ch.elio.football;

import ch.elio.football.model.Match;
import ch.elio.football.model.MatchResponse;
import ch.elio.football.model.StandingsResponse;
import ch.elio.football.model.TeamResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.List;

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
        return footballClient.getMatches(apiKey, "LIVE").matches;
    }

    public List<Match> getTodayMatches() {
        return footballClient.getMatches(apiKey, "SCHEDULED").matches;
    }

    public StandingsResponse getStandings(String competitionId) {
        return footballClient.getStandings(apiKey, competitionId);
    }

    public List<TeamResponse.Team> searchTeams(String query) {
        try {
            TeamResponse response = footballClient.searchTeams(apiKey, query);
            if (response.teams != null && !response.teams.isEmpty()) {
                return response.teams;
            }
        } catch (Exception e) {
            // Falls API nichts findet, weiter zur Fuzzy-Suche
        }

        TeamResponse allTeams = footballClient.searchTeams(apiKey, "");
        if (allTeams.teams == null) return List.of();

        return allTeams.teams.stream()
                .filter(t -> {
                    double sim = fuzzySearch.similarity(query, t.name);
                    double simShort = t.shortName != null
                            ? fuzzySearch.similarity(query, t.shortName)
                            : 0.0;
                    return Math.max(sim, simShort) >= 0.4;
                })
                .sorted((a, b) -> {
                    double simA = Math.max(
                            fuzzySearch.similarity(query, a.name),
                            a.shortName != null ? fuzzySearch.similarity(query, a.shortName) : 0.0
                    );
                    double simB = Math.max(
                            fuzzySearch.similarity(query, b.name),
                            b.shortName != null ? fuzzySearch.similarity(query, b.shortName) : 0.0
                    );
                    return Double.compare(simB, simA);
                })
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Match> getTeamMatches(int teamId, String status) {
        return footballClient.getTeamMatches(apiKey, teamId, status, 10).matches;
    }
}