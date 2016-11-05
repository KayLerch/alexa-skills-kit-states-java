package io.klerch.alexa.state.model;

/**
 * Instead of managing state of POJO models you can also provide id-value objects
 * to the state handlers.
 */
public class AlexaStateObject {
    private final String id;
    private final Object value;
    private final AlexaScope scope;

    /**
     * Creates a id-value state. When given to a state handler the object will be
     * saved in SESSION scope.
     * @param id the id used by the state handler to write value to persistence store.
     * @param value the value to write to persistence store by a state handler
     */
    public AlexaStateObject(final String id, final Object value) {
        this(id, value, AlexaScope.SESSION);
    }

    /**
     * Creates a id-value state. When given to a state handler the object will be
     * saved in the given scope.
     * @param id the id used by the state handler to write value to persistence store.
     * @param value the value to write to persistence store by a state handler
     * @param scope the scope used by the state handler to write value to persistence store.
     */
    public AlexaStateObject(final String id, final Object value, final AlexaScope scope) {
        this.id = id;
        this.value = value;
        this.scope = scope;
    }

    /**
     * Returns the id used by the state handler to read/write value from/to persistence store.
     * @return The id used by the state handler to read/write value from/to persistence store.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the value written/read by a state handler from/to persistence store
     * @return The value written/read by a state handler from/to persistence store
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the scope used by the state handler to read/write value from/to persistence store.
     * @return the scope used by the state handler to read/write value from/to persistence store.
     */
    public AlexaScope getScope() {
        return scope;
    }
}
