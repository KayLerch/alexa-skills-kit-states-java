/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import io.klerch.alexa.state.model.*;
import io.klerch.alexa.state.utils.AlexaStateException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * A state handler is an object which encapsulates persistence logic of your model states
 * It comes in multiple flavours where each of the implementation is dedicated to a persistence store.
 */
public interface AlexaStateHandler {
    /**
     * Returns the Alexa Session object which was given to this handler. This is where state of
     * all models are written to and read from.
     * @return Alexa Session object
     */
    Session getSession();

    /**
     * Creates an instance of your model class and binds it to this handler.
     * Thus you can manage state directly on or in your model without referencing the handler anymore (e.g. {@link AlexaStateModel#saveState()} or {@link AlexaStateModel#removeState()})
     * This really much does the same as what you can achieve with {@link AlexaStateModelFactory#createModel(Class, AlexaStateHandler)}.
     * Of course you can initialize your models with its own constructor but make sure you setHandler() before
     * calling aforementioned methods of this model.
     * By using this method you are not assigning an id to the model. Your model behaves like a singleton within
     * its AlexaScope (Session, User or Application) and is overridden on {@link AlexaStateModel#saveState()} if already existent.
     * @param modelClass type of your model you want to initialize. Model has to derive from {@link AlexaStateModel}
     * @param <TModel> type derived from {@link AlexaStateModel}
     * @return Fresh instance of a model
     */
    <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass);

    /**
     * Creates an instance of your model class and binds it to this handler.
     * Thus you can manage state directly on or in your model without referencing the handler anymore (e.g. {@link AlexaStateModel#saveState()} or {@link AlexaStateModel#removeState()})
     * This really much does the same as what you can achieve with {@link AlexaStateModelFactory#createModel(Class, AlexaStateHandler)}.
     * Of course you can initialize your models with its own constructor but make sure you {@link AlexaStateModel#setHandler(AlexaStateHandler)} before
     * calling aforementioned methods of this model.
     * By using this method you will assign an id to the model. From now on this id is used to read and
     * write to the persistence store associated with the handler. It gives you freedom to have more than one of a kind
     * of your model within its {@link AlexaScope AlexaScope} (Session, User, Application)
     * @param modelClass type of your model you want to initialize. Model has to derive from {@link AlexaStateModel}
     * @param id An id unique within a model's {@link AlexaScope AlexaScope}. If a model with this id is already existing in the persistence store it will be overridden on {@link AlexaStateModel#saveState()}.
     * @param <TModel> type derived from {@link AlexaStateModel}
     * @return Fresh instance of a model
     */
    <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass, final String id);

    /**
     * The given model will be saved in the persistence store according to its {@link AlexaStateSave}-annotations.
     * If you set up an Id for the model it will be accessible with it on later reads to the handler.
     * @param model Your model which needs to be a type of {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeModel(final AlexaStateModel model) throws AlexaStateException;

    /**
     * The given models will be saved in the persistence store according to their {@link AlexaStateSave}-annotations.
     * If you set up an Id for the model it will be accessible with it on later reads to the handler.
     * If you have multiple models to save always prefer this method as it tries to batch process
     * the models so it reduces the number of write-transactions to one.
     * @param models list of models to save state
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeModels(final Collection<? extends AlexaStateModel> models) throws AlexaStateException;

    /**
     * The given value will be saved with the given id in the persistence store. Without giving
     * it a scope the handler will save state of the given object in Alexa session (Scope = Session)
     * @param id the key used when writing value to the store
     * @param value the actual object whose value will be saved
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeValue(final String id, final Object value) throws AlexaStateException;

    /**
     * The given value will be saved with the given id in the persistence store. Keep in
     * mind that for the AlexaSessionStateHandler USER and APPLICATION scope cannot be
     * served. In that case the value will be saved to Alexa session and will not persist
     * permanently.
     * @param id the key used when writing value to the store
     * @param value the actual object whose value will be saved
     * @param scope the scope the value is saved
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeValue(final String id, final Object value, final AlexaScope scope) throws AlexaStateException;

    /**
     * The value of a given state object will be saved with its id in the persistence store. Keep in
     * mind that for the AlexaSessionStateHandler USER and APPLICATION scope cannot be
     * served. In that case the value will be saved to Alexa session and will not persist
     * permanently.
     * @param stateObject the state object to write to the persistence store
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeValue(final AlexaStateObject stateObject) throws AlexaStateException;

    /**
     * Values of given state objects will be saved with their ids in the persistence store. Keep in
     * mind that for the AlexaSessionStateHandler USER and APPLICATION scope cannot be
     * served. In that case the value will be saved to Alexa session and will not persist
     * permanently.
     * @param stateObjects the state objects to write to the persistence store
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeValues(final Collection<? extends AlexaStateObject> stateObjects) throws AlexaStateException;

    /**
     * The given model will be removed from the persistence store. If it's not existing in the store nothing happens.
     * Be aware of the Id set in the model as the removal will only affect the model with this id.
     * @param model Your model which needs to be a type of {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void removeModel(final AlexaStateModel model) throws AlexaStateException;

    /**
     * The given models will be removed from the persistence store. If one of them is not existing in the store nothing happens.
     * Be aware of the Id set in the models as the removal will only affect the model with its id.
     * @param models Your models which need to be a type of {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void removeModels(final Collection<? extends AlexaStateModel> models) throws AlexaStateException;

    /**
     * Removes a single value state from the persistence store with the given key used
     * when single value was written in store. You could also use this method to remove
     * a model. In that case be sure you provide the attribute-key of the model - not its
     * id.
     * @param id the key of a single value object whose state you want to remove from the store.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void removeValue(final String id) throws AlexaStateException;

    /**
     * Removes multiple value states from the persistence store with given keys used
     * when those values were written in store. You could also use this method to remove
     * models. In that case be sure you provide the attributeKey of those models - not their
     * id.
     * @param ids the keys of single value objects whose state you want to remove from the store.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void removeValues(final Collection<String> ids) throws AlexaStateException;

    /**
     * Reads out the model from the persistence store of this handler. Depending on the {@link AlexaScope AlexaScope}s configured in
     * the {@link AlexaStateSave AlexaStateSave} annotations it will possibly collect data from more than one persistence store. (most
     * likely from the Alexa session and maybe from the permanent persistence store associated with this handler.
     * You are not providing an id with this call. Thus it will look for the single representative without an id.
     * @param modelClass Type of the model you would like to read out. It needs to be of type {@link AlexaStateModel}.
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return A model matching the given type and augmented with all the attributes found in the persistence store.
     */
    <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass) throws AlexaStateException;

    /**
     * Reads out the model from the persistence store of this handler. Depending on the {@link AlexaScope AlexaScope}s configured in
     * the {@link AlexaStateSave AlexaStateSave} annotations it will possibly collect data from more than one persistence store. (most
     * likely from the Alexa session and maybe from the permanent persistence store associated with this handler.
     * You are providing an id with this call. If you never saved the model with this id in the store you won't get anything.
     * @param modelClass Type of the model you would like to read out. It needs to be of type {@link AlexaStateModel}.
     * @param id The id of an existing instance of your model in the persistence store.
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return A model matching the given type and id. Augmented with all the attributes found in the persistence store.
     */
    <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass, final String id) throws AlexaStateException;

    /**
     * Reads out models from the persistence store of this handler. Depending on the {@link AlexaScope AlexaScope}s configured in
     * the {@link AlexaStateSave AlexaStateSave} annotations it will possibly collect data from more than one persistence store. (most
     * likely from the Alexa session and maybe from the permanent persistence store associated with this handler.)
     * You are providing ids with this call. If you never saved any model with one of the ids in the store you won't get them.
     * @param modelClass Type of the model you would like to read out. It needs to be of type {@link AlexaStateModel}.
     * @param ids Collection of ids of existing instances of your model in the persistence store. If you provide ids of non-existing items they just won't show up in the returned map.
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     * @return Map of ids from your input list of ids pointing to models that were found in the persistence store. Augmented with all the attributes found in the persistence store.
     */
    <TModel extends AlexaStateModel> Map<String, TModel> readModels(final Class<TModel> modelClass, final Collection<String> ids) throws AlexaStateException;

    /**
     * Reads a single object value from the persistence store. If no scope is provided this method will
     * always look for the value in Alexa session by default (Scope = Session). If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param id id of a state-object whose state you want to read from the store.
     * @return the value of a state object with the given id. Empty, if not existent.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    Optional<AlexaStateObject> readValue(final String id) throws AlexaStateException;

    /**
     * Reads a single object value from the persistence store. If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param id id of a state-object whose state you want to read from the store.
     * @param scope read state-object in that scope.
     * @return the value of a state object with the given id. Empty, if not existent.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    Optional<AlexaStateObject> readValue(final String id, final AlexaScope scope) throws AlexaStateException;

    /**
     * Reads multiple single object values from the persistence store. If no scope is provided this method will
     * always look for values in Alexa session by default (Scope = Session). If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param ids ids of state-objects whose state you want to read from the store.
     * @return map of state-objects found in the store where key is the object-id. State-objects
     * which were not found in the store will be missing in that map.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    Map<String, AlexaStateObject> readValues(final Collection<String> ids) throws AlexaStateException;

    /**
     * Reads multiple single object values from the persistence store in a specific scope. If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param ids ids of state-objects whose state you want to read from the store.
     * @param scope read state-objects in that scope.
     * @return map of state-objects found in the store where key is the object-id. State-objects
     * which were not found in the store will be missing in that map.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    Map<String, AlexaStateObject> readValues(final Collection<String> ids, final AlexaScope scope) throws AlexaStateException;

    /**
     * Reads multiple single object values from the persistence store in scopes. If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param idsInScope ids with scope of state-objects whose state you want to read from the store.
     * @return map of state-objects found in the store where key is the object-id. State-objects
     * which were not found in the store will be missing in that map.
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    Map<String, AlexaStateObject> readValues(final Map<String, AlexaScope> idsInScope) throws AlexaStateException;

    /**
     * Looks for a model in the persistence store. As this method does not take an id it
     * looks for the static model instance which was written over the handler without giving
     * it an id. If no scope is provided this method will always look for the model in Alexa
     * session by default (Scope = Session)
     * @param modelClass Type of the model you would like to check for existence. It needs to be of type {@link AlexaStateModel}.
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @return True, if model exists
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass) throws AlexaStateException;

    /**
     * Looks for a model with given id in the persistence store. If no scope is provided this method will
     * always look for the model in Alexa session by default (Scope = Session)
     * @param modelClass Type of the model you would like to check for existence. It needs to be of type {@link AlexaStateModel}.
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @param id the id of a model
     * @return True, if model exists
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass, final String id) throws AlexaStateException;

    /**
     * Looks for a model in the persistence store in a given scope. As this method does not take an id it
     * looks for the static model instance which was written over the handler without giving
     * it an id.
     * @param modelClass Type of the model you would like to check for existence. It needs to be of type {@link AlexaStateModel}.
     * @param scope scope in which to test existence of model
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @return True, if model exists in scope
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass, final AlexaScope scope) throws AlexaStateException;

    /**
     * Looks for a model with the given id in the persistence store in a given scope.
     * @param modelClass Type of the model you would like to check for existence. It needs to be of type {@link AlexaStateModel}.
     * @param <TModel> Type derived from {@link AlexaStateModel}
     * @param id the id of a model
     * @param scope scope in which to test existence of model
     * @return True, if model exists in scope
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    <TModel extends AlexaStateModel> boolean exists(final Class<TModel> modelClass, final String id, final AlexaScope scope) throws AlexaStateException;

    /**
     * Looks for a state object with the given id. If no scope is provided this method will
     * always look for the model in Alexa session by default (Scope = Session). If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param id id of a state-object whose existence you want to be checked in the store.
     * @return True, if state object exists in the store
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    boolean exists(final String id) throws AlexaStateException;

    /**
     * Looks for a state object with the given id in the given scope. If you want
     * to check existence of a state model keep in mind its id is the attribute-key of the
     * model object, not the id you provided. The attribute-key derives from the model-id.
     * @param id id of a state-object whose existence you want to be checked in the store.
     * @param scope look for state-object in that scope.
     * @return True, if state object exists in that scope
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    boolean exists(final String id, final AlexaScope scope) throws AlexaStateException;
}
