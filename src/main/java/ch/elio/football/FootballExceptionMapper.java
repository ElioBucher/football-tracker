package ch.elio.football;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FootballExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        int status = 500;
        String message = "Interner Serverfehler";

        if (exception instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            message = switch (status) {
                case 404 -> "Ressource nicht gefunden";
                case 429 -> "API-Rate-Limit erreicht, bitte kurz warten";
                case 400 -> "Ungültige Anfrage";
                case 403 -> "Kein Zugriff – API-Key prüfen";
                default -> "Serverfehler";
            };
        }

        String body = "{\"error\": \"" + message + "\", \"status\": " + status + "}";
        return Response.status(status)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}