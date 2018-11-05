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

package info.maaskant.wmsnotes.desktop.preview;

import com.vladsch.flexmark.ast.Node;
import info.maaskant.wmsnotes.desktop.editing.EditingModel;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.BorderPane;

import java.nio.file.Path;

/**
 * Markdown preview pane.
 *
 * @author Karl Tauber
 */
public class MarkdownPreviewPane {
    private final BorderPane pane = new BorderPane();
    private final Preview preview = new WebViewPreview();
    private final PreviewContext previewContext = new PreviewContext() {
        @Override
        public Node getMarkdownAst() {
            return markdownAst.get();
        }

        @Override
        public String getHtml() {
            return html.get();
        }

        @Override
        public Path getPath() {
            return path.get();
        }
    };

    interface Preview {
        javafx.scene.Node getNode();

        void update(PreviewContext context);

        void scrollY(PreviewContext context, double value);
    }

    interface PreviewContext {
        Node getMarkdownAst();

        String getHtml();

        Path getPath();
    }

    public MarkdownPreviewPane(EditingModel editingModel) {
        pane.getStyleClass().add("preview-pane");
        pane.setCenter(preview.getNode());

//        path.addListener((observable, oldValue, newValue) -> update());
        editingModel.getMarkdownAst().subscribe((markdownAst) -> {
            this.markdownAst.setValue(markdownAst);
            update();
        });
        editingModel.getHtml().subscribe((html) -> {
            this.html.setValue(html);
            update();
        });
//        scrollY.addListener((observable, oldValue, newValue) -> scrollY());
    }

    public javafx.scene.Node getNode() {
        return pane;
    }

    private boolean updateRunLaterPending;

    private void update() {
        // avoid too many (and useless) runLater() invocations
        if (updateRunLaterPending)
            return;
        updateRunLaterPending = true;

        Platform.runLater(() -> {
            updateRunLaterPending = false;
            preview.update(previewContext);
        });
    }

    private boolean scrollYrunLaterPending;

    private void scrollY() {
        // avoid too many (and useless) runLater() invocations
        if (scrollYrunLaterPending)
            return;
        scrollYrunLaterPending = true;

        Platform.runLater(() -> {
            scrollYrunLaterPending = false;
            preview.scrollY(previewContext, scrollY.get());
        });
    }

    private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private final ObjectProperty<Node> markdownAst = new SimpleObjectProperty<>();
    private final ObjectProperty<String> html = new SimpleObjectProperty<>();
    private final DoubleProperty scrollY = new SimpleDoubleProperty();
}
