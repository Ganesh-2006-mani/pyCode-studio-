package com.example.compiler

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class InterpreterState {
    object Idle : InterpreterState()
    object Running : InterpreterState()
    data class WaitingForInput(val prompt: String) : InterpreterState()
    data class Completed(val executionTimeMs: Long, val totalLinesRun: Int) : InterpreterState()
    data class Error(val errorName: String, val message: String, val line: Int, val surroundingCode: String) : InterpreterState()
}

sealed class ConsoleOutput {
    data class Standard(val text: String) : ConsoleOutput()
    data class InputPrompt(val text: String) : ConsoleOutput()
    data class Error(val text: String) : ConsoleOutput()
    data class System(val text: String) : ConsoleOutput()
}

class PythonInterpreter(
    private val onOutput: (ConsoleOutput) -> Unit,
    private val onStateChange: (InterpreterState) -> Unit
) {
    private val globalVariables = mutableMapOf<String, Any?>()
    private val functions = mutableMapOf<String, PythonFunction>()
    private var inputDeferred: CompletableDeferred<String>? = null
    private var currentLineIndex = 0
    private var isTerminated = false

    data class PythonFunction(
        val name: String,
        val params: List<String>,
        val bodyLines: List<String>,
        val startLineNum: Int
    )

    fun terminate() {
        isTerminated = true
        inputDeferred?.complete("")
    }

    suspend fun provideInput(input: String) {
        onOutput(ConsoleOutput.Standard(input))
        inputDeferred?.complete(input)
    }

    suspend fun executeCode(code: String) = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        globalVariables.clear()
        functions.clear()
        isTerminated = false
        currentLineIndex = 0

        onStateChange(InterpreterState.Running)
        onOutput(ConsoleOutput.System(">>> Python execution started (Offline Mode) <<<"))

        val lines = code.split("\n")
        val parsedStatements = parseBlocks(lines, 0)

        try {
            executeStatements(parsedStatements, globalVariables)
            if (!isTerminated) {
                val duration = System.currentTimeMillis() - startTime
                onStateChange(InterpreterState.Completed(duration, lines.size))
                onOutput(ConsoleOutput.System("\n>>> Execution and parsing completed in ${duration}ms <<<"))
            }
        } catch (e: PythonException) {
            onStateChange(InterpreterState.Error(e.errorName, e.message ?: "Unknown runtime error", e.lineNum, lines.getOrNull(e.lineNum - 1)?.trim() ?: ""))
            onOutput(ConsoleOutput.Error("${e.errorName} (line ${e.lineNum}): ${e.message}"))
        } catch (e: Exception) {
            val lineNum = currentLineIndex + 1
            onStateChange(InterpreterState.Error("RuntimeError", e.message ?: "Native system crash", lineNum, lines.getOrNull(currentLineIndex)?.trim() ?: ""))
            onOutput(ConsoleOutput.Error("RuntimeError (line $lineNum): ${e.message}"))
        }
    }

    private suspend fun executeStatements(statements: List<Statement>, localScope: MutableMap<String, Any?>) {
        for (statement in statements) {
            if (isTerminated) return
            currentLineIndex = statement.lineNum - 1
            
            when (statement) {
                is Statement.Assignment -> {
                    val value = evaluateExpression(statement.expression, localScope)
                    if (statement.target.contains("[")) {
                        // handling list assignments e.g. items[0] = value
                        assignToListIndex(statement.target, value, localScope)
                    } else {
                        localScope[statement.target] = value
                    }
                }
                is Statement.Print -> {
                    val outputs = statement.expressions.map { eval ->
                        val result = evaluateExpression(eval, localScope)
                        stringifyValue(result)
                    }
                    onOutput(ConsoleOutput.Standard(outputs.joinToString(" ")))
                }
                is Statement.IfBlock -> {
                    var conditionMet = false
                    for (branch in statement.branches) {
                        val condValue = evaluateExpression(branch.condition, localScope)
                        if (isTruthy(condValue)) {
                            executeStatements(branch.body, localScope)
                            conditionMet = true
                            break
                        }
                    }
                    if (!conditionMet && statement.elseBody != null) {
                        executeStatements(statement.elseBody, localScope)
                    }
                }
                is Statement.WhileBlock -> {
                    var iterationLimit = 5000 // Infinite loop safety threshold
                    while (isTruthy(evaluateExpression(statement.condition, localScope)) && !isTerminated) {
                        executeStatements(statement.body, localScope)
                        iterationLimit--
                        if (iterationLimit <= 0) {
                            throw PythonException("InfiniteLoopError", "Safety break triggered: loop exceeded 5000 iterations.", statement.lineNum)
                        }
                    }
                }
                is Statement.ForBlock -> {
                    val iterable = evaluateExpression(statement.iterableExpr, localScope)
                    val loopItems = evaluateIterable(iterable, statement.lineNum)
                    
                    var iterationLimit = 5000 // Infinite loop safety threshold
                    for (item in loopItems) {
                        if (isTerminated) break
                        localScope[statement.loopVar] = item
                        executeStatements(statement.body, localScope)
                        iterationLimit--
                        if (iterationLimit <= 0) {
                            throw PythonException("InfiniteLoopError", "Safety break triggered: for-loop iterator safety exception.", statement.lineNum)
                        }
                    }
                }
                is Statement.FunctionDef -> {
                    functions[statement.name] = PythonFunction(
                        name = statement.name,
                        params = statement.params,
                        bodyLines = statement.bodyLines,
                        startLineNum = statement.lineNum
                    )
                }
                is Statement.ExpressionCall -> {
                    evaluateExpression(statement.exprString, localScope)
                }
                is Statement.ReturnStatement -> {
                    val retVal = statement.expression?.let { evaluateExpression(it, localScope) }
                    throw ReturnSignal(retVal)
                }
                is Statement.Pass -> {
                    // Do nothing
                }
            }
        }
    }

    private suspend fun evaluateExpression(exprRaw: String, localScope: MutableMap<String, Any?>): Any? {
        val expr = exprRaw.trim()
        if (expr.isEmpty()) return null

        // Literals
        if (expr == "None") return null
        if (expr == "True") return true
        if (expr == "False") return false
        if (expr.startsWith("\"") && expr.endsWith("\"")) return expr.removeSurrounding("\"")
        if (expr.startsWith("'") && expr.endsWith("'")) return expr.removeSurrounding("'")

        // Number check
        val intVal = expr.toIntOrNull()
        if (intVal != null) return intVal
        val doubleVal = expr.toDoubleOrNull()
        if (doubleVal != null) return doubleVal

        // String Interpolation (f"...{var}...")
        if (expr.startsWith("f\"") && expr.endsWith("\"")) {
            return interpolateFString(expr.substring(2, expr.length - 1), localScope)
        }
        if (expr.startsWith("f'") && expr.endsWith("'")) {
            return interpolateFString(expr.substring(2, expr.length - 1), localScope)
        }

        // List creation [a, b, c]
        if (expr.startsWith("[") && expr.endsWith("]")) {
            val content = expr.substring(1, expr.length - 1).trim()
            if (content.isEmpty()) return mutableListOf<Any?>()
            val parsedElements = splitByCommaTopLevel(content).map { evaluateExpression(it, localScope) }
            return parsedElements.toMutableList()
        }

        // Built-in operations & lists append e.g. list.append(val)
        if (expr.contains(".append(") && expr.endsWith(")")) {
            val index = expr.indexOf(".append(")
            val listName = expr.substring(0, index).trim()
            val arg = expr.substring(index + 8, expr.length - 1).trim()
            val listObj = resolveVariable(listName, localScope, currentLineIndex + 1)
            if (listObj is MutableList<*>) {
                val valueToAppend = evaluateExpression(arg, localScope)
                @Suppress("UNCHECKED_CAST")
                (listObj as MutableList<Any?>).add(valueToAppend)
            } else {
                throw PythonException("AttributeError", "'${listObj?.javaClass?.simpleName ?: "None"}' object has no attribute 'append'", currentLineIndex + 1)
            }
            return null
        }

        // Function Calls or builtins
        if (expr.contains("(") && expr.endsWith(")")) {
            val firstParen = expr.indexOf("(")
            val funcName = expr.substring(0, firstParen).trim()
            val argsRaw = expr.substring(firstParen + 1, expr.length - 1)
            val args = splitByCommaTopLevel(argsRaw).map { evaluateExpression(it, localScope) }
            
            return evaluateFunctionCall(funcName, args, localScope)
        }

        // Boolean Operators: AND / OR
        if (expr.contains(" and ")) {
            val parts = expr.split(" and ", limit = 2)
            val left = evaluateExpression(parts[0], localScope)
            return if (!isTruthy(left)) left else evaluateExpression(parts[1], localScope)
        }
        if (expr.contains(" or ")) {
            val parts = expr.split(" or ", limit = 2)
            val left = evaluateExpression(parts[0], localScope)
            return if (isTruthy(left)) left else evaluateExpression(parts[1], localScope)
        }

        // Binary comparators
        val ops = listOf("==", "!=", "<=", ">=", "<", ">", "+", "-", "*", "/", "%", "**")
        for (op in ops) {
            val parts = splitExpressionByOperator(expr, op)
            if (parts != null && parts.size == 2) {
                val left = evaluateExpression(parts[0], localScope)
                val right = evaluateExpression(parts[1], localScope)
                return applyOperator(left, op, right, currentLineIndex + 1)
            }
        }

        // Index retrieval: items[0]
        if (expr.endsWith("]") && expr.contains("[")) {
            val firstBrack = expr.indexOf("[")
            val listName = expr.substring(0, firstBrack).trim()
            val indexExpr = expr.substring(firstBrack + 1, expr.length - 1).trim()
            val listObj = resolveVariable(listName, localScope, currentLineIndex + 1)
            val idx = evaluateExpression(indexExpr, localScope)
            if (listObj is List<*> && idx is Int) {
                if (idx >= 0 && idx < listObj.size) {
                    return listObj[idx]
                } else {
                    throw PythonException("IndexError", "list index out of range: $idx in size ${listObj.size}", currentLineIndex + 1)
                }
            } else {
                throw PythonException("TypeError", "indices must be integers, or object not indexable", currentLineIndex + 1)
            }
        }

        // Variable lookup
        return resolveVariable(expr, localScope, currentLineIndex + 1)
    }

    private fun resolveVariable(name: String, localScope: Map<String, Any?>, lineNum: Int): Any? {
        val trimmed = name.trim()
        if (localScope.containsKey(trimmed)) return localScope[trimmed]
        if (globalVariables.containsKey(trimmed)) return globalVariables[trimmed]
        
        throw PythonException("NameError", "name '$trimmed' is not defined", lineNum)
    }

    private fun assignToListIndex(target: String, value: Any?, localScope: MutableMap<String, Any?>) {
        val firstBrack = target.indexOf("[")
        val listName = target.substring(0, firstBrack).trim()
        val indexExpr = target.substring(firstBrack + 1, target.length - 1).trim()
        
        // Lookup list
        val listObj = if (localScope.containsKey(listName)) localScope[listName] else globalVariables[listName]
        if (listObj is MutableList<*>) {
            val idx = try {
                val evaluated = evaluateExpressionDirectSync(indexExpr, localScope)
                if (evaluated is Number) evaluated.toInt() else throw Exception()
            } catch (e: Exception) {
                throw PythonException("TypeError", "list index must be an integer", currentLineIndex + 1)
            }
            if (idx in 0 until listObj.size) {
                @Suppress("UNCHECKED_CAST")
                (listObj as MutableList<Any?>)[idx] = value
            } else {
                throw PythonException("IndexError", "list assignment index out of range", currentLineIndex + 1)
            }
        } else {
            throw PythonException("NameError", "name '$listName' is not a list or not defined", currentLineIndex + 1)
        }
    }

    private fun evaluateExpressionDirectSync(expr: String, localScope: Map<String, Any?>): Any? {
        val trimmed = expr.trim()
        if (trimmed == "True") return true
        if (trimmed == "False") return false
        val intV = trimmed.toIntOrNull()
        if (intV != null) return intV
        if (localScope.containsKey(trimmed)) return localScope[trimmed]
        if (globalVariables.containsKey(trimmed)) return globalVariables[trimmed]
        return trimmed
    }

    private suspend fun interpolateFString(content: String, localScope: MutableMap<String, Any?>): String {
        val result = java.lang.StringBuilder()
        var idx = 0
        while (idx < content.length) {
            val c = content[idx]
            if (c == '{') {
                val closeIdx = content.indexOf('}', idx)
                if (closeIdx != -1) {
                    val expr = content.substring(idx + 1, closeIdx)
                    try {
                        val eval = evaluateExpression(expr, localScope)
                        if (eval is Double && expr.contains(":.2f")) {
                            result.append(java.lang.String.format(java.util.Locale.US, "%.2f", eval))
                        } else {
                            result.append(stringifyValue(eval))
                        }
                    } catch (e: Exception) {
                        result.append("{$expr}")
                    }
                    idx = closeIdx + 1
                } else {
                    result.append(c)
                    idx++
                }
            } else {
                result.append(c)
                idx++
            }
        }
        return result.toString()
    }

    private suspend fun evaluateFunctionCall(name: String, args: List<Any?>, localScope: MutableMap<String, Any?>): Any? {
        when (name) {
            "int" -> {
                val first = args.firstOrNull() ?: return 0
                if (first is Number) return first.toInt()
                return first.toString().toIntOrNull() ?: throw PythonException("ValueError", "invalid literal for int(): $first", currentLineIndex + 1)
            }
            "str" -> return stringifyValue(args.firstOrNull())
            "float" -> {
                val first = args.firstOrNull() ?: return 0.0
                if (first is Number) return first.toDouble()
                return first.toString().toDoubleOrNull() ?: throw PythonException("ValueError", "invalid literal for float(): $first", currentLineIndex + 1)
            }
            "len" -> {
                val first = args.firstOrNull() ?: return 0
                if (first is List<*>) return first.size
                if (first is Map<*, *>) return first.size
                if (first is String) return first.length
                throw PythonException("TypeError", "object of type '${first.javaClass.simpleName}' has no len()", currentLineIndex + 1)
            }
            "abs" -> {
                val first = args.firstOrNull() as? Number ?: throw PythonException("TypeError", "abs() expects a number", currentLineIndex + 1)
                return if (first is Double) Math.abs(first.toDouble()) else Math.abs(first.toLong()).toInt()
            }
            "range" -> {
                val start = if (args.size > 1) (args[0] as? Number)?.toInt() ?: 0 else 0
                val end = if (args.size > 1) (args[1] as? Number)?.toInt() ?: 0 else (args.firstOrNull() as? Number)?.toInt() ?: 0
                val step = if (args.size > 2) (args[2] as? Number)?.toInt() ?: 1 else 1
                return (start until end step step).toList()
            }
            "input" -> {
                val prompt = args.firstOrNull()?.toString() ?: ""
                withContext(Dispatchers.Main) {
                    onOutput(ConsoleOutput.InputPrompt(prompt))
                    onStateChange(InterpreterState.WaitingForInput(prompt))
                }
                
                inputDeferred = CompletableDeferred()
                val userInput = inputDeferred!!.await()
                inputDeferred = null
                
                withContext(Dispatchers.Main) {
                    onStateChange(InterpreterState.Running)
                }
                return userInput
            }
            "type" -> {
                val arg = args.firstOrNull() ?: return "NoneType"
                return when (arg) {
                    is Int -> "int"
                    is Double -> "float"
                    is String -> "str"
                    is Boolean -> "bool"
                    is List<*> -> "list"
                    else -> arg.javaClass.simpleName
                }
            }
            "sum" -> {
                val list = args.firstOrNull() as? List<*> ?: throw PythonException("TypeError", "sum() expects a list of numbers", currentLineIndex + 1)
                return list.filterIsInstance<Number>().sumOf { it.toDouble() }
            }
            "max" -> {
                val list = args.firstOrNull() as? List<*> ?: throw PythonException("TypeError", "max() expects a list", currentLineIndex + 1)
                return list.filterIsInstance<Number>().maxOfOrNull { it.toDouble() } ?: 0
            }
            "min" -> {
                val list = args.firstOrNull() as? List<*> ?: throw PythonException("TypeError", "min() expects a list", currentLineIndex + 1)
                return list.filterIsInstance<Number>().minOfOrNull { it.toDouble() } ?: 0
            }
        }

        // Check custom defined function
        val customFunc = functions[name]
        if (customFunc != null) {
            val funcScope = mutableMapOf<String, Any?>()
            // Bind parameters
            for (i in customFunc.params.indices) {
                val paramName = customFunc.params[i]
                val paramValue = args.getOrNull(i)
                funcScope[paramName] = paramValue
            }
            
            // Execute statements
            val bodyStatements = parseBlocks(customFunc.bodyLines, customFunc.startLineNum)
            return try {
                executeStatements(bodyStatements, funcScope)
                null
            } catch (ret: ReturnSignal) {
                ret.value
            }
        }

        throw PythonException("NameError", "function '$name' is not defined offline", currentLineIndex + 1)
    }

    private fun applyOperator(left: Any?, op: String, right: Any?, lineNum: Int): Any? {
        if (left is String || right is String) {
            if (op == "+") {
                return stringifyValue(left) + stringifyValue(right)
            }
            if (op == "*" && (left is Int || right is Int)) {
                val str = if (left is String) left else right as String
                val times = if (left is Int) left else right as Int
                return str.repeat(times.coerceAtLeast(0))
            }
        }

        if (left is Number && right is Number) {
            val l = left.toDouble()
            val r = right.toDouble()
            
            val isFloatResult = left is Double || right is Double || op == "/"
            
            return when (op) {
                "+" -> if (isFloatResult) l + r else (left.toLong() + right.toLong()).toInt()
                "-" -> if (isFloatResult) l - r else (left.toLong() - right.toLong()).toInt()
                "*" -> if (isFloatResult) l * r else (left.toLong() * right.toLong()).toInt()
                "/" -> {
                    if (r == 0.0) throw PythonException("ZeroDivisionError", "division by zero", lineNum)
                    l / r
                }
                "%" -> {
                    if (r == 0.0) throw PythonException("ZeroDivisionError", "integer division or modulo by zero", lineNum)
                    if (isFloatResult) l % r else (left.toLong() % right.toLong()).toInt()
                }
                "**" -> {
                    val result = Math.pow(l, r)
                    if (isFloatResult) result else result.toInt()
                }
                "==" -> l == r
                "!=" -> l != r
                "<" -> l < r
                ">" -> l > r
                "<=" -> l <= r
                ">=" -> l >= r
                else -> throw PythonException("TypeError", "Unsupported operator '$op' for numbers", lineNum)
            }
        }

        // Objects/Booleans comparison
        return when (op) {
            "==" -> left == right
            "!=" -> left != right
            else -> throw PythonException("TypeError", "unsupported operand type(s) for $op: '${left?.javaClass?.simpleName ?: "NoneType"}' and '${right?.javaClass?.simpleName ?: "NoneType"}'", lineNum)
        }
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        if (value is Number) return value.toDouble() != 0.0
        if (value is String) return value.isNotEmpty()
        if (value is List<*>) return value.isNotEmpty()
        return true
    }

    private fun evaluateIterable(iterable: Any?, lineNum: Int): List<Any?> {
        if (iterable is List<*>) return iterable.map { it }
        if (iterable is String) return iterable.map { it.toString() }
        throw PythonException("TypeError", "'${iterable?.javaClass?.simpleName ?: "NoneType"}' object is not iterable", lineNum)
    }

    private fun stringifyValue(value: Any?): String {
        if (value == null) return "None"
        if (value is Boolean) return if (value) "True" else "False"
        if (value is List<*>) {
            return "[" + value.joinToString(", ") { stringifyValue(it) } + "]"
        }
        return value.toString()
    }

    private fun splitByCommaTopLevel(content: String): List<String> {
        val result = mutableListOf<String>()
        var bracketLevel = 0
        var parenLevel = 0
        var inQuotes = false
        var quoteChar = ' '
        var currentToken = StringBuilder()

        var idx = 0
        while (idx < content.length) {
            val c = content[idx]
            if (inQuotes) {
                currentToken.append(c)
                if (c == quoteChar && (idx == 0 || content[idx - 1] != '\\')) {
                    inQuotes = false
                }
            } else {
                when (c) {
                    '"', '\'' -> {
                        inQuotes = true
                        quoteChar = c
                        currentToken.append(c)
                    }
                    '[' -> {
                        bracketLevel++
                        currentToken.append(c)
                    }
                    ']' -> {
                        bracketLevel--
                        currentToken.append(c)
                    }
                    '(' -> {
                        parenLevel++
                        currentToken.append(c)
                    }
                    ')' -> {
                        parenLevel--
                        currentToken.append(c)
                    }
                    ',' -> {
                        if (bracketLevel == 0 && parenLevel == 0) {
                            result.add(currentToken.toString().trim())
                            currentToken = StringBuilder()
                        } else {
                            currentToken.append(c)
                        }
                    }
                    else -> currentToken.append(c)
                }
            }
            idx++
        }
        if (currentToken.isNotEmpty()) {
            result.add(currentToken.toString().trim())
        }
        return result
    }

    private fun splitExpressionByOperator(expr: String, op: String): List<String>? {
        var bracketLevel = 0
        var parenLevel = 0
        var inQuotes = false
        var quoteChar = ' '

        var idx = expr.length - 1
        while (idx >= 0) {
            val c = expr[idx]
            if (inQuotes) {
                if (c == quoteChar && (idx == 0 || expr[idx - 1] != '\\')) {
                    inQuotes = false
                }
            } else {
                when (c) {
                    '"', '\'' -> {
                        inQuotes = true
                        quoteChar = c
                    }
                    ']' -> bracketLevel++
                    '[' -> bracketLevel--
                    ')' -> parenLevel++
                    '(' -> parenLevel--
                }
                
                if (bracketLevel == 0 && parenLevel == 0 && !inQuotes) {
                    // Check if current position matches operator
                    val opLen = op.length
                    if (idx - opLen + 1 >= 0) {
                        val substring = expr.substring(idx - opLen + 1, idx + 1)
                        if (substring == op) {
                            // Extra validation to distinguish comparator '=' from assignment
                            if (op == "==" || (op != "==" && indexIsNotPartOfDubleOp(expr, idx, op))) {
                                val left = expr.substring(0, idx - opLen + 1)
                                val right = expr.substring(idx + 1)
                                return listOf(left, right)
                            }
                        }
                    }
                }
            }
            idx--
        }
        return null
    }

    private fun indexIsNotPartOfDubleOp(expr: String, idx: Int, op: String): Boolean {
        if (op == "+" || op == "-" || op == "*" || op == "/" || op == "%") {
            // make sure not inside double operator like **
            if (op == "*" && idx > 0 && expr[idx - 1] == '*') return false
            if (op == "*" && idx < expr.length - 1 && expr[idx + 1] == '*') return false
        }
        if (op == "<" || op == ">") {
            if (idx < expr.length - 1 && expr[idx + 1] == '=') return false
        }
        return true
    }

    private fun parseBlocks(lines: List<String>, baseLineOffset: Int): List<Statement> {
        val statements = mutableListOf<Statement>()
        var idx = 0
        
        while (idx < lines.size) {
            val originalLine = lines[idx]
            val trimmed = originalLine.trim()
            val absoluteLineNumber = baseLineOffset + idx + 1

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                idx++
                continue
            }

            val indent = getIndentation(originalLine)
            
            // IF statement block
            if (trimmed.startsWith("if ") && trimmed.endsWith(":")) {
                val cond = trimmed.substring(3, trimmed.length - 1).trim()
                val (bodyLines, consumedLinesCount) = collectIndentedBlock(lines, idx + 1, indent)
                val body = parseBlocks(bodyLines, absoluteLineNumber)
                
                // Collect ELIF / ELSE branches
                val branches = mutableListOf<IfBranch>()
                var currentSeek = idx + consumedLinesCount + 1
                var hasElse = false
                val elseBody = mutableListOf<Statement>()
                
                while (currentSeek < lines.size) {
                    val nextRawLine = lines[currentSeek]
                    val nextTrim = nextRawLine.trim()
                    if (nextTrim.isEmpty() || nextTrim.startsWith("#")) {
                        currentSeek++
                        continue
                    }
                    val nextIndent = getIndentation(nextRawLine)
                    if (nextIndent != indent) break
                    
                    if (nextTrim.startsWith("elif ") && nextTrim.endsWith(":")) {
                        val elifCond = nextTrim.substring(5, nextTrim.length - 1).trim()
                        val (elifBodyLines, elifConsumed) = collectIndentedBlock(lines, currentSeek + 1, indent)
                        branches.add(IfBranch(elifCond, parseBlocks(elifBodyLines, baseLineOffset + currentSeek + 1)))
                        currentSeek += elifConsumed + 1
                    } else if (nextTrim.startsWith("else:") || nextTrim.startsWith("else :")) {
                        val (elseBodyLines, elseConsumed) = collectIndentedBlock(lines, currentSeek + 1, indent)
                        elseBody.addAll(parseBlocks(elseBodyLines, baseLineOffset + currentSeek + 1))
                        currentSeek += elseConsumed + 1
                        hasElse = true
                        break
                    } else {
                        break
                    }
                }
                
                statements.add(Statement.IfBlock(
                    condition = cond,
                    branches = listOf(IfBranch(cond, body)) + branches,
                    elseBody = if (hasElse) elseBody else null,
                    lineNum = absoluteLineNumber
                ))
                idx = currentSeek
                continue
            }

            // WHILE statement block
            if (trimmed.startsWith("while ") && trimmed.endsWith(":")) {
                val cond = trimmed.substring(6, trimmed.length - 1).trim()
                val (bodyLines, consumed) = collectIndentedBlock(lines, idx + 1, indent)
                statements.add(Statement.WhileBlock(
                    condition = cond,
                    body = parseBlocks(bodyLines, absoluteLineNumber),
                    lineNum = absoluteLineNumber
                ))
                idx += consumed + 1
                continue
            }

            // FOR statement block
            if (trimmed.startsWith("for ") && trimmed.endsWith(":")) {
                val loopParts = trimmed.substring(4, trimmed.length - 1).trim()
                if (loopParts.contains(" in ")) {
                    val inIndex = loopParts.indexOf(" in ")
                    val loopVar = loopParts.substring(0, inIndex).trim()
                    val iterableExpr = loopParts.substring(inIndex + 4).trim()
                    val (bodyLines, consumed) = collectIndentedBlock(lines, idx + 1, indent)
                    statements.add(Statement.ForBlock(
                        loopVar = loopVar,
                        iterableExpr = iterableExpr,
                        body = parseBlocks(bodyLines, absoluteLineNumber),
                        lineNum = absoluteLineNumber
                    ))
                    idx += consumed + 1
                    continue
                }
            }

            // DEF Function definition block
            if (trimmed.startsWith("def ") && trimmed.endsWith(":")) {
                val funcParts = trimmed.substring(4, trimmed.length - 1).trim()
                val firstParen = funcParts.indexOf("(")
                if (firstParen != -1 && funcParts.endsWith(")")) {
                    val name = funcParts.substring(0, firstParen).trim()
                    val paramsRaw = funcParts.substring(firstParen + 1, funcParts.length - 1)
                    val params = paramsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val (bodyLines, consumed) = collectIndentedBlock(lines, idx + 1, indent)
                    
                    statements.add(Statement.FunctionDef(
                        name = name,
                        params = params,
                        bodyLines = bodyLines,
                        lineNum = absoluteLineNumber
                    ))
                    idx += consumed + 1
                    continue
                }
            }

            // PASS statement
            if (trimmed == "pass") {
                statements.add(Statement.Pass(absoluteLineNumber))
                idx++
                continue
            }

            // RETURN statement
            if (trimmed.startsWith("return")) {
                val expr = trimmed.substring(6).trim().ifEmpty { null }
                statements.add(Statement.ReturnStatement(expr, absoluteLineNumber))
                idx++
                continue
            }

            // PRINT statement
            if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val inner = trimmed.substring(6, trimmed.length - 1)
                val expressions = splitByCommaTopLevel(inner)
                statements.add(Statement.Print(expressions, absoluteLineNumber))
                idx++
                continue
            }

            // ASSIGNMENT statement (e.g. x = y)
            val eqIdx = findTopLevelEquals(trimmed)
            if (eqIdx != -1) {
                val target = trimmed.substring(0, eqIdx).trim()
                val expr = trimmed.substring(eqIdx + 1).trim()
                statements.add(Statement.Assignment(target, expr, absoluteLineNumber))
                idx++
                continue
            }

            // Lone expression calls e.g. run() or items.append()
            statements.add(Statement.ExpressionCall(trimmed, absoluteLineNumber))
            idx++
        }
        return statements
    }

    private fun findTopLevelEquals(line: String): Int {
        var bracketLevel = 0
        var parenLevel = 0
        var inQuotes = false
        var quoteChar = ' '

        var idx = 0
        while (idx < line.length) {
            val c = line[idx]
            if (inQuotes) {
                if (c == quoteChar && (idx == 0 || line[idx - 1] != '\\')) {
                    inQuotes = false
                }
            } else {
                when (c) {
                    '"', '\'' -> {
                        inQuotes = true
                        quoteChar = c
                    }
                    '[' -> bracketLevel++
                    ']' -> bracketLevel--
                    '(' -> parenLevel++
                    ')' -> parenLevel--
                    '=' -> {
                        if (bracketLevel == 0 && parenLevel == 0) {
                            // Check if it is a mathematical comparator '==', '<=', etc.
                            if (idx < line.length - 1 && line[idx + 1] == '=') {
                                idx++ // Skip next '='
                            } else if (idx > 0 && (line[idx - 1] == '=' || line[idx - 1] == '!' || line[idx - 1] == '<' || line[idx - 1] == '>')) {
                                // Part of comparator operator
                            } else {
                                return idx
                            }
                        }
                    }
                }
            }
            idx++
        }
        return -1
    }

    private fun getIndentation(line: String): Int {
        var count = 0
        for (char in line) {
            if (char == ' ') count++
            else if (char == '\t') count += 4
            else break
        }
        return count
    }

    private fun collectIndentedBlock(lines: List<String>, startIdx: Int, parentIndent: Int): Pair<List<String>, Int> {
        val body = mutableListOf<String>()
        var idx = startIdx
        var collectedCount = 0

        while (idx < lines.size) {
            val line = lines[idx]
            val trimLine = line.trim()
            
            if (trimLine.isEmpty()) {
                body.add(line) // Keep empty lines to preserve indexing alignment
                collectedCount++
                idx++
                continue
            }

            val indent = getIndentation(line)
            if (indent <= parentIndent) {
                // If it is non-empty and has less/equal indentation, it ends the block
                if (trimLine.isNotEmpty() && !trimLine.startsWith("#")) {
                    break
                }
            }

            body.add(line)
            collectedCount++
            idx++
        }
        
        // Trim trailing empty lines from structural verification
        while (body.isNotEmpty() && body.last().trim().isEmpty()) {
            body.removeAt(body.size - 1)
            collectedCount--
        }

        return Pair(body, collectedCount.coerceAtLeast(0))
    }
}

sealed class Statement(open val lineNum: Int) {
    data class Assignment(val target: String, val expression: String, override val lineNum: Int) : Statement(lineNum)
    data class Print(val expressions: List<String>, override val lineNum: Int) : Statement(lineNum)
    data class IfBlock(val condition: String, val branches: List<IfBranch>, val elseBody: List<Statement>?, override val lineNum: Int) : Statement(lineNum)
    data class WhileBlock(val condition: String, val body: List<Statement>, override val lineNum: Int) : Statement(lineNum)
    data class ForBlock(val loopVar: String, val iterableExpr: String, val body: List<Statement>, override val lineNum: Int) : Statement(lineNum)
    data class FunctionDef(val name: String, val params: List<String>, val bodyLines: List<String>, override val lineNum: Int) : Statement(lineNum)
    data class ReturnStatement(val expression: String?, override val lineNum: Int) : Statement(lineNum)
    data class ExpressionCall(val exprString: String, override val lineNum: Int) : Statement(lineNum)
    data class Pass(override val lineNum: Int) : Statement(lineNum)
}

data class IfBranch(val condition: String, val body: List<Statement>)

class PythonException(val errorName: String, message: String, val lineNum: Int) : Exception(message)
class ReturnSignal(val value: Any?) : Throwable()
