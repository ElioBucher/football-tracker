package ch.elio.football.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamResponse {
    public List<Team> teams;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        public int id;
        public String name;
        public String shortName;
        public String crest;
        public String venue;
        public String founded;
        public String clubColors;
    }
}