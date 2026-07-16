package cz.jbenak.ncrm_backend.model.dto.store;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-16
 * DTO carrying the binary content of a catalogue item image together with its media type.
 */
public record ItemImageDto(
        byte[] content,
        String contentType
) {
}
