package ch.elio.football.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingsResponse {
    public Competition competition;
    public List<Standing> standings;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Standing {
        public String type;
        public List<TableEntry> table;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TableEntry {
        public int position;
        public Team team;
        public int playedGames;
        public int won;
        public int draw;
        public int lost;
        public int points;
        public int goalsFor;
        public int goalsAgainst;
        public int goalDifference;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        public int id;
        public String name;
        public String crest;
    }
}