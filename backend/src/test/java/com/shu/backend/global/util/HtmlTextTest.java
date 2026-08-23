package com.shu.backend.global.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void unescapeRestoresHtmlEntitiesForDisplay() {
        String stored = "title &hellip; &lt;script&gt; &amp; &#39; &quot;";

        String result = HtmlText.unescape(stored);

        assertThat(result).isEqualTo("title \u2026 <script> & ' \"");
    }

    @Test
    void unescapeKeepsNullSafe() {
        assertThat(HtmlText.unescape(null)).isNull();
        assertThat(HtmlText.unescapeOrEmpty(null)).isEmpty();
    }
}
