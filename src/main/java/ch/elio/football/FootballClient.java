package ch.elio.football;

import ch.elio.football.model.MatchResponse;
import ch.elio.football.model.ScorersResponse;
import ch.elio.football.model.StandingsResponse;
import ch.elio.football.model.TeamDetailResponse;
import ch.elio.football.model.TeamResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ch.elio.football.FootballClient")
@Path("/v4")
public interface FootballClient {

    @GET
    @Path("/matches")
    MatchResponse getMatches(
            @HeaderParam("X-Auth-Token") String token,
            @QueryParam("status") String status
    );

    @GET
    @Path("/competitions/{id}/standings")
    StandingsResponse getStandings(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") String competitionId
    );

    @GET
    @Path("/competitions/{id}/scorers")
    ScorersResponse getScorers(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") String competitionId,
            @QueryParam("limit") int limit
    );

    @GET
    @Path("/competitions/{id}/matches")
    MatchResponse getCompetitionMatches(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") String competitionId,
            @QueryParam("matchday") int matchday,
            @QueryParam("status") String status
    );

    @GET
    @Path("/teams")
    TeamResponse searchTeams(
            @HeaderParam("X-Auth-Token") String token,
            @QueryParam("name") String name
    );

    @GET
    @Path("/teams/{id}")
    TeamDetailResponse getTeamDetail(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") int teamId
    );

    @GET
    @Path("/teams/{id}/matches")
    MatchResponse getTeamMatches(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") int teamId,
            @QueryParam("status") String status,
            @QueryParam("limit") int limit
    );

    @GET
    @Path("/matches/{id}")
    ch.elio.football.model.Match getMatch(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") int matchId
    );

    @GET
    @Path("/competitions/{id}/teams")
    TeamResponse getCompetitionTeams(
            @HeaderParam("X-Auth-Token") String token,
            @PathParam("id") String competitionId
    );
}