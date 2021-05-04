package org.apache.struts.beanaction;

/**
 * This exception is thrown internally by BeanAction and
 * <br>
 * can also be used by bean action methods as a general
 * <br>
 * or base exception.
 * <br>
 * <br>
 * <br>
 * Date: Mar 13, 2004 8:17:00 PM
 *
 * @author Clinton Begin
 */
public class BeanActionException extends RuntimeException {
    public BeanActionException() {
        super();
    }

    public BeanActionException(String string) {
        super(string);
    }

    public BeanActionException(Throwable throwable) {
        super(throwable);
    }

    public BeanActionException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
