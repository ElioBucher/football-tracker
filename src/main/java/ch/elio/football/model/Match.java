package ch.elio.football.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Match {
    public int id;
    public Team homeTeam;
    public Team awayTeam;
    public Score score;
    public String status;
    public String utcDate;
    public int matchday;
    public String lastUpdated;
    public List<Goal> goals;
    public Competition competition;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        public int id;
        public String name;
        public String crest;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Score {
        public String winner;
        public FullTime fullTime;
        public HalfTime halfTime;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class FullTime {
            public Integer home;
            public Integer away;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HalfTime {
            public Integer home;
            public Integer away;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Goal {
        public Integer minute;
        public String type;
        public Scorer scorer;
        public Team team;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Scorer {
            public String name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {
        public String name;
        public String emblem;
    }
}