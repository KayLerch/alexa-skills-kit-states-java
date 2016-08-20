/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field having this annotation will be ignored from being saved and loaded by AlexaStateHandlers
 * even when they have the AlexaStateSave annotation. Use the scope parameter to define in which
 * scope (Session, User, Application) the ignore should apply. This tag makes most sense if the
 * whole model class is annotated with AlexaStateSave and only few fields should be excluded from
 * the general saving and loading of all fields of the model.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AlexaStateIgnore {
    /**
     * The scope in which the field is ignored by AlexaStateHandlers.
     * @return The scope in which the field is ignored by AlexaStateHandlers.
     */
    AlexaScope Scope() default AlexaScope.SESSION;
}