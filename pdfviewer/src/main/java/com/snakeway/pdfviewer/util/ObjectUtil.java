package com.snakeway.pdfviewer.util;

/**
 * @author snakeway
 */
public class ObjectUtil {

    public static boolean isObjectEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
