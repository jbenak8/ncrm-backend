package cz.jbenak.ncrm_backend.model.dto.store;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemImageDto other)) {
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
        return "ItemImageDto[content=" + (content == null ? "null" : content.length + " byte(s)")
                + ", contentType=" + contentType + "]";
    }
}
