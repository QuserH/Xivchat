package com.quserh.eorzeaphone.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneText
import java.math.BigDecimal
import java.math.RoundingMode

private enum class CalcOp(val symbol: String) {
    None(""), Add("+"), Subtract("−"), Multiply("×"), Divide("÷")
}

private data class CalcHistoryEntry(val expression: String, val result: String)

private class PhoneCalculatorEngine {
    companion object {
        private const val MaxDigits = 9
        private const val MaxHistory = 50
    }

    private var accumulator = 0.0
    private var lastOperand = 0.0
    private var pending = CalcOp.None
    private var freshEntry = true
    private var justEvaluated = false
    private var error = false
    private var expressionPrefix = ""
    private var lastExpression = ""

    var display by mutableStateOf("0")
        private set
    val history = mutableStateListOf<CalcHistoryEntry>()
    val activeOperator: CalcOp get() = if (pending != CalcOp.None && freshEntry && !justEvaluated) pending else CalcOp.None
    val showAllClear: Boolean get() = display == "0" && !error
    val expression: String
        get() = when {
            justEvaluated -> if (lastExpression.isBlank()) "" else "$lastExpression ="
            expressionPrefix.isBlank() && freshEntry -> ""
            else -> (expressionPrefix + if (freshEntry) "" else display).trimEnd()
        }

    fun inputDigit(digit: Int) {
        if (error) reset()
        startFreshAfterEvaluation()
        if (freshEntry) {
            display = digit.toString()
            freshEntry = false
        } else if (display == "0") {
            display = digit.toString()
        } else if (display == "-0") {
            display = "-$digit"
        } else if (display.count(Char::isDigit) < MaxDigits) {
            display += digit
        }
    }

    fun inputDecimal() {
        if (error) reset()
        startFreshAfterEvaluation()
        if (freshEntry) {
            display = "0."
            freshEntry = false
        } else if ('.' !in display) display += "."
    }

    fun setOperator(op: CalcOp) {
        if (error) return
        if (justEvaluated) {
            expressionPrefix = "$display ${op.symbol} "
            accumulator = parse(display)
            pending = op
            freshEntry = true
            justEvaluated = false
            lastExpression = ""
            return
        }
        if (freshEntry && pending != CalcOp.None) {
            pending = op
            expressionPrefix = expressionPrefix.trimEnd().substringBeforeLast(' ', "") + " ${op.symbol} "
            return
        }
        val entered = display
        accumulator = if (pending == CalcOp.None) parse(display) else apply(accumulator, pending, parse(display))
        if (error) return
        display = format(accumulator)
        expressionPrefix += "$entered ${op.symbol} "
        pending = op
        freshEntry = true
    }

    fun equals() {
        if (error || pending == CalcOp.None) {
            justEvaluated = true
            freshEntry = true
            return
        }
        val operand = if (freshEntry) lastOperand else parse(display)
        val entered = if (freshEntry) format(lastOperand) else display
        lastOperand = operand
        val fullExpression = expressionPrefix + entered
        accumulator = apply(accumulator, pending, operand)
        display = format(accumulator)
        lastExpression = fullExpression
        if (!error) {
            history.add(0, CalcHistoryEntry(fullExpression, display))
            if (history.size > MaxHistory) history.removeAt(history.lastIndex)
        }
        expressionPrefix = ""
        pending = CalcOp.None
        freshEntry = true
        justEvaluated = true
    }

    fun negate() {
        if (error) return
        display = if (display.startsWith('-')) display.drop(1) else if (display == "0") display else "-$display"
    }

    fun percent() {
        if (error) return
        display = format(parse(display) / 100.0)
        justEvaluated = false
    }

    fun clear() {
        if (display != "0") {
            display = "0"
            freshEntry = true
            justEvaluated = false
        } else reset()
    }

    fun recall(result: String) {
        display = result
        accumulator = parse(result)
        expressionPrefix = ""
        lastExpression = ""
        pending = CalcOp.None
        freshEntry = true
        justEvaluated = false
        error = false
    }

