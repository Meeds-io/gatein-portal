package org.exoplatform.portal.jdbc.service;

public class Util {
    public static long parseLong(String s) {
        try {
            return s == null ? 0 : Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
