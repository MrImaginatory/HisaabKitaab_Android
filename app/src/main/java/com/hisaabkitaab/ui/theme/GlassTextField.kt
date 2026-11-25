package com.hisaabkitaab.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Unified state-based Material 3 Outlined text field powered by Compose Foundation's modern
 * [BasicTextField] ([androidx.compose.foundation.text.input.TextFieldState]) and
 * [OutlinedTextFieldDefaults.DecorationBox].
 *
 * Unlike legacy String-based OutlinedTextField, this natively supports:
 * - Accurate single-tap cursor repositioning
 * - Double-tap word selection & triple-tap paragraph selection on Android touchscreens
 * - Smooth long-press selection handles & floating Cut/Copy/Paste toolbar
 * - Non-destructive input validation via [InputTransformation] without cursor resets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: TextFieldColors = glassTextFieldColors(),
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding()
) {
    val cs = MaterialTheme.colorScheme
    val state = rememberTextFieldState(initialText = value)
    val interactionSource = remember { MutableInteractionSource() }
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val emittedHistory = remember { ArrayDeque<String>() }

    // Emit text changes to caller ONLY when the text buffer itself changes (never on cursor/selection moves).
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .distinctUntilChanged()
            .drop(1)
            .collect { currentText ->
                if (emittedHistory.size >= 16) emittedHistory.removeFirst()
                emittedHistory.addLast(currentText)
                latestOnValueChange(currentText)
            }
    }

    // Apply external updates (OCR prefill, Search Clear button, Currency preset, Color swatch, DB reload)
    // without interfering with in-flight user typing or cursor/selection state.
    LaunchedEffect(value) {
        val currentText = state.text.toString()
        if (value != currentText && !emittedHistory.contains(value)) {
            emittedHistory.clear()
            state.setTextAndPlaceCursorAtEnd(value)
        } else if (value == currentText) {
            emittedHistory.clear()
        }
    }

    val lineLimits = remember(singleLine, minLines, maxLines) {
        if (singleLine) {
            TextFieldLineLimits.SingleLine
        } else {
            TextFieldLineLimits.MultiLine(
                minHeightInLines = minLines.coerceAtLeast(1),
                maxHeightInLines = maxLines.coerceAtLeast(minLines.coerceAtLeast(1))
            )
        }
    }

    val mergedTextStyle = textStyle.merge(
        TextStyle(color = if (enabled) cs.onSurface else cs.onSurfaceVariant)
    )

    BasicTextField(
        state = state,
        modifier = modifier
            .then(if (label != null) Modifier.padding(top = 8.dp) else Modifier)
            .defaultMinSize(
                minWidth = OutlinedTextFieldDefaults.MinWidth,
                minHeight = OutlinedTextFieldDefaults.MinHeight
            ),
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = inputTransformation,
        textStyle = mergedTextStyle,
        keyboardOptions = keyboardOptions,
        lineLimits = lineLimits,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(if (isError) cs.error else cs.primary),
        decorator = TextFieldDecorator { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = state.text.toString(),
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                singleLine = singleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = contentPadding,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        shape = shape
                    )
                }
            )
        }
    )
}

/**
 * Truncates text beyond [maxChars] without blocking paste or disturbing cursor/selection changes.
 */
@OptIn(ExperimentalFoundationApi::class)
class MaxLengthTransformation(private val maxChars: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        if (length > maxChars) {
            delete(maxChars, length)
        }
    }
}

/**
 * Rejects edits whose resulting text fails [regex] (e.g. decimal amount), leaving selection-only
 * gestures completely untouched.
 */
@OptIn(ExperimentalFoundationApi::class)
class RegexInputTransformation(private val regex: Regex) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        if (!asCharSequence().matches(regex)) {
            revertAllChanges()
        }
    }
}

/**
 * Keeps only ASCII digits up to [maxDigits], safe for both keystrokes and pasting.
 */
@OptIn(ExperimentalFoundationApi::class)
class DigitsMaxLengthTransformation(private val maxDigits: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        val raw = asCharSequence().toString()
        val filtered = raw.filter { it.isDigit() }.take(maxDigits)
        if (filtered != raw) {
            replace(0, length, filtered)
        }
    }
}

/**
 * Uppercases and caps text at [maxChars] (used for CAPTCHA verification inputs).
 */
@OptIn(ExperimentalFoundationApi::class)
class UppercaseMaxLengthTransformation(private val maxChars: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (changes.changeCount == 0) return
        val raw = asCharSequence().toString()
        val formatted = raw.uppercase().take(maxChars)
        if (formatted != raw) {
            replace(0, length, formatted)
        }
    }
}
