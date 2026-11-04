package fit.biejk.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads a text field of a partial update ({@code PATCH}) that can be cleared. The field has three states:
 * <ul>
 *   <li>not in the JSON: {@code null}, the value is kept;</li>
 *   <li>{@code null} in the JSON: {@link Optional#empty()}, the value is cleared;</li>
 *   <li>a text in the JSON: {@code Optional.of(text)}, the value is replaced.</li>
 * </ul>
 * Jackson reads a missing field as {@code Optional.empty()}, which could not be told from an explicit {@code null},
 * so this class returns {@code null} for a missing field.
 */
public final class ClearableTextDeserializer extends StdDeserializer<Optional<String>> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the deserializer.
     */
    public ClearableTextDeserializer() {
        super(Optional.class);
    }

    @Override
    public Optional<String> deserialize(final JsonParser parser, final DeserializationContext context)
            throws IOException {
        return Optional.of(StringDeserializer.instance.deserialize(parser, context));
    }

    @Override
    public Optional<String> getNullValue(final DeserializationContext context) {
        return Optional.empty();
    }

    @Override
    public Object getAbsentValue(final DeserializationContext context) {
        return null;
    }
}
