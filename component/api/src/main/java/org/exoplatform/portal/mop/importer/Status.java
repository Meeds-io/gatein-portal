package org.exoplatform.portal.mop.importer;
public enum Status {

    UNKNOWN(-1),

    FAILED(0),

    DONE(1),

    WANT_REIMPORT(2);

    private final int status;

    Status(int status) {
        this.status = status;
    }

    public int status() {
        return this.status;
    }

    public static Status getStatus(int status) {
        for (Status type : Status.values()) {
            if (type.status() == status) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
