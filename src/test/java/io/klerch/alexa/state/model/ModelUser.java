package io.klerch.alexa.state.model;

@AlexaStateSave(Scope=AlexaScope.USER)
public class ModelUser extends AlexaStateModel {
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
