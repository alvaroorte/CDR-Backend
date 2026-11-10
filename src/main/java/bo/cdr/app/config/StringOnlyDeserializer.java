package bo.cdr.app.config;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

public class StringOnlyDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {

        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            throw MismatchedInputException.from(
                    parser,
                    String.class,
                    parser.currentName() + " debe ser un string"
            );
        }

        return parser.getValueAsString().trim();
    }
}
