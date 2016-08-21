/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.model.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.lerch.alexa.state.model.AlexaScope;
import me.lerch.alexa.state.model.AlexaStateModel;
import me.lerch.alexa.state.utils.AlexaStateException;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * This custom JSON serializer considers all fields tagged with the AlexaStateSave annotation in a given scope.
 * The scope is read out from an attribute which comes with the provider and is set by another Serializer extending
 * the abstract class.
 */
public abstract class AlexaStateSerializer extends JsonSerializer<AlexaStateModel> {
    final String scopeAttributeKey = "AlexaScope";

    @Override
    public void serialize(AlexaStateModel alexaStateModel, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        {
            final AlexaScope scope = (AlexaScope)serializerProvider.getAttribute(scopeAttributeKey);
            jsonGenerator.writeObjectField("id", alexaStateModel.getId());
            // look for statesave fields in model if not already whole class is statesave
            for (final Field field : alexaStateModel.getSaveStateFields(scope)) {
                final String fieldName = field.getName();
                Object fieldValue;
                try {
                    fieldValue = alexaStateModel.get(field);
                } catch (AlexaStateException e) {
                    // wrap custom exception with expected IOException
                    throw new IOException("Could not get value of field " + fieldName, e);
                }
                jsonGenerator.writeObjectField(fieldName, fieldValue);
            }
        }
        jsonGenerator.writeEndObject();
    }
}
