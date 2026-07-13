package cz.jbenak.ncrm_backend.model.dto.company;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-13
 * DTO carrying the binary content of a company logo together with its media type.
 */
public record CompanyLogoDto(
        byte[] content,
        String contentType
) {
}
