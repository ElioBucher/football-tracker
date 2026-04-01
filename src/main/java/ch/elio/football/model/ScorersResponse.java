package ch.elio.football.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScorersResponse {
    public List<ScorerEntry> scorers;
    public Competition competition;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScorerEntry {
        public Player player;
        public Team team;
        public int goals;
        public int assists;
        public int penalties;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Player {
            public String name;
            public String nationality;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Team {
            public String name;
            public String crest;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {
        public String name;
    }
}