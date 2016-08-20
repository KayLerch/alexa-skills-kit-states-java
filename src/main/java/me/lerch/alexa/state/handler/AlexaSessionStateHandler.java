/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import me.lerch.alexa.state.model.AlexaScope;
import me.lerch.alexa.state.model.AlexaStateModel;
import me.lerch.alexa.state.model.AlexaStateModelFactory;
import me.lerch.alexa.state.utils.AlexaStateErrorException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * As this handler works in the session scope it persists all models to the attributes of an associate Alexa Session object.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 */
public class AlexaSessionStateHandler implements AlexaStateHandler {
    final Session session;

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
    <TModel extends AlexaStateModel> String getAttributeKey(final Class<TModel> modelClass) {
        return getAttributeKey(modelClass, null);
    }

    /**
     * Returns the key used to save the model in the session attributes. This method takes an id
     * thus will return the key for a specific instance of the model as many of them can exist in your session.
     * @param modelClass The type of an AlexaStateModel.
     * @param id the key for a specific instance of the model
     * @param <TModel> The model type derived from AlexaStateModel.
     * @return key used to save the model in the session attributes
     */
    <TModel extends AlexaStateModel> String getAttributeKey(final Class<TModel> modelClass, String id) {
        return modelClass.getTypeName() + (id != null && !id.isEmpty() ? ":" + id : "");
    }

    /**
     * Returns the key used to save the model in the session attributes. This method obtains an id from the given model
     * thus will return the key for a specific instance of the model as many of them can exist in your session. If given
     * model does not provide an id it will return the key for the singleton object of the model.
     * @param model the model to save in the session
     * @return key used to save the model in the session attributes
     */
    String getAttributeKey(AlexaStateModel model) {
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
    public void writeModel(AlexaStateModel model) throws AlexaStateErrorException {
        // scope annotations will be ignored as there is only one context you can saveState attributes
        // thus scope will always be session
        final String attributeKey = getAttributeKey(model);
        session.setAttribute(attributeKey, model.toMap(AlexaScope.SESSION));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(AlexaStateModel model) {
        final String attributeKey = getAttributeKey(model);
        session.removeAttribute(attributeKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(Class<TModel> modelClass) throws AlexaStateErrorException {
        final String attributeKey = getAttributeKey(modelClass);
        // look for any key which starts with the model class name as id is unknown
        Optional<String> firstKey = session.getAttributes().keySet().stream().filter(key -> key.startsWith(attributeKey)).findFirst();
        if (firstKey.isPresent()) {
            final String firstKeyForSure = firstKey.get();
            // extract id from key
            final String id = firstKeyForSure.substring(firstKeyForSure.indexOf(":") + 1);
            // read first model from session
            return readModel(modelClass, id);
        }
        // no attribute found which has a prefix like modelClass
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(Class<TModel> modelClass, String id) throws AlexaStateErrorException {
        final String attributeKey = getAttributeKey(modelClass, id);
        final Object o = session.getAttribute(attributeKey);
        if (o == null) return Optional.empty();
        // expect o to be a map of key-values
        if (o instanceof Map<?, ?>) {
            final Map<?, ?> childAttributes = (Map<?, ?>) o;
            final TModel model = AlexaStateModelFactory.createModel(modelClass, this, id);
            if (model != null) {
                for (final Field field : model.getSaveStateFields()) {
                    final String fieldName = field.getName();
                    if (childAttributes.containsKey(fieldName)) {
                        model.set(field, childAttributes.get(field.getName()));
                    }
                }
            }
            return model != null ? Optional.of(model) : Optional.empty();
        }
        else {
            // if not a map than expect it to be the model
            // this only happens if a model was added to the session before its json-serialization
            return Optional.of((TModel)o);
        }
    }
}
