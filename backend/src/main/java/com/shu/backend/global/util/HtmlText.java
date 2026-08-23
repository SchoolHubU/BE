package com.shu.backend.global.util;

import org.springframework.web.util.HtmlUtils;

public final class HtmlText {

    private HtmlText() {
    }

    public static String unescape(String value) {
        return value == null ? null : HtmlUtils.htmlUnescape(value);
    }

    public static String unescapeOrEmpty(String value) {
        return value == null ? "" : HtmlUtils.htmlUnescape(value);
    }
}
