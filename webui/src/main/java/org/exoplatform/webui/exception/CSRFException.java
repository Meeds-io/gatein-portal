package org.exoplatform.webui.exception;

public class CSRFException extends Exception{

    public CSRFException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    public CSRFException(String errorMessage) {
        super(errorMessage);
    }
}
