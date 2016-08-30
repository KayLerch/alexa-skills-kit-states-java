/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaStateModelFactory;
import io.klerch.alexa.state.utils.AlexaStateException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * As this handler works in the session scope it persists all models to the attributes of an associate Alexa Session object.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 */
public class AlexaSessionStateHandler implements AlexaStateHandler {
    final Session session;
    private final String typeWithIdSeparator = ":";

    /**
     * Initializes a handler and applies an Alexa Session to persist models.
     *  @param session The Alexa session of your current skill invocation.
     */
    public AlexaSessionStateHandler(final Session session) {
        this.session = session;
    }

    /**
     * Returns the key used to save the model in the session attributes. This method doesn't take an id
     * thus will return the key for the singleton object of the model.
     * @param modelClass The type of an AlexaStateModel.
     * @param <TModel> The model type derived from AlexaStateModel.
     * @return key used to save the model in the session attributes
     */
    private <TModel extends AlexaStateModel> String getAttributeKey(final Class<TModel> modelClass) {
        return getAttributeKey(modelClass, null);
    }

    /**
     * Returns the Alexa Session object which was given to this handler. This is where state of
     * all models are written to and read from.
     * @return Alexa Session object
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * Returns the key used to save the model in the session attributes. This method takes an id
     * thus will return the key for a specific instance of the model as many of them can exist in your session.
     * @param modelClass The type of an AlexaStateModel.
     * @param id the key for a specific instance of the model
     * @param <TModel> The model type derived from AlexaStateModel.
     * @return key used to save the model in the session attributes
     */
    public <TModel extends AlexaStateModel> String getAttributeKey(final Class<TModel> modelClass, String id) {
        return modelClass.getTypeName() + (id != null && !id.isEmpty() ? typeWithIdSeparator + id : "");
    }

    /**
     * Returns the key used to save the model in the session attributes. This method obtains an id from the given model
     * thus will return the key for a specific instance of the model as many of them can exist in your session. If given
     * model does not provide an id it will return the key for the singleton object of the model.
     * @param model the model to save in the session
     * @return key used to save the model in the session attributes
     */
    public String getAttributeKey(AlexaStateModel model) {
        return getAttributeKey(model.getClass(), model.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass) {
        // acts just like a factory for custom models
        return AlexaStateModelFactory.createModel(modelClass, this, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass, String id) {
        // acts just like a factory for custom models
        return AlexaStateModelFactory.createModel(modelClass, this, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModel(AlexaStateModel model) throws AlexaStateException {
        // scope annotations will be ignored as there is only one context you can saveState attributes
        // thus scope will always be session
        final String attributeKey = getAttributeKey(model);
        session.setAttribute(attributeKey, model.toMap(AlexaScope.SESSION));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(AlexaStateModel model) throws AlexaStateException {
        final String attributeKey = getAttributeKey(model);
        session.removeAttribute(attributeKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(Class<TModel> modelClass) throws AlexaStateException {
        return readModel(modelClass, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(Class<TModel> modelClass, String id) throws AlexaStateException {
        final String attributeKey = getAttributeKey(modelClass, id);
        final Object o = session.getAttribute(attributeKey);
        if (o == null) return Optional.empty();

        if (o instanceof Map<?, ?>) {
            final Map<?, ?> childAttributes = (Map<?, ?>) o;
            final TModel model = AlexaStateModelFactory.createModel(modelClass, this, id);
            if (model != null) {
                for (final Field field : model.getSaveStateFields(AlexaScope.SESSION)) {
                    final String fieldName = field.getName();
                    if (childAttributes.containsKey(fieldName)) {
                        model.set(field, childAttributes.get(field.getName()));
                    }
                }
            }
            return model != null ? Optional.of(model) : Optional.empty();
        }
        else if (o instanceof String) {
            final TModel model = AlexaStateModelFactory.createModel(modelClass, this, id);
            model.fromJSON((String)o);
            return Optional.of(model);
        }
        else {
            // if not a map than expect it to be the model
            // this only happens if a model was added to the session before its json-serialization
            return Optional.of((TModel)o);
        }
    }
}
