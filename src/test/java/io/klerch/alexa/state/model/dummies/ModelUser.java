package io.klerch.alexa.state.model.dummies;

import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaStateSave;

@AlexaStateSave(Scope= AlexaScope.USER)
public class ModelUser extends AlexaStateModel {
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
