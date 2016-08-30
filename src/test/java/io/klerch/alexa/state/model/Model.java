/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

import java.util.ArrayList;
import java.util.List;

public class Model extends AlexaStateModel {
    private String privateField;
    @AlexaStateSave public String sampleString;
    @AlexaStateSave(Scope = AlexaScope.USER) public String sampleUser;
    @AlexaStateSave(Scope = AlexaScope.APPLICATION) public boolean sampleApplication;
    @AlexaStateSave(Scope = AlexaScope.SESSION) public List<String> sampleSession = new ArrayList<>();
    @AlexaStateIgnore public String sampleIgnore;
    public Model() {}
}
