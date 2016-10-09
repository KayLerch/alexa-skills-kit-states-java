package io.klerch.alexa.state.model;

/**
 * Instead of managing state of POJO models you can also provide key-value objects
 * to the state handlers.
 */
public class AlexaStateObject {
    private final String key;
    private final Object value;
    private final AlexaScope scope;

    /**
     * Creates a key-value state. When given to a state handler the object will be
     * saved in SESSION scope.
     * @param key the key used by the state handler to write value to persistence store.
     * @param value the value to write to persistence store by a state handler
     */
    public AlexaStateObject(final String key, final Object value) {
        this(key, value, AlexaScope.SESSION);
    }

    /**
     * Creates a key-value state. When given to a state handler the object will be
     * saved in the given scope.
     * @param key the key used by the state handler to write value to persistence store.
     * @param value the value to write to persistence store by a state handler
     * @param scope the scope used by the state handler to write value to persistence store.
     */
    public AlexaStateObject(final String key, final Object value, final AlexaScope scope) {
        this.key = key;
        this.value = value;
        this.scope = scope;
    }

    /**
     * Returns the key used by the state handler to read/write value from/to persistence store.
     * @return The key used by the state handler to read/write value from/to persistence store.
     */
    public String getKey() {
        return key;
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
