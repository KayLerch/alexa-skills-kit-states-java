/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.utils;

import io.klerch.alexa.state.handler.AlexaStateHandler;
import io.klerch.alexa.state.model.AlexaStateModel;

public class AlexaStateException extends Exception {
    private static final long serialVersionUID = 466433664499611218L;
    private final AlexaStateModel model;
    private final AlexaStateHandler handler;

    public AlexaStateException(final AlexaStateExceptionBuilder builder) {
        super(builder.errorMessage, builder.cause);
        this.model = builder.model;
        this.handler = builder.handler != null ? builder.handler : this.model != null ? model.getHandler() : null;
    }

    public AlexaStateModel getModel() {
        return this.model;
    }

    public AlexaStateHandler getHandler() {
        return this.handler;
    }

    public static AlexaStateExceptionBuilder create(final String errorMessage) {
        return new AlexaStateExceptionBuilder(errorMessage);
    }

    public static class AlexaStateExceptionBuilder{
        AlexaStateModel model;
        AlexaStateHandler handler;
        final String errorMessage;
        Throwable cause;

        protected AlexaStateExceptionBuilder(final String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public AlexaStateExceptionBuilder withModel(final AlexaStateModel model) {
            this.model = model;
            return this;
        }

        public AlexaStateExceptionBuilder withHandler(final AlexaStateHandler handler) {
            this.handler = handler;
            return this;
        }

        public AlexaStateExceptionBuilder withCause(final Throwable cause) {
            this.cause = cause;
            return this;
        }

        public AlexaStateException build() {
            return new AlexaStateException(this);
        }
    }
}
