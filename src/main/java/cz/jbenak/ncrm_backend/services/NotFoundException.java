package cz.jbenak.ncrm_backend.services;

import java.util.UUID;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-11
 * Exception thrown when a requested entity is not found. Translated to HTTP 404 by the global exception handler.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entity, UUID id) {
        super(entity + " with id " + id + " not found");
    }
}
