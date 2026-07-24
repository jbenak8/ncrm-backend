package cz.jbenak.ncrm_backend.model.dto.company;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Jan Benák
 * @version 1.0
 * @since 2026-07-24
 * DTO carrying the binary content of a company stamp together with its media type.
 */
public record CompanyStampDto(
        byte[] content,
        String contentType
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompanyStampDto other)) {
            return false;
        }
        return Arrays.equals(content, other.content) && Objects.equals(contentType, other.contentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(content), contentType);
    }

    @Override
    public String toString() {
        return "CompanyStampDto[content=" + (content == null ? "null" : content.length + " byte(s)")
                + ", contentType=" + contentType + "]";
    }
}
