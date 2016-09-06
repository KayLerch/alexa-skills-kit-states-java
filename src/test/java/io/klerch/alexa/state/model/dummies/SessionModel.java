/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model.dummies;

import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateIgnore;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaStateSave;

import java.util.ArrayList;
import java.util.List;

@AlexaStateSave
public class SessionModel extends AlexaStateModel {
    private String privateField;
    @AlexaStateSave public String sampleString;
    @AlexaStateIgnore
    public String sampleIgnore;
    @AlexaStateIgnore (Scope= AlexaScope.SESSION) public String sampleIgnoreSession;
    @AlexaStateIgnore (Scope=AlexaScope.USER) public String sampleIgnoreUser;
    @AlexaStateIgnore (Scope=AlexaScope.APPLICATION) public String sampleIgnoreApplication;
    public SessionModel() {}
}
