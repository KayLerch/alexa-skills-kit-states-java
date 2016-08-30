/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModelFactory;
import io.klerch.alexa.state.model.AlexaStateSave;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.state.model.AlexaStateModel;

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
     * If you set up an Id for the model it will be accessible with it on later on reads to the handler.
     * @param model Your model which needs to be a type of {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void writeModel(final AlexaStateModel model) throws AlexaStateException;

    /**
     * The given model will be removed from the persistence store. If it's not existing in the store nothing happens.
     * Be aware of the Id set in the model as the removal will only affect the model with this id.
     * @param model Your model which needs to be a type of {@link AlexaStateModel}
     * @throws AlexaStateException Wraps all inner exceptions and gives you context related to handler and model
     */
    void removeModel(final AlexaStateModel model) throws AlexaStateException;

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
}
