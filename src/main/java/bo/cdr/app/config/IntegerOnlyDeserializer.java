package bo.cdr.app.config;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

public class IntegerOnlyDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser jsonParser, DeserializationContext context) {

        if (!JsonToken.VALUE_NUMBER_INT.equals(jsonParser.currentToken())) {

            String fieldName = jsonParser.currentName();

            throw InvalidFormatException.from(
                    jsonParser,
                    fieldName + " se espera un número entero",
                    jsonParser.getText(),
                    Integer.class
            );
        }

        return jsonParser.getIntValue();
    }
}
