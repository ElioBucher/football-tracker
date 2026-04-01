package ch.elio.football.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamDetailResponse {
    public int id;
    public String name;
    public String shortName;
    public String crest;
    public String venue;
    public String founded;
    public String clubColors;
    public String website;
    public List<Player> squad;
    public Coach coach;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Player {
        public int id;
        public String name;
        public String position;
        public String nationality;
        public String dateOfBirth;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coach {
        public String name;
        public String nationality;
    }
}