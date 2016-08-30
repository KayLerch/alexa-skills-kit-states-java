package io.klerch.alexa.state.handler;

public class AlexaSessionStateHandlerTest extends AlexaStateHandlerTest<AlexaSessionStateHandler> {
    @Override
    public AlexaSessionStateHandler getHandler() {
        return new AlexaSessionStateHandler(session);
    }
}