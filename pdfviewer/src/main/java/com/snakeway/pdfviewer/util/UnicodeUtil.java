package com.snakeway.pdfviewer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author snakeway
 */
public class UnicodeUtil {

    public static String unicodeToString(String unicodeStr) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(unicodeStr);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            unicodeStr = unicodeStr.replace(matcher.group(1), ch + "");
        }
        return unicodeStr;
    }

    public static String convertUnicode(int unicode) {
        return "\\u" + Util.leftPaddingString(Integer.toHexString(unicode), 4, '0');
    }
}
