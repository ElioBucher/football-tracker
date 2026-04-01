package ch.elio.football;

import ch.elio.football.model.Match;
import ch.elio.football.model.ScorersResponse;
import ch.elio.football.model.StandingsResponse;
import ch.elio.football.model.TeamDetailResponse;
import ch.elio.football.model.TeamResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/football")
public class FootballResource {

    @Inject
    FootballService footballService;

    @GET
    @Path("/live")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Match> getLiveMatches() {
        return footballService.getLiveMatches();
    }

    @GET
    @Path("/today")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Match> getTodayMatches() {
        return footballService.getTodayMatches();
    }

    @GET
    @Path("/standings/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public StandingsResponse getStandings(@PathParam("id") String id) {
        return footballService.getStandings(id);
    }

    @GET
    @Path("/scorers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ScorersResponse getScorers(@PathParam("id") String id) {
        return footballService.getScorers(id);
    }

    @GET
    @Path("/matchday/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Match> getMatchday(
            @PathParam("id") String id,
            @QueryParam("matchday") int matchday
    ) {
        return footballService.getMatchdayMatches(id, matchday);
    }

    @GET
    @Path("/teams/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TeamResponse.Team> searchTeams(@QueryParam("name") String name) {
        return footballService.searchTeams(name);
    }

    @GET
    @Path("/teams/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTeamDetail(@PathParam("id") int id) {
        TeamDetailResponse detail = footballService.getTeamDetail(id);
        if (detail == null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(detail).build();
    }

    @GET
    @Path("/teams/{id}/matches")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Match> getTeamMatches(
            @PathParam("id") int id,
            @QueryParam("status") String status
    ) {
        return footballService.getTeamMatches(id, status);
    }

    @GET
    @Path("/matches/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMatchDetail(@PathParam("id") int id) {
        Match match = footballService.getMatchDetail(id);
        if (match == null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(match).build();
    }
}