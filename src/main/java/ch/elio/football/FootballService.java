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

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class FootballService {

    @ConfigProperty(name = "football.api.key")
    String apiKey;

    private final FootballClient footballClient;
    private final FuzzySearchService fuzzySearch;

    private static final List<String> SEARCH_COMPETITIONS = List.of("PL", "BL1", "SA", "PD", "FL1", "CL");
    private static final long SEARCH_POOL_TTL_MS = 5L * 60 * 1000;
    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Zurich");

    private final Object searchPoolLock = new Object();
    private final AtomicReference<List<TeamResponse.Team>> cachedCompetitionTeams = new AtomicReference<>(List.of());
    private final AtomicLong competitionTeamsCacheUntilMs = new AtomicLong(0L);

    @Inject
    public FootballService(@RestClient FootballClient footballClient, FuzzySearchService fuzzySearch) {
        this.footballClient = footballClient;
        this.fuzzySearch = fuzzySearch;
    }

    public List<Match> getLiveMatches() {
        try {
            MatchResponse response = footballClient.getMatches(apiKey, "LIVE");
            return sortMatchesByKickoff(response != null ? response.matches : List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Match> getTodayMatches() {
        try {
            LocalDate today = LocalDate.now(APP_ZONE);
            String dateFrom = today.toString();
            String dateTo = today.plusDays(1).toString(); // wichtig: dateTo ist exklusiv

            MatchResponse response = footballClient.getMatchesByDate(apiKey, dateFrom, dateTo);
            return sortMatchesByKickoff(response != null ? response.matches : List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    @CacheResult(cacheName = "standings-cache")
    public StandingsResponse getStandings(String competitionId) {
        try {
            StandingsResponse response = footballClient.getStandings(apiKey, competitionId);
            return response != null ? response : new StandingsResponse();
        } catch (Exception e) {
            return new StandingsResponse();
        }
    }

    @CacheResult(cacheName = "scorers-cache")
    public ScorersResponse getScorers(String competitionId) {
        try {
            ScorersResponse response = footballClient.getScorers(apiKey, competitionId, 10);
            return response != null ? response : new ScorersResponse();
        } catch (Exception e) {
            return new ScorersResponse();
        }
    }

    @CacheResult(cacheName = "matchday-cache")
    public List<Match> getMatchdayMatches(String competitionId, int matchday) {
        try {
            MatchResponse response = footballClient.getCompetitionMatches(apiKey, competitionId, matchday, null);
            return sortMatchesByKickoff(response != null ? response.matches : List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<TeamResponse.Team> searchTeams(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String queryNormalized = normalize(query);
        Map<Integer, TeamResponse.Team> apiCandidates = new LinkedHashMap<>();

        try {
            TeamResponse response = footballClient.searchTeams(apiKey, query.trim());
            if (response != null && response.teams != null) {
                for (TeamResponse.Team team : response.teams) {
                    if (team != null) {
                        apiCandidates.putIfAbsent(team.id, team);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        List<ScoredTeam> scoredApi = scoreTeams(apiCandidates.values(), queryNormalized);
        List<TeamResponse.Team> strongApi = scoredApi.stream()
                .filter(t -> t.score >= 0.58)
                .limit(10)
                .map(t -> t.team)
                .toList();

        if (strongApi.size() >= 3) {
            return strongApi;
        }

        Map<Integer, TeamResponse.Team> allCandidates = new LinkedHashMap<>(apiCandidates);
        for (TeamResponse.Team team : loadCompetitionTeamsPoolCached()) {
            allCandidates.putIfAbsent(team.id, team);
        }

        List<ScoredTeam> scoredAll = scoreTeams(allCandidates.values(), queryNormalized);
        if (scoredAll.isEmpty()) {
            return List.of();
        }

        double bestScore = scoredAll.get(0).score;
        double dynamicThreshold = Math.max(0.62, bestScore - 0.20);

        return scoredAll.stream()
                .filter(t -> t.score >= dynamicThreshold)
                .limit(10)
                .map(t -> t.team)
                .toList();
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
            MatchResponse response = footballClient.getTeamMatches(apiKey, teamId, status, 10);
            return sortMatchesByKickoff(response != null ? response.matches : List.of());
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

    private List<ScoredTeam> scoreTeams(Iterable<TeamResponse.Team> teams, String queryNormalized) {
        List<ScoredTeam> scored = new ArrayList<>();
        for (TeamResponse.Team team : teams) {
            double score = relevanceScore(queryNormalized, team);
            if (score > 0) {
                scored.add(new ScoredTeam(team, score));
            }
        }

        scored.sort(Comparator.comparingDouble((ScoredTeam t) -> t.score).reversed());
        return scored;
    }

    private double relevanceScore(String queryNormalized, TeamResponse.Team team) {
        if (team == null) {
            return 0.0;
        }

        String nameNormalized = normalize(team.name);
        String shortNormalized = normalize(team.shortName);

        if (nameNormalized.equals(queryNormalized) || shortNormalized.equals(queryNormalized)) {
            return 1.0;
        }

        if (startsWithToken(nameNormalized, queryNormalized) || startsWithToken(shortNormalized, queryNormalized)) {
            return 0.97;
        }

        if (nameNormalized.contains(queryNormalized) || shortNormalized.contains(queryNormalized)) {
            return 0.92;
        }

        double fuzzyName = similarityWithLengthPenalty(queryNormalized, nameNormalized);
        double fuzzyShort = similarityWithLengthPenalty(queryNormalized, shortNormalized);
        return Math.max(fuzzyName, fuzzyShort);
    }

    private double similarityWithLengthPenalty(String query, String target) {
        if (target == null || target.isBlank()) {
            return 0.0;
        }

        double base = fuzzySearch.similarity(query, target);
        int min = Math.min(query.length(), target.length());
        int max = Math.max(query.length(), target.length());
        double ratio = max == 0 ? 0.0 : (double) min / max;
        return base * (0.55 + (0.45 * ratio));
    }

    private boolean startsWithToken(String target, String query) {
        if (target == null || target.isBlank() || query == null || query.isBlank()) {
            return false;
        }

        for (String token : target.split("[^a-z0-9]+")) {
            if (!token.isEmpty() && token.startsWith(query)) {
                return true;
            }
        }
        return false;
    }

    private List<TeamResponse.Team> loadCompetitionTeamsPoolCached() {
        long now = System.currentTimeMillis();
        List<TeamResponse.Team> snapshot = cachedCompetitionTeams.get();
        if (now < competitionTeamsCacheUntilMs.get() && !snapshot.isEmpty()) {
            return snapshot;
        }

        synchronized (searchPoolLock) {
            now = System.currentTimeMillis();
            snapshot = cachedCompetitionTeams.get();
            if (now < competitionTeamsCacheUntilMs.get() && !snapshot.isEmpty()) {
                return snapshot;
            }

            List<TeamResponse.Team> refreshed = loadCompetitionTeamsPool();
            if (!refreshed.isEmpty()) {
                cachedCompetitionTeams.set(List.copyOf(refreshed));
                competitionTeamsCacheUntilMs.set(now + SEARCH_POOL_TTL_MS);
            }
            return cachedCompetitionTeams.get();
        }
    }

    private List<TeamResponse.Team> loadCompetitionTeamsPool() {
        Map<Integer, TeamResponse.Team> uniqueTeams = new LinkedHashMap<>();

        for (String competitionId : SEARCH_COMPETITIONS) {
            try {
                TeamResponse response = footballClient.getCompetitionTeams(apiKey, competitionId);
                if (response == null || response.teams == null) {
                    continue;
                }

                for (TeamResponse.Team team : response.teams) {
                    if (team != null) {
                        uniqueTeams.putIfAbsent(team.id, team);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return new ArrayList<>(uniqueTeams.values());
    }

    private List<Match> sortMatchesByKickoff(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        return matches.stream()
                .filter(m -> m != null)
                .sorted(Comparator.comparing(m -> m.utcDate == null ? "" : m.utcDate))
                .toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String lowered = value.toLowerCase(Locale.ROOT).trim();
        String umlautSafe = lowered
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");

        return Normalizer.normalize(umlautSafe, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }

    private static final class ScoredTeam {
        private final TeamResponse.Team team;
        private final double score;

        private ScoredTeam(TeamResponse.Team team, double score) {
            this.team = team;
            this.score = score;
        }
    }
}