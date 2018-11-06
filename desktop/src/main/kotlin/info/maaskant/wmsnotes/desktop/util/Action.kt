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

package info.maaskant.wmsnotes.desktop.util

import com.github.thomasnield.rxkotlinfx.observeOnFx
import de.jensd.fx.glyphs.GlyphIcon
import io.reactivex.Observable
import javafx.scene.control.Button
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCombination
import org.slf4j.LoggerFactory
import tornadofx.*

class Action(
        messageKey: String,
        accelerator: String? = null,
        val icon: GlyphIcon<*>? = null,
        val disable: Observable<Boolean>? = null,
        val action: () -> Unit
) {
    val text: String by lazy { Messages[messageKey] }
    val accelerator: KeyCombination? = if (accelerator != null) KeyCombination.valueOf(accelerator) else null
}

fun Menu.item(action: Action): MenuItem =
        this.item(
                name = action.text,
                graphic = action.icon,
                keyCombination = action.accelerator
        ).apply {
            action(action.action)
        }

fun ToolBar.button(action: Action, op: Button.() -> Unit = {}): Button =
        this.button(
                text = action.text,
                graphic = action.icon,
                op = op
        ).apply {
            action(action.action)
            action.disable
                    ?.observeOnFx()
                    ?.subscribe(this::setDisable) { LoggerFactory.getLogger(Action::class.java).warn("Error", it) }
        }

