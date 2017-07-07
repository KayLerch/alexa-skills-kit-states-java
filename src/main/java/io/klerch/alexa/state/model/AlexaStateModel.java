/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.klerch.alexa.state.handler.AlexaStateHandler;
import io.klerch.alexa.state.model.serializer.AlexaAppStateSerializer;
import io.klerch.alexa.state.model.serializer.AlexaSessionStateSerializer;
import io.klerch.alexa.state.model.serializer.AlexaStateSerializer;
import io.klerch.alexa.state.model.serializer.AlexaUserStateSerializer;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.state.utils.ConversionUtils;
import io.klerch.alexa.state.utils.ReflectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This abstract class turns your POJO model into a model compatible to the AlexaStateHandler.
 */
public abstract class AlexaStateModel {
    @AlexaStateIgnore
    private final Logger log = Logger.getLogger(AlexaStateModel.class);
    @AlexaStateIgnore
    private String __internalId;
    @AlexaStateIgnore
    private AlexaStateHandler __handler;
    @AlexaStateIgnore
    private final String validIdPattern = "[a-zA-Z0-9_\\-]+";
    @AlexaStateIgnore
    private Boolean hasSessionScopedFields;
    @AlexaStateIgnore
    private Boolean hasApplicationScopedFields;
    @AlexaStateIgnore
    private Boolean hasUserScopedFields;
    @AlexaStateIgnore
    private static final String AttributeKeySeparator = ":";

