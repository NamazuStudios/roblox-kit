package dev.getelements.robloxkit.element.rest;

import dev.getelements.elements.sdk.model.ErrorResponse;
import dev.getelements.elements.sdk.model.exception.BaseException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import static dev.getelements.elements.sdk.model.exception.ErrorCode.UNKNOWN;

public class RobloxExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(final Exception ex) {
        final var error = errorResponseFromException(ex);
        return builderFromException(ex).entity(error).build();
    }

    public static ErrorResponse errorResponseFromException(final Exception ex) {

        final var error = new ErrorResponse();
        error.setMessage(ex.getMessage());

        if (ex instanceof BaseException bex) {
            error.setMessage(bex.getMessage());
            error.setCode(bex.getCode().toString());
        } else {
            error.setCode(UNKNOWN.toString());
        }

        return error;

    }

    public static Response.ResponseBuilder builderFromException(final Exception ex) {
        if (ex instanceof BaseException bex) {
            return switch (bex.getCode()) {
                case NOT_FOUND -> Response.status(Response.Status.NOT_FOUND);
                case INVALID_DATA -> Response.status(Response.Status.BAD_REQUEST);
                case FORBIDDEN -> Response.status(Response.Status.FORBIDDEN);
                default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            };
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
