package ch.elio;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class GreetingResourceTest {
    @Test
    void testTeamSearchEndpoint() {
        given()
                .queryParam("name", "arsenal")
                .when().get("/api/football/teams/search")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }

}