    /**
     * Returns the key used to save the model in the session attributes. This method doesn't take an id
     * thus will return the key for the singleton object of the model.
     * @param modelClass The type of an AlexaStateModel.
     * @param <TModel> The model type derived from AlexaStateModel.
     * @return key used to save the model in the session attributes
     */
    public static <TModel extends AlexaStateModel> String getAttributeKey(final Class<TModel> modelClass) {
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
    public static <TModel extends AlexaStateModel> String getAttributeKey(final Class<TModel> modelClass, final String id) {
        return modelClass.getTypeName() + (id != null && !id.isEmpty() ? AttributeKeySeparator + id : "");
    }

    /**
     * Returns the key used to save the model in the session attributes. This method obtains an id from this model
     * thus will return the key for a specific instance of the model as many of them can exist in your session. If this
     * model does not provide an id it will return the key for the singleton object of the model.
     * @return key used to save the model in the session attributes
     */
    public String getAttributeKey() {
        return getAttributeKey(this.getClass(), __internalId);
    }

    /**
     * Sets an id for this model instance. The id should be an unique identifier within a model-type within an AlexaScope.
     * This is how you can persist multiple instances per model per user or per application or per session. An id has
     * some regulations to its allowed characters (a-zA-Z0-9_-.)
     * @param id an identifier for this model instance.
     */
    public void setId(final String id) {
        if (id != null && !id.isEmpty()) {
            Validate.matchesPattern(id, validIdPattern, "Chosen model Id contains illegal characters. Ensure your Id matches the following pattern: " + validIdPattern);
            this.__internalId = id;
        }
    }

    /**
     * Gets id for this model instance. The id is an unique identifier within a model-type within an AlexaScope.
     * This is how you can a specific instance for a model in either scope of user or application or session.
     * @return identifier which can also be null for singleton model instances
     */
    public String getId() {
        return this.__internalId;
    }

    /**
     * Sets the AlexaStateHandler which takes care of this model when it {@link #saveState()}, {@link #removeState()}.
     * A state handler usually is dedicated to a persistence store which stores the AlexaStateSave-tagged fields of this model
     * @param handler a state handler implementation
     */
    public void setHandler(final AlexaStateHandler handler) {
        this.__handler = handler;
    }

    /**
     * Sets the AlexaStateHandler which takes care of this model when it {@link #saveState()}, {@link #removeState()}.
     * A state handler usually is dedicated to a persistence store which stores the AlexaStateSave-tagged fields of this model
     * @param handler a state handler implementation
     * @return abstract representation of your model
     */
    public AlexaStateModel withHandler(final AlexaStateHandler handler) {
        setHandler(handler); return this; }

    /**
     * Gets the AlexaStateHandler which takes care of this model when it {@link #saveState()}, {@link #removeState()}.
     * A state handler usually is dedicated to a persistence store which stores the AlexaStateSave-tagged fields of this model
     * @return the state handler
     */
    public AlexaStateHandler getHandler() {
        return this.__handler;
    }

    /**
     * Asks the state handler associated with this model to save all AlexaStateSave-tagged fields in the persistence store.
     * This method will raise an exception if no handler is set for this model.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    public void saveState() throws AlexaStateException {
        Validate.notNull(this.__handler, "Save state is not allowed for this model as it needs an AlexaSessionHandler. Assign a handler to this object or use AlexaStateModelFactory.");
        this.__handler.writeModel(this);
    }

    /**
     * Asks the state handler associated with this model to remove the model from the persistence store. It means you won't be
     * able to access the model with its id anymore. There is no impact on the runtime instance. Its values remain.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    public void removeState() throws AlexaStateException {
        Validate.notNull(this.__handler, "Remove state is not allowed for this model as it needs an AlexaSessionHandler. Assign a handler to this object or use AlexaStateModelFactory.");
        this.__handler.removeModel(this);
    }

    /**
     * Creates a new AlexaStateModel
     * @param modelClass type of the model.
     * @param <T> type of the model.
     * @return builder to use for completing the object creation
     */
    static <T extends AlexaStateModel> AlexaModelBuilder create(final Class<T> modelClass) {
        return new AlexaModelBuilder(modelClass);
    }

    /**
     * Generic getter for all the fields in this model. A getter method following the naming convention
     * get[Fieldname] is called. Otherwise the field is read out directly.
     * @param field The field whose value you desire.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return Value of the given field.
     */
    public Object get(final Field field) throws AlexaStateException {
        final String fieldName = field.getName();
        // prefer getting value from getter over direct read from field
        try {
            // look for a getter for this field
            final Method getter = ReflectionUtils.getGetter(this, fieldName);
            // if there is a getter go for it otherwise read value from field directly
            field.setAccessible(true);
            return getter != null ? getter.invoke(this) : field.get(this);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            final String error = String.format("Could not access field '%1$s' of model '%2$s' for reading. Ensure there's a public getter for this field.", fieldName, this);
            log.error(error, e);
            throw AlexaStateException.create(error).withCause(e).withModel(this).build();
        }
    }

    /**
     * Generic setter for all the fields in this model. A setter method following the naming convention
     * set[Fieldname] is used to write the given value. Otherwise the field is written directly. If there's a problem
     * with accessing the field this method returns false
     * @param field The field whose value you want to set.
     * @param value New value for the given field
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    public void set(final Field field, final Object value) throws AlexaStateException {
        final String fieldName = field.getName();
        // prefer setting value with setter over direct value assignment to field
        try {
            // look for a setter for this field
            final Method setter = ReflectionUtils.getSetter(this, fieldName);
            if (setter != null) {
                // invoke setter
                setter.invoke(this, value);
            }
            else {
                // or direct value assignment
                field.setAccessible(true);
                field.set(this, value);
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            final String error = String.format("Could not access field '%1$s' of model '%2$s' for writing. Ensure there's a public setter for this field.", fieldName, this);
            log.error(error, e);
            throw AlexaStateException.create(error).withCause(e).withModel(this).build();
        }
    }

    /**
     * Expects a json-string which contains keys with values. Any key which is equal a fieldname of this model
     * will result in its value being written to the field of this model. Those fields needs to have the AlexaStateSave-annotation
     * otherwise they will not be considered even though there name match with a key in the given json.
     * @param json A json with key-value-pairs where the keys likely equal some of the AlexaStateSave-tagged fields in this model.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return True, if json-keys matched with AlexaStateSave-tagged fields.
     */
    public boolean fromJSON(final String json) throws AlexaStateException {
        // by default take over everything from json to fields that map in this model
        // session-scope covers it all
        return fromJSON(json, AlexaScope.SESSION);
    }

    /**
     * Expects a json-string which contains keys with values. Any key which is equal a fieldname of this model
     * will result in its value being written to the field of this model. Those fields need to have the AlexaStateSave-annotation
     * with the given scope otherwise they will not be considered even though there name match with a key in the given json.
     * @param json A json with key-value-pairs where the keys likely equal some of the AlexaStateSave-tagged with given scope fields in this model.
     * @param scope The scope a AlexaStateSave-annotated field must have to be considered for value assignment
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return True, if json-keys matched with AlexaStateSave-tagged fields with given scope.
     */
    public boolean fromJSON(final String json, final AlexaScope scope) throws AlexaStateException {
        Boolean modelChanged = false;

        try {
            final ObjectMapper om = new ObjectMapper();
            om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            final Object model = new ObjectMapper().readValue(json, this.getClass());
            for (final Field field : getSaveStateFields(scope)) {
                field.setAccessible(true);
                this.set(field, field.get(model));
                modelChanged = true;
            }
        } catch (final IOException | IllegalAccessException e) {
            final String error = String.format("Error while deserializing model of '%1$s' as Json.", this);
            log.error(error, e);
            throw AlexaStateException.create(error).withCause(e).withModel(this).build();
        }
        return modelChanged;
    }

    /**
     * Returns a json with key-value-pairs - one for each AlexaStateSave-annotated field in this model configured to be valid
     * in the given scope
     * @param scope The scope a AlexaStateSave-annotated field must have or be part of to be considered in the returned json
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return A json-string with key-value-pairs - one for each AlexaStateSave-annotated field in this model configured to be valid
     */
    public String toJSON(final AlexaScope scope) throws AlexaStateException {
        // for each scope there is a custom json serializer so initialize the one which corresponds to the given scope
        final AlexaStateSerializer serializer = AlexaScope.APPLICATION.equals(scope) ?
                new AlexaAppStateSerializer() : AlexaScope.USER.equals(scope) ?
                new AlexaUserStateSerializer() : new AlexaSessionStateSerializer();
        // associate a mapper with the serializer
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(this.getClass(), serializer);
        mapper.registerModule(module);
        try {
            // serialize model which only contains those fields tagged with the given scope
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            final String error = String.format("Error while serializing model of '%1$s' as Json.", this);
            log.error(error, e);
            throw AlexaStateException.create(error).withCause(e).withModel(this).build();
        }
    }

    /**
     * Returns a map with key-value-pairs - one for each AlexaStateSave-annotated field in this model configured to be valid
     * in the given scope
     * @param scope The scope a AlexaStateSave-annotated field must have or be part of to be considered in the returned map
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return A map with key-value-pairs - one for each AlexaStateSave-annotated field in this model configured to be valid
     */
    public Map<String, Object> toMap(final AlexaScope scope) throws AlexaStateException {
        // for each scope there is a custom json serializer so initialize the one which corresponds to the given scope
        return ConversionUtils.mapJson(toJSON(scope));
    }

    /**
     * It returns if any AlexaStateSave field is in the model scoped in SESSION
     * @return True, if there are any AlexaStateSave fields in the model scoped in SESSION
     */
    public Boolean hasSessionScopedField() {
        hasSessionScopedFields = (hasSessionScopedFields != null) ? hasSessionScopedFields :
                hasFieldInScope(AlexaScope.SESSION);
        return hasSessionScopedFields;
    }

    /**
     * It returns if any AlexaStateSave field is in the model scoped in USER
     * @return True, if there are any AlexaStateSave fields in the model scoped in USER
     */
    public Boolean hasUserScopedField() {
        hasUserScopedFields = (hasUserScopedFields != null) ? hasUserScopedFields :
                hasFieldInScope(AlexaScope.USER);
        return hasUserScopedFields;
    }

    /**
     * It returns if any AlexaStateSave field is in the model scoped in APPLICATION
     * @return True, if there are any AlexaStateSave fields in the model scoped in APPLICATION
     */
    public Boolean hasApplicationScopedField() {
        hasApplicationScopedFields = (hasApplicationScopedFields != null) ? hasApplicationScopedFields :
                hasFieldInScope(AlexaScope.APPLICATION);
        return hasApplicationScopedFields;
    }

    /**
     * Gives you all the fields of this model which are annotated with AlexaStateSave
     * @return list of all the fields of this model which are annotated with AlexaStateSave
     */
    public List<Field> getSaveStateFields() {
        return Arrays.stream(this.getClass().getDeclaredFields()).filter(this::isStateSave).collect(Collectors.toList());
    }

    /**
     * Gives you all the fields of this model which are annotated with AlexaStateSave and whose scope is set to a scope
     * which at least in included in the given scope.
     * @param scope Defines the scope which is used to filter all the AlexaStateSave-annotated fields
     * @return list of all the fields of this model which are annotated with AlexaStateSave and whose scope is set to a scope
     * which at least in included in the given scope.
     */
    public List<Field> getSaveStateFields(final AlexaScope scope) {
        return Arrays.stream(this.getClass().getDeclaredFields()).filter(field -> isStateSave(field, scope)).collect(Collectors.toList());
    }

    /**
     * Checks, if the given field is tagged with AlexaStateSave
     * @param field the field you want to check for the AlexaStateSave-annotation
     * @return True, if the given field has the AlexaStateSave-annotation
     */
    private boolean isStateSave(final Field field) {
        // either field itself is annotated as statesave or whole class is
        // however, StateIgnore prevends field of being statesave
        return !field.isAnnotationPresent(AlexaStateIgnore.class) &&
                (field.isAnnotationPresent(AlexaStateSave.class) ||
                        this.getClass().isAnnotationPresent(AlexaStateSave.class));
    }

    /**
     * Checks, if the given field is tagged with AlexaStateSave and whose scope is set to a scope which at least
     * is included in the given scope.
     * @param field the field you want to check for the AlexaStateSave-annotation
     * @param scope the scope which at least must be included in the scope of the field
     * @return True, if the field has the AlexaStateSave-annotation and given scope (or an included scope)
     */
    private boolean isStateSave(final Field field, final AlexaScope scope) {
        // either field itself is tagged as state-save in given scope or whole class is statesave in the given scope
        // however, StateIgnore in given scope prevents field of being statesave
        return ((!field.isAnnotationPresent(AlexaStateIgnore.class) || !scope.isIn(field.getAnnotation(AlexaStateIgnore.class).Scope())) &&
                ((field.isAnnotationPresent(AlexaStateSave.class) && scope.includes(field.getAnnotation(AlexaStateSave.class).Scope()) ||
                        (this.getClass().isAnnotationPresent(AlexaStateSave.class) && scope.includes(this.getClass().getAnnotation(AlexaStateSave.class).Scope())))));
    }

    private boolean hasFieldInScope(final AlexaScope scope) {
        return !getSaveStateFields(scope).isEmpty();
    }

    static final class AlexaModelBuilder {
        private final Logger log = Logger.getLogger(AlexaModelBuilder.class);

        private String __internalId;
        private AlexaStateHandler __handler;
        private Class<?> modelClass;

        <T extends AlexaStateModel> AlexaModelBuilder(Class<T> modelClass) {
            this.modelClass = modelClass;
        }

        /**
         * Sets an id for this model instance. The id should be an unique identifier within a model-type within an AlexaScope.
         * This is how you can persist multiple instances per model per user or per application or per session. An id has
         * some regulations to its allowed characters (a-zA-Z0-9_-.)
         * @param id an identifier for this model instance.
         * @return builder
         */
        public AlexaModelBuilder withId(final String id) {
            this.__internalId = id;
            return this;
        }

        /**
         * Sets the AlexaStateHandler which takes care of this model when it {@link #saveState()}, {@link #removeState()}.
         * A state handler usually is dedicated to a persistence store which stores the AlexaStateSave-tagged fields of this model
         * @param handler a state handler implementation
         * @return builder
         */
        public AlexaModelBuilder withHandler(final AlexaStateHandler handler) {
            this.__handler = handler;
            return this;
        }

        /**
         * Builds the model. Be sure your model class has a parameterless constructor otherwise this
         * instanciation will fail. In that case this method returns null.
         * @param <TModel> type of the model. Must be of type AlexaStateModel
         * @return model of desired type
         */
        public <TModel extends AlexaStateModel> TModel build() {
            Validate.notNull(this.__handler, "Model needs a handler for its initialization.");

            try {
                final TModel model = (TModel)modelClass.newInstance();
                model.setId(__internalId);
                model.setHandler(__handler);
                return model;
            } catch (InstantiationException | IllegalAccessException e) {
                log.error(String.format("Could not create model of '%1$s'.", this.modelClass.getTypeName()), e);
                return null;
            }
        }
    }

    @Override
    public String toString() {
        return this.getAttributeKey();
    }
}
