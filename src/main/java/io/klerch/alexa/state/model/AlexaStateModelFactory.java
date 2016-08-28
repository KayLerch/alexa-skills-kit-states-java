/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

import io.klerch.alexa.state.handler.AlexaStateHandler;

public class AlexaStateModelFactory {
    /**
     * Creates an instance of your POJO model classes derived from AlexaStateModel. This is the usual way of instantiating
     * the models as it makes sure a AlexaStateHandler is applied. An even more convenient way of instantiating your models
     * is with your AlexaStateHandlers createModel method.
     * @param modelClass The type of your POJO model derived from AlexaStateModel
     * @param handler The handler which should take care of your model when saving, loading or removing the model.
     * @param <TModel> The type of your POJO model derived from AlexaStateModel
     * @return a new model with already applied handler
     */
    public static <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass, final AlexaStateHandler handler) {
        return AlexaStateModel.create(modelClass).withHandler(handler).build();
    }

    /**
     * Creates an instance of your POJO model classes derived from AlexaStateModel. This is the usual way of instantiating
     * the models as it makes sure a AlexaStateHandler is applied as well as an id for the model.
     * An even more convenient way of instantiating your models
     * is with your AlexaStateHandlers createModel method.
     * @param modelClass The type of your POJO model derived from AlexaStateModel
     * @param handler The handler which should take care of your model when saving, loading or removing the model.
     * @param id The id which is used by the AlexaStateHandler when saving, loading or removing the model
     * @param <TModel> The type of your POJO model derived from AlexaStateModel
     * @return a new model with already applied handler and id
     */
    public static <TModel extends AlexaStateModel> TModel createModel(final Class<TModel> modelClass, final AlexaStateHandler handler, final String id) {
        return AlexaStateModel.create(modelClass).withId(id).withHandler(handler).build();
    }
}
