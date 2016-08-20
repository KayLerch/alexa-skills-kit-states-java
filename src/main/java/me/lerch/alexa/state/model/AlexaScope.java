/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.model;

/**
 * The scope defines in which context values are stored in the persistence stores. Alexa gives you three
 * of those contexts. One is the session-scope which is valid throughout one user-session. Values saved in this
 * scope will be gone on session termination. On the other hand there are user- and application-scopes where values
 * persist throughout many session - either per authenticated Alexa-user or per Alexa application. This is how you
 * would scope user-defined configuration (user-scope) or an overall highscore or counter (app-scope).
 */
public enum AlexaScope {
    // apply numeric value to scopes cause it gives us the order of scopes in terms of their inclusions (0 includes 1 includes 2)
    // e.g. app-scope with value 1 is included in session-scope with value 0.
    // so app-scoped fields will be included in session-scope
    // same applies for state-ignore tags. ignored fields for session-scope should not be ignored in app-scope
    SESSION(0), USER(1), APPLICATION(1);

    private int value;

    AlexaScope(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public boolean includes(final AlexaScope scope) {
        // the given scope needs a higher value than this one to get covered except for they equal
        // this is how scopes with same values exclude each other (which is desirable for user and application)
        // e.g. fields tagged with user-scoped(=1) are part of the session-scope(=0)
        // e.g. fields tagged with user-scoped(=1) are not part of app-scope(=1)
        return scope.getValue() > this.value || this.equals(scope);
    }

    public boolean excludes(final AlexaScope scope) {
        // the given scope needs a lower value than this one to be excluded from this one
        // e.g. fields tagged as session-scope(=0) are not part of the app-scope(=1)
        // e.g. fields tagged as app-scoped(=1) are neither part of user-scope(=1)
        return scope.getValue() <= this.value;
    }
}
