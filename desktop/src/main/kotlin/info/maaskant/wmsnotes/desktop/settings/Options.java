/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package info.maaskant.wmsnotes.desktop.settings;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.text.Font;

import java.util.List;
import java.util.prefs.Preferences;

public class Options {
    private static final String[] DEF_FONT_FAMILIES = {
            "Consolas",
            "DejaVu Sans Mono",
            "Lucida Sans Typewriter",
            "Lucida Console",
    };

    public static final int DEF_FONT_SIZE = 12;
    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 36;

    public Options(Preferences options) {
        fontFamily.init(options, "fontFamily", null, this::safeFontFamily);
        fontSize.init(options, "fontSize", DEF_FONT_SIZE);
        lineSeparator.init(options, "lineSeparator", null);
        showLineNo.init(options, "showLineNo", false);
        showWhitespace.init(options, "showWhitespace", false);

        emphasisMarker.init(options, "emphasisMarker", "_");
        strongEmphasisMarker.init(options, "strongEmphasisMarker", "**");
        bulletListMarker.init(options, "bulletListMarker", "-");
    }

    /**
     * Check whether font family is null or invalid (family not available on system)
     * and search for an available family.
     */
    private String safeFontFamily(String fontFamily) {
        List<String> fontFamilies = Font.getFamilies();
        if (fontFamily != null && fontFamilies.contains(fontFamily))
            return fontFamily;

        for (String family : DEF_FONT_FAMILIES) {
            if (fontFamilies.contains(family))
                return family;
        }
        return "Monospaced";
    }

    // 'fontFamily' property
    private final PrefsStringProperty fontFamily = new PrefsStringProperty();

    public String getFontFamily() {
        return fontFamily.get();
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily.set(fontFamily);
    }

    public StringProperty fontFamilyProperty() {
        return fontFamily;
    }

    // 'fontSize' property
    private final PrefsIntegerProperty fontSize = new PrefsIntegerProperty();

    public int getFontSize() {
        return fontSize.get();
    }

    public void setFontSize(int fontSize) {
        this.fontSize.set(Math.min(Math.max(fontSize, MIN_FONT_SIZE), MAX_FONT_SIZE));
    }

    public IntegerProperty fontSizeProperty() {
        return fontSize;
    }

    // 'lineSeparator' property
    private final PrefsStringProperty lineSeparator = new PrefsStringProperty();

    public String getLineSeparator() {
        return lineSeparator.get();
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator.set(lineSeparator);
    }

    public StringProperty lineSeparatorProperty() {
        return lineSeparator;
    }

    // 'showLineNo' property
    private final PrefsBooleanProperty showLineNo = new PrefsBooleanProperty();

    public boolean isShowLineNo() {
        return showLineNo.get();
    }

    public void setShowLineNo(boolean showLineNo) {
        this.showLineNo.set(showLineNo);
    }

    public BooleanProperty showLineNoProperty() {
        return showLineNo;
    }

    // 'showWhitespace' property
    private final PrefsBooleanProperty showWhitespace = new PrefsBooleanProperty();

    public boolean isShowWhitespace() {
        return showWhitespace.get();
    }

    public void setShowWhitespace(boolean showWhitespace) {
        this.showWhitespace.set(showWhitespace);
    }

    public BooleanProperty showWhitespaceProperty() {
        return showWhitespace;
    }

    // 'emphasisMarker' property
    private final PrefsStringProperty emphasisMarker = new PrefsStringProperty();

    public String getEmphasisMarker() {
        return emphasisMarker.get();
    }

    public void setEmphasisMarker(String emphasisMarker) {
        this.emphasisMarker.set(emphasisMarker);
    }

    public StringProperty emphasisMarkerProperty() {
        return emphasisMarker;
    }

    // 'strongEmphasisMarker' property
    private final PrefsStringProperty strongEmphasisMarker = new PrefsStringProperty();

    public String getStrongEmphasisMarker() {
        return strongEmphasisMarker.get();
    }

    public void setStrongEmphasisMarker(String strongEmphasisMarker) {
        this.strongEmphasisMarker.set(strongEmphasisMarker);
    }

    public StringProperty strongEmphasisMarkerProperty() {
        return strongEmphasisMarker;
    }

    // 'bulletListMarker' property
    private final PrefsStringProperty bulletListMarker = new PrefsStringProperty();

    public String getBulletListMarker() {
        return bulletListMarker.get();
    }

    public void setBulletListMarker(String bulletListMarker) {
        this.bulletListMarker.set(bulletListMarker);
    }

    public StringProperty bulletListMarkerProperty() {
        return bulletListMarker;
    }
}
