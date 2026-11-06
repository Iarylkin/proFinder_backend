package fit.biejk.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The text must not contain control characters ({@code U+0000} - {@code U+001F} and {@code U+007F}). A multi-line
 * text may contain line feeds and tabs. A missing value is valid.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoControlCharsValidator.class)
public @interface NoControlChars {

    /**
     * The message of the violation.
     *
     * @return the message
     */
    String message() default "must not contain control characters";

    /**
     * Whether line feeds and tabs are allowed (a multi-line text such as a description).
     *
     * @return {@code true} for a multi-line text
     */
    boolean multiline() default false;

    /**
     * The validation groups.
     *
     * @return the groups
     */
    Class<?>[] groups() default {};

    /**
     * The payload of the constraint.
     *
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
}
