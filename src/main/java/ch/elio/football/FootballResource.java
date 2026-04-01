package ch.elio.football;

import ch.elio.football.model.Match;
import ch.elio.football.model.StandingsResponse;
import ch.elio.football.model.TeamResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
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
    @Path("/teams/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TeamResponse.Team> searchTeams(@QueryParam("name") String name) {
        return footballService.searchTeams(name);
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
}