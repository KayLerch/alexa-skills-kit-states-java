/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

public class AlexaSessionStateHandlerTest extends AlexaStateHandlerTest<AlexaSessionStateHandler> {
    @Override
    public AlexaSessionStateHandler givenHandler() {
        return new AlexaSessionStateHandler(session);
    }
}