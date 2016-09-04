/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

/**
 * The scope defines in which context state is stored in the persistence stores. Alexa gives you three
 * of those contexts. One is the session-scope which is valid throughout one user-session. Values saved in this
 * scope will be gone on session termination. On the other hand there are user- and application-scopes where values
 * persist throughout many session - either per authenticated Alexa-user or per Alexa application. This is how you
 * would scope user-defined configuration (user-scope) or an overall highscore or counter (app-scope).
 */
public enum AlexaScope {
    /**
     * State saved in this scope is persistent throughout a single user session in a skill and
     * won't be saved permanently.
     */
    SESSION(0),
    /**
     * State saved in this scope is shared across all sessions by an Amazon user (amzn-user-id)
     */
    USER(1),
    /**
     * State saved in this scope is shared across the whole application (skill) by all users
     */
    APPLICATION(1);

    // apply numeric value to scopes cause it gives us the order of scopes in terms of their inclusions (0 includes 1 includes 2)
    // e.g. app-scope with value 1 is included in session-scope with value 0.
    // so app-scoped fields will be included in session-scope
    // same applies for state-ignore tags. ignored fields for session-scope should not be ignored in app-scope
    private int value;

    AlexaScope(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    /**
     * Checks if the given scope is covered by this scope.
     * e.g. fields tagged with user-scoped(=1) are part of the session-scope(=0)
     * e.g. fields tagged with user-scoped(=1) are not part of app-scope(=1)
     * @param scope The scope to test for coverage by this scope
     * @return True, if the given scope is covered by this scope
     */
    public boolean includes(final AlexaScope scope) {
        // the given scope needs a higher value than this one to get covered except for they equal
        // this is how scopes with same values exclude each other (which is desirable for user and application)

        return scope.getValue() > this.value || this.equals(scope);
    }

    /**
     * Checks if the given scope is not covered by this scope.
     * e.g. fields tagged as session-scope(=0) are not part of the app-scope(=1)
     * e.g. fields tagged as app-scoped(=1) are neither part of user-scope(=1)
     * @param scope The scope to test for coverage by this scope
     * @return True, if the given scope is not covered by this scope
     */
    public boolean excludes(final AlexaScope scope) {
        // the given scope needs a lower value than this one to be excluded from this one
        return scope.getValue() <= this.value && !this.equals(scope);
    }
}
