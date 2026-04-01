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
        // Direkt alle Teams laden und lokal filtern
        TeamResponse allTeams;
        try {
            allTeams = footballClient.searchTeams(apiKey, "");
        } catch (Exception e) {
            return List.of();
        }

        if (allTeams.teams == null) return List.of();

        String queryLower = query.toLowerCase().trim();

        return allTeams.teams.stream()
                .filter(t -> {
                    String nameLower = t.name.toLowerCase();
                    String shortLower = t.shortName != null ? t.shortName.toLowerCase() : "";

                    if (nameLower.startsWith(queryLower) || shortLower.startsWith(queryLower)) return true;
                    if (nameLower.contains(queryLower) || shortLower.contains(queryLower)) return true;

                    double sim = fuzzySearch.similarity(queryLower, nameLower);
                    double simShort = fuzzySearch.similarity(queryLower, shortLower);
                    return Math.max(sim, simShort) >= 0.3;
                })
                .sorted((a, b) -> {
                    double simA = Math.max(
                            fuzzySearch.similarity(queryLower, a.name.toLowerCase()),
                            a.shortName != null ? fuzzySearch.similarity(queryLower, a.shortName.toLowerCase()) : 0.0
                    );
                    double simB = Math.max(
                            fuzzySearch.similarity(queryLower, b.name.toLowerCase()),
                            b.shortName != null ? fuzzySearch.similarity(queryLower, b.shortName.toLowerCase()) : 0.0
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