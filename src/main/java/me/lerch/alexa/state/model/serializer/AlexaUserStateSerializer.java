/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.model.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import me.lerch.alexa.state.model.AlexaScope;
import me.lerch.alexa.state.model.AlexaStateModel;

import java.io.IOException;

/**
 * The serializer considers all fields in a model tagged with AlexaStateSave and scoped with USER or a scope
 * which includes USER.
 */
public class AlexaUserStateSerializer extends AlexaStateSerializer {
    @Override
    public void serialize(AlexaStateModel alexaStateModel, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        serializerProvider.setAttribute(this.scopeAttributeKey, AlexaScope.USER);
        super.serialize(alexaStateModel, jsonGenerator, serializerProvider);
    }
}
