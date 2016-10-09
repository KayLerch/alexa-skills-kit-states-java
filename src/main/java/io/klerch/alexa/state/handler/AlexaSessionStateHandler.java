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
import io.klerch.alexa.state.model.AlexaStateObject;
import io.klerch.alexa.state.utils.AlexaStateException;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * As this handler works in the session scope it persists all models to the attributes of an associate Alexa Session object.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 */
public class AlexaSessionStateHandler implements AlexaStateHandler {
    private final Logger log = Logger.getLogger(AlexaSessionStateHandler.class);
    final Session session;

    /**
     * Initializes a handler and applies an Alexa Session to persist models.
     *  @param session The Alexa session of your current skill invocation.
     */
    public AlexaSessionStateHandler(final Session session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession() {
        return this.session;
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
    public void writeModel(final AlexaStateModel model) throws AlexaStateException {
        writeModels(Collections.singletonList(model));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModels(final Collection<AlexaStateModel> models) throws AlexaStateException {
        models.forEach(model -> {
            try {
                // scope annotations will be ignored as there is only one context you can saveState attributes
                // thus scope will always be session
                session.setAttribute(model.getAttributeKey(), model.toMap(AlexaScope.SESSION));
                log.debug(String.format("Wrote state to session attributes for '%1$s'.", model));
            } catch (final AlexaStateException e) {
                log.error(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValue(final String id, final Object value) throws AlexaStateException {
        writeValue(id, value, AlexaScope.USER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValue(final String id, final Object value, final AlexaScope scope) throws AlexaStateException {
        if (AlexaScope.SESSION.includes(scope)) {
            session.setAttribute(id, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValue(final AlexaStateObject stateObject) throws AlexaStateException {
        writeValues(Collections.singleton(stateObject));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValues(final Collection<AlexaStateObject> stateObjects) throws AlexaStateException {
        Validate.notNull(stateObjects, "List of state objects to write to persistence store must not be null.");
        stateObjects.forEach(stateObject -> {
            try {
                writeValue(stateObject.getKey(), stateObject.getValue(), stateObject.getScope());
            } catch (AlexaStateException e) {
                log.error(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(final AlexaStateModel model) throws AlexaStateException {
        removeValue(model.getAttributeKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeValue(final String id) throws AlexaStateException {
        session.removeAttribute(id);
        log.debug(String.format("Removed state from session attributes for '%1$s'.", id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass) throws AlexaStateException {
        return readModel(modelClass, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass, final String id) throws AlexaStateException {
        final Object o = session.getAttribute(TModel.getAttributeKey(modelClass, id));
        if (o == null) {
            log.info(String.format("Could not find state for '%1$s' in session attributes.", TModel.getAttributeKey(modelClass, id)));
            return Optional.empty();
        }

        if (o instanceof Map<?, ?>) {
            final Map<?, ?> childAttributes = (Map<?, ?>) o;
            final TModel model = AlexaStateModelFactory.createModel(modelClass, this, id);

            if (model != null) {
                model.getSaveStateFields(AlexaScope.SESSION).parallelStream()
                        .filter(field -> childAttributes.containsKey(field.getName()))
                        .forEach(field -> {
                            try {
                                model.set(field, childAttributes.get(field.getName()));
                            } catch (AlexaStateException e) {
                                log.error(String.format("Could not set value for '%1$s' of model '%2$s'", field.getName(), model), e);
                            }
                        });
                log.debug(String.format("Read state for '%1$s' in session attributes.", model));
            }
            return model != null ? Optional.of(model) : Optional.empty();
        }
        else if (o instanceof String) {
            final TModel model = AlexaStateModelFactory.createModel(modelClass, this, id);
            model.fromJSON((String)o);
            log.debug(String.format("Read state for '%1$s' in session attributes.", model));
            return Optional.of(model);
        }
        else {
            // if not a map than expect it to be the model
            // this only happens if a model was added to the session before its json-serialization
            final TModel model = (TModel)o;
            log.debug(String.format("Read state for '%1$s' in session attributes.", model));
            return Optional.of(model);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass) throws AlexaStateException {
        return exists(TModel.getAttributeKey(modelClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass, final AlexaScope scope) throws AlexaStateException {
        return exists(TModel.getAttributeKey(modelClass), scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final String id) throws AlexaStateException {
        return exists(id, AlexaScope.SESSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final String id, final AlexaScope scope) throws AlexaStateException {
        return AlexaScope.SESSION.includes(scope) && session.getAttributes().containsKey(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AlexaStateObject> readValue(final String id) throws AlexaStateException {
        return readValue(id, AlexaScope.SESSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<AlexaStateObject> readValue(final String id, final AlexaScope scope) throws AlexaStateException {
        return exists(id, scope) ? Optional.of(new AlexaStateObject(id, session.getAttribute(id), scope)) : Optional.empty();
    }
}
