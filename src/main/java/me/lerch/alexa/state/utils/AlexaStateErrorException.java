/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.utils;

import me.lerch.alexa.state.handler.AlexaStateHandler;
import me.lerch.alexa.state.model.AlexaStateModel;

public class AlexaStateErrorException extends Exception {
    private static final long serialVersionUID = 466433664499611218L;
    private final AlexaStateModel model;
    private final AlexaStateHandler handler;

    public AlexaStateErrorException(final AlexaStateErrorExceptionBuilder builder) {
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

    public static AlexaStateErrorExceptionBuilder create(final String errorMessage) {
        return new AlexaStateErrorExceptionBuilder(errorMessage);
    }

    public static class AlexaStateErrorExceptionBuilder {
        AlexaStateModel model;
        AlexaStateHandler handler;
        final String errorMessage;
        Throwable cause;

        protected AlexaStateErrorExceptionBuilder(final String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public AlexaStateErrorExceptionBuilder withModel(final AlexaStateModel model) {
            this.model = model;
            return this;
        }

        public AlexaStateErrorExceptionBuilder withHandler(final AlexaStateHandler handler) {
            this.handler = handler;
            return this;
        }

        public AlexaStateErrorExceptionBuilder withCause(final Throwable cause) {
            this.cause = cause;
            return this;
        }

        public AlexaStateErrorException build() {
            return new AlexaStateErrorException(this);
        }
    }
}
