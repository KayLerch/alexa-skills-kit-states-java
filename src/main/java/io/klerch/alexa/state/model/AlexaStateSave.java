/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field having this annotation will be saved and loaded by AlexaStateHandlers
 * as long as they don't have the AlexaStateIgnore-tag. Use the scope parameter to define in which
 * scope (Session, User, Application) this consideration should apply.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface AlexaStateSave {
    /**
     * The scope in which the field is considered by AlexaStateHandlers.
     * @return The scope in which the field is considered by AlexaStateHandlers.
     */
    AlexaScope Scope() default AlexaScope.SESSION;
}