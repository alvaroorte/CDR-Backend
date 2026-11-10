package bo.cdr.app.config;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.ArrayList;
import java.util.List;

public class IntegerListDeserializer extends ValueDeserializer<List<Integer>> {

    @Override
    public List<Integer> deserialize(JsonParser parser, DeserializationContext context) {

        if (!parser.isExpectedStartArrayToken()) {
            throw MismatchedInputException.from(
                    parser,
                    List.class,
                    "Se espera una lista de números enteros"
            );
        }

        List<Integer> result = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_ARRAY) {

            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                throw MismatchedInputException.from(
                        parser,
                        Integer.class,
                        "Todos los elementos deben ser números enteros"
                );
            }

            result.add(parser.getIntValue());
        }

        return result;
    }
}
