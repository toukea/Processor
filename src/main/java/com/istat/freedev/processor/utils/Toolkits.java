package com.istat.freedev.processor.utils;

public class Toolkits {

    public static final boolean isEmpty(String string) {
        return string == null || string.equals("");
    }

    public static final boolean isEmpty(Object string) {
        return string == null || "".equals("" + string);
    }
}
