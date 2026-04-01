package ch.elio.football.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Match {
    public Team homeTeam;
    public Team awayTeam;
    public Score score;
    public String status;
    public String utcDate;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        public FullTime fullTime;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FullTime {
            public Integer home;
            public Integer away;
        }
    }
}