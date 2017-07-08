/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import com.amazonaws.util.StringUtils;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaStateModelFactory;
import io.klerch.alexa.state.model.AlexaStateObject;
import io.klerch.alexa.state.utils.AlexaStateException;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * As this handler works in the session scope it persists all models to the attributes of an associate Alexa Session object.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 */
public class AlexaSessionStateHandler implements AlexaStateHandler {
    private final Logger log = Logger.getLogger(AlexaSessionStateHandler.class);
    private String userId;
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
     * sets the userId used as a key when storing user-scoped model-state
     * If no userId is provided the handler will use userId coming in with the session
     * Note, the userId from Alexa will change when a user re-enables your skill
     * @param userId userId used as a key when storing user-scoped model-state
     */
    public final void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * Gets the userId used as a key when storing user-scoped model-state. If no custom
     * userId was set for this handler it returns the userId from the underlying Alexa session.
     *  @return userId used as a key when storing user-scoped model-state
     */
    public final String getUserId() {
        return StringUtils.isNullOrEmpty(this.userId) ? session.getUser().getUserId() : this.userId;
    }

    /**
     * sets the userId used as a key when storing user-scoped model-state
     * If no userId is provided the handler will use userId coming in with the session
     * Note, the userId from Alexa will change when a user re-enables your skill
     * @param userId userId used as a key when storing user-scoped model-state
     * @return handler
     */
    public AlexaStateHandler withUserId(final String userId) {
        setUserId(userId);
        return this;
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
    public <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass, final String id) {
        // acts just like a factory for custom models
        return AlexaStateModelFactory.createModel(modelClass, this, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModel(final AlexaStateModel model) throws AlexaStateException {
        Validate.notNull(model, "Model to write must not be null.");
        writeModels(Collections.singletonList(model));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModels(final Collection<? extends AlexaStateModel> models) throws AlexaStateException {
        Validate.notNull(models, "Collection of models to write must not be null.");
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
        writeValue(id, value, AlexaScope.SESSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValue(final String id, final Object value, final AlexaScope scope) throws AlexaStateException {
        Validate.notBlank(id, "Id of single state object must not be blank.");
        Validate.notNull(scope, "Scope of single state object must not be null.");
        writeValue(new AlexaStateObject(id, value, scope));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValue(final AlexaStateObject stateObject) throws AlexaStateException {
        Validate.notNull(stateObject, "State object must not be null.");
        writeValues(Collections.singleton(stateObject));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValues(final Collection<? extends AlexaStateObject> stateObjects) throws AlexaStateException {
        Validate.notNull(stateObjects, "List of state objects to write to persistence store must not be null.");
        stateObjects.stream()
                .filter(o -> AlexaScope.SESSION.includes(o.getScope()))
                .forEach(stateObject -> session.setAttribute(stateObject.getId(), stateObject.getValue()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(final AlexaStateModel model) throws AlexaStateException {
        Validate.notNull(model, "Model to be removed must not be null.");
        removeModels(Collections.singletonList(model));
    }

    @Override
    public void removeModels(Collection<? extends AlexaStateModel> models) throws AlexaStateException {
        Validate.notNull(models, "Collection of models to be removed must not be null.");
        final List<String> ids = models.stream().map(AlexaStateModel::getAttributeKey).collect(Collectors.toList());
        removeValues(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeValue(final String id) throws AlexaStateException {
        removeValues(Collections.singletonList(id));
    }

    @Override
    public void removeValues(final Collection<String> ids) throws AlexaStateException {
        Validate.notNull(ids, "Collection of ids whose values to be removed must not be null.");
        ids.forEach(session::removeAttribute);
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
         final Map<String, TModel> models = readModels(modelClass, Collections.singletonList(id));
         return models.isEmpty() ? Optional.empty() : Optional.of(models.get(id));
    }

    @Override
    public <TModel extends AlexaStateModel> Map<String, TModel> readModels(final Class<TModel> modelClass, final Collection<String> ids) throws AlexaStateException {
        final Map<String, TModel> models = new HashMap<>();
        final Collection<String> resolvedIds = ids.stream().map(id -> TModel.getAttributeKey(modelClass, id)).collect(Collectors.toList());
        final Map<String, Object> raw = new HashMap<>();

        session.getAttributes().forEach((id,val) -> {
            if (resolvedIds.contains(id) && val != null) {
                raw.put(id, val);
            }
        });

        for (final String id : raw.keySet()) {
            final Object o = raw.get(id);

            if (o instanceof Map<?, ?>) {
                final Map<?, ?> childAttributes = (Map<?, ?>) o;
                final TModel model = AlexaStateModelFactory.createModel(modelClass, this, TModel.resolveAttributeKeyToId(modelClass, id));

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
                    models.put(id, model);
                }
            }
            else if (o instanceof String) {
                final TModel model = AlexaStateModelFactory.createModel(modelClass, this, id);
                model.fromJSON((String)o);
                log.debug(String.format("Read state for '%1$s' in session attributes.", model));
                models.put(id, model);
            }
            else {
                // if not a map than expect it to be the model
                // this only happens if a model was added to the session before its json-serialization
                final TModel model = (TModel)o;
                log.debug(String.format("Read state for '%1$s' in session attributes.", model));
                models.put(id, model);
            }
        }

        final Map<String, TModel> modelsToReturn = new HashMap<>();
        // unresolve ids
        models.forEach((id, model) -> {
            modelsToReturn.put(model.getId(), model);
        });
        return modelsToReturn;
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
    public <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass, final String id) throws AlexaStateException {
        return exists(TModel.getAttributeKey(modelClass, id));
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
    public <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass, final String id, final AlexaScope scope) throws AlexaStateException {
        return exists(TModel.getAttributeKey(modelClass, id), scope);
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
        return readValue(id, scope).isPresent();
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
        final Map<String, AlexaStateObject> result = readValues(Collections.singletonList(id), scope);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AlexaStateObject> readValues(final Collection<String> ids) throws AlexaStateException {
        return readValues(ids, AlexaScope.SESSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AlexaStateObject> readValues(final Collection<String> ids, final AlexaScope scope) throws AlexaStateException {
        final Map<String, AlexaScope> idsInScope = new HashMap<>();
        ids.forEach(id -> idsInScope.putIfAbsent(id, scope));
        return readValues(idsInScope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AlexaStateObject> readValues(Map<String, AlexaScope> idsInScope) throws AlexaStateException {
        final Map<String, AlexaStateObject> stateObjectMap = new HashMap<>();
        idsInScope.forEach((k,v) -> {
            // do for session-scoped keys only
            if (existsInSession(k, v)) {
                // read from session and wrap value in state object
                stateObjectMap.putIfAbsent(k, new AlexaStateObject(k, session.getAttribute(k), v));
            }
        });
        return stateObjectMap;
    }

    private boolean existsInSession(final String id, final AlexaScope scope) {
        return AlexaScope.SESSION.includes(scope) && session.getAttributes().containsKey(id);
    }
}
