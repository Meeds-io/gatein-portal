package org.exoplatform.web.security.csrf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describe a method which needs CSRF protection.
 * https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)
 *
 * Cross-Site Request Forgery (CSRF) is an attack that forces an end user to execute unwanted
 * actions on a web application in which they're currently authenticated.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExoCSRFCheck {
}
