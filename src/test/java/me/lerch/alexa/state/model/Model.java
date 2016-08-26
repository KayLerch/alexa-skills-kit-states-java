package me.lerch.alexa.state.model;

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