    private fun startFreshAfterEvaluation() {
        if (!justEvaluated) return
        accumulator = 0.0
        pending = CalcOp.None
        expressionPrefix = ""
        lastExpression = ""
        justEvaluated = false
        freshEntry = true
    }

    private fun reset() {
        accumulator = 0.0
        lastOperand = 0.0
        pending = CalcOp.None
        display = "0"
        expressionPrefix = ""
        lastExpression = ""
        freshEntry = true
        justEvaluated = false
        error = false
    }

    private fun apply(left: Double, op: CalcOp, right: Double): Double = when (op) {
        CalcOp.Add -> left + right
        CalcOp.Subtract -> left - right
        CalcOp.Multiply -> left * right
        CalcOp.Divide -> if (right == 0.0) { error = true; 0.0 } else left / right
        CalcOp.None -> right
    }

    private fun format(value: Double): String {
        if (error || !value.isFinite()) { error = true; return "错误" }
        val rounded = BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()
    }

    private fun parse(text: String): Double = text.toDoubleOrNull() ?: 0.0
}

@Composable
fun CalculatorScreen(state: PhoneState) {
    val engine = remember { PhoneCalculatorEngine() }
    ScreenFrame {
        ScreenHeader("计算器", state, showBack = false)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (engine.history.isNotEmpty()) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
                    items(engine.history.take(8).reversed()) { entry ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { engine.recall(entry.result) }.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(entry.expression, color = PhoneMuted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(entry.result, color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().animateContentSize().padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.End) {
                if (engine.expression.isNotBlank()) Text(engine.expression, color = PhoneMuted, fontSize = 18.sp, maxLines = 1)
                Text(engine.display, color = PhoneText, fontSize = if (engine.display.length > 9) 42.sp else 58.sp, fontWeight = FontWeight.Normal, maxLines = 1, textAlign = TextAlign.End)
            }
            CalculatorKeypad(engine)
        }
    }
}

@Composable
private fun CalculatorKeypad(engine: PhoneCalculatorEngine) {
    val rows = listOf(
        listOf("AC", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label -> CalcKey(label = if (label == "AC" && !engine.showAllClear) "C" else label, active = engine.activeOperator.symbol == label, modifier = Modifier.weight(1f)) { pressCalculatorKey(engine, label) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcKey("0", Modifier.weight(2f).aspectRatio(2.08f), pill = true) { engine.inputDigit(0) }
            CalcKey(".", Modifier.weight(1f)) { engine.inputDecimal() }
            CalcKey("=", Modifier.weight(1f), operator = true) { engine.equals() }
        }
    }
}

@Composable
private fun CalcKey(label: String, modifier: Modifier = Modifier, operator: Boolean = label in setOf("÷", "×", "−", "+", "="), active: Boolean = false, pill: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.66f, stiffness = 600f), label = "calculator-key")
    val function = label in setOf("AC", "C", "±", "%")
    val background = when { active -> Color.White; operator -> PhoneAccent; function -> Color(0xFFA4A4AA); else -> Color(0xFF38383D) }
    val ink = when { active -> PhoneAccent; function -> Color(0xFF101014); else -> Color.White }
    Box(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }.aspectRatio(if (pill) 2.08f else 1f).clip(if (pill) RoundedCornerShape(100) else CircleShape).background(background).clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = if (pill) Alignment.CenterStart else Alignment.Center,
    ) { Text(label, color = ink, fontSize = 25.sp, fontWeight = FontWeight.Medium, modifier = if (pill) Modifier.padding(start = 28.dp) else Modifier) }
}

private fun pressCalculatorKey(engine: PhoneCalculatorEngine, label: String) {
    when (label) {
        "AC" -> engine.clear()
        "±" -> engine.negate()
        "%" -> engine.percent()
        "÷" -> engine.setOperator(CalcOp.Divide)
        "×" -> engine.setOperator(CalcOp.Multiply)
        "−" -> engine.setOperator(CalcOp.Subtract)
        "+" -> engine.setOperator(CalcOp.Add)
        else -> label.toIntOrNull()?.let(engine::inputDigit)
    }
}
