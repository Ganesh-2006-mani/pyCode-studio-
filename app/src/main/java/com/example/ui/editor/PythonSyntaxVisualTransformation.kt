package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.*

class PythonSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightPythonCode(text.text),
            OffsetMapping.Identity
        )
    }

    private fun highlightPythonCode(code: String): AnnotatedString {
        val builder = AnnotatedString.Builder(code)
        
        // Match lists
        val keywords = listOf(
            "def", "class", "if", "elif", "else", "for", "while", "in", 
            "return", "pass", "break", "continue", "import", "try", 
            "except", "and", "or", "not", "is", "as", "from", "with"
        )
        
        val builtins = listOf(
            "print", "input", "len", "range", "append", "int", "str", 
            "float", "type", "sum", "max", "min", "abs", "list", "dict"
        )

        // Parse line by line to handle comments easily
        val lines = code.split("\n")
        var overallIndex = 0

        for (line in lines) {
            val lineLength = line.length
            var commentIndex = line.indexOf('#')
            val endOfCodeIndex = if (commentIndex != -1) commentIndex else lineLength

            // Process code portion
            var idx = 0
            while (idx < endOfCodeIndex) {
                val char = line[idx]

                // Strings handling
                if (char == '"' || char == '\'') {
                    val quoteChar = char
                    val startQuote = idx
                    idx++
                    while (idx < endOfCodeIndex && line[idx] != quoteChar) {
                        if (line[idx] == '\\' && idx + 1 < endOfCodeIndex) {
                            idx += 2
                        } else {
                            idx++
                        }
                    }
                    if (idx < endOfCodeIndex) idx++ // include end quote
                    val endQuote = idx
                    builder.addStyle(
                        style = SpanStyle(color = SyntaxString),
                        start = overallIndex + startQuote,
                        end = overallIndex + endQuote
                    )
                    continue
                }

                // Numbers handling
                if (char.isDigit()) {
                    val startNum = idx
                    while (idx < endOfCodeIndex && (line[idx].isDigit() || line[idx] == '.')) {
                        idx++
                    }
                    builder.addStyle(
                        style = SpanStyle(color = SyntaxNumber),
                        start = overallIndex + startNum,
                        end = overallIndex + idx
                    )
                    continue
                }

                // Words (Keywords, variables, builtins)
                if (char.isLetter() || char == '_') {
                    val startWord = idx
                    while (idx < endOfCodeIndex && (line[idx].isLetterOrDigit() || line[idx] == '_')) {
                        idx++
                    }
                    val word = line.substring(startWord, idx)
                    if (keywords.contains(word)) {
                        builder.addStyle(
                            style = SpanStyle(color = SyntaxKeyword),
                            start = overallIndex + startWord,
                            end = overallIndex + idx
                        )
                    } else if (builtins.contains(word)) {
                        builder.addStyle(
                            style = SpanStyle(color = SyntaxBuiltIn),
                            start = overallIndex + startWord,
                            end = overallIndex + idx
                        )
                    } else {
                        builder.addStyle(
                            style = SpanStyle(color = SyntaxText),
                            start = overallIndex + startWord,
                            end = overallIndex + idx
                        )
                    }
                    continue
                }

                // Other symbols
                idx++
            }

            // Comment portion
            if (commentIndex != -1) {
                builder.addStyle(
                    style = SpanStyle(color = SyntaxComment),
                    start = overallIndex + commentIndex,
                    end = overallIndex + lineLength
                )
            }

            overallIndex += lineLength + 1 // +1 for the newline character
        }

        return builder.toAnnotatedString()
    }
}
