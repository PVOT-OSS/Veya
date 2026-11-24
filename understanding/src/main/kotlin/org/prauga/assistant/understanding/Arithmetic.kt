// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Exact decimal arithmetic from text such as "18% of 240", "(2 + 3) × 4", "square root of 81" or
 * "twelve divided by five". Operator words come from the lexicon; symbols work in every language.
 */
class Arithmetic(lexicon: Lexicon, private val numbers: Numbers = Numbers(lexicon)) {
    sealed interface Result {
        /** [operations] counts the operators used, so "42" alone can be told from a sum. */
        data class Value(val value: BigDecimal, val operations: Int) : Result

        data object DivideByZero : Result

        data object Undefined : Result
    }

    private enum class Op {
        PLUS,
        MINUS,
        TIMES,
        DIVIDE,
        POWER,
        PERCENT,
        OF,
        ROOT,
        SQUARED,
        CUBED,
        OPEN,
        CLOSE,
    }

    private sealed interface Token {
        class Number(val value: BigDecimal) : Token

        class Operator(val op: Op) : Token
    }

    private class Value(val number: BigDecimal, val percent: Boolean = false)

    private class Stop(val result: Result) : RuntimeException(null, null, false, false)

    private val words: List<Pair<WordTable, Op>> =
        listOf(
                "math.plus" to Op.PLUS,
                "math.minus" to Op.MINUS,
                "math.times" to Op.TIMES,
                "math.divide" to Op.DIVIDE,
                "math.power" to Op.POWER,
                "math.percent" to Op.PERCENT,
                "math.of" to Op.OF,
                "math.root" to Op.ROOT,
                "math.squared" to Op.SQUARED,
                "math.cubed" to Op.CUBED,
            )
            .map { (name, op) -> lexicon.optional(name) to op }
    private val fillers = lexicon.optional("math.filler")

    /** The value of [text], or null when it isn't arithmetic. */
    fun evaluate(text: String): Result? {
        val tokens = tokenize(text) ?: return null
        if (tokens.isEmpty()) return null
        val parser = Parser(tokens)
        return try {
            val value = parser.expression()
            if (!parser.done()) null else Result.Value(round(value.number), parser.operations)
        } catch (stop: Stop) {
            stop.result
        } catch (e: ArithmeticException) {
            Result.Undefined
        }
    }

    private fun tokenize(text: String): List<Token>? {
        val pieces = PIECE.findAll(GROUPING.replace(text.lowercase(), "")).map { it.value }.toList()
        val tokens = ArrayList<Token>(pieces.size)
        var index = 0
        while (index < pieces.size) {
            val piece = pieces[index]
            val symbol = SYMBOLS[piece]
            when {
                symbol != null -> {
                    tokens += Token.Operator(symbol)
                    index++
                }
                piece[0].isDigit() || piece[0] == '.' -> {
                    tokens += Token.Number(piece.toBigDecimalOrNull() ?: return null)
                    index++
                }
                else -> {
                    val filler = fillers.at(pieces, index)
                    val word =
                        words.firstNotNullOfOrNull { (table, op) ->
                            table.at(pieces, index)?.let { it to op }
                        }
                    val number = if (word == null) numbers.read(pieces, index) else null
                    when {
                        word != null &&
                            (filler == null || word.first.consumed >= filler.consumed) -> {
                            tokens += Token.Operator(word.second)
                            index += word.first.consumed
                        }
                        filler != null -> index += filler.consumed
                        number != null -> {
                            tokens += Token.Number(BigDecimal.valueOf(number.value))
                            index += number.consumed
                        }
                        else -> return null
                    }
                }
            }
        }
        return tokens
    }

    private inner class Parser(private val tokens: List<Token>) {
        private var at = 0
        var operations = 0
            private set

        fun done(): Boolean = at == tokens.size

        fun expression(): Value {
            var left = term()
            while (true) {
                val op = peek() ?: break
                if (op != Op.PLUS && op != Op.MINUS) break
                take()
                val right = term()
                val amount =
                    if (right.percent && !left.percent) left.number.multiply(right.number, MATH)
                    else right.number
                left =
                    Value(
                        if (op == Op.PLUS) left.number.add(amount, MATH)
                        else left.number.subtract(amount, MATH)
                    )
            }
            return left
        }

        private fun term(): Value {
            var left = power()
            while (true) {
                val op = peek() ?: break
                if (op != Op.TIMES && op != Op.DIVIDE && op != Op.OF) break
                take()
                val right = power()
                left =
                    if (op == Op.DIVIDE) {
                        if (right.number.signum() == 0) throw Stop(Result.DivideByZero)
                        Value(left.number.divide(right.number, MATH))
                    } else {
                        Value(left.number.multiply(right.number, MATH))
                    }
            }
            return left
        }

        private fun power(): Value {
            val base = unary()
            if (peek() != Op.POWER) return base
            take()
            val exponent = power().number
            val whole =
                exponent.stripTrailingZeros().takeIf { it.scale() <= 0 }?.toInt()
                    ?: throw Stop(Result.Undefined)
            if (whole !in -MAX_EXPONENT..MAX_EXPONENT) throw Stop(Result.Undefined)
            if (whole < 0 && base.number.signum() == 0) throw Stop(Result.DivideByZero)
            return Value(base.number.pow(whole, MATH))
        }

        private fun unary(): Value =
            when (peek()) {
                Op.MINUS -> {
                    take()
                    Value(unary().number.negate())
                }
                Op.PLUS -> {
                    take()
                    unary()
                }
                Op.ROOT -> {
                    take()
                    Value(root(unary().number))
                }
                else -> postfix()
            }

        private fun postfix(): Value {
            var value = primary()
            while (true) {
                value =
                    when (peek()) {
                        Op.SQUARED -> Value(value.number.pow(2, MATH))
                        Op.CUBED -> Value(value.number.pow(3, MATH))
                        Op.PERCENT -> Value(value.number.movePointLeft(2), percent = true)
                        else -> return value
                    }
                take()
            }
        }

        private fun primary(): Value {
            val token = tokens.getOrNull(at) ?: throw Stop(Result.Undefined)
            if (token is Token.Number) {
                at++
                return Value(token.value)
            }
            if ((token as Token.Operator).op != Op.OPEN) throw Stop(Result.Undefined)
            at++
            val inner = expression()
            if (peek() != Op.CLOSE) throw Stop(Result.Undefined)
            at++
            return inner
        }

        private fun peek(): Op? = (tokens.getOrNull(at) as? Token.Operator)?.op

        private fun take() {
            at++
            operations++
        }
    }

    private fun root(value: BigDecimal): BigDecimal {
        if (value.signum() < 0) throw Stop(Result.Undefined)
        if (value.signum() == 0) return BigDecimal.ZERO
        var guess = BigDecimal(Math.sqrt(value.toDouble()), MATH)
        repeat(NEWTON_STEPS) {
            guess = guess.add(value.divide(guess, MATH), MATH).divide(TWO, MATH)
        }
        return guess
    }

    private fun round(value: BigDecimal): BigDecimal {
        val rounded = value.setScale(SHOWN_DECIMALS, RoundingMode.HALF_EVEN).stripTrailingZeros()
        return if (rounded.signum() == 0) BigDecimal.ZERO else rounded
    }

    private companion object {
        val MATH = MathContext.DECIMAL128
        val TWO: BigDecimal = BigDecimal.valueOf(2)
        const val MAX_EXPONENT = 999
        const val NEWTON_STEPS = 4
        const val SHOWN_DECIMALS = 10
        val GROUPING = Regex("(?<=\\d),(?=\\d{3}(?!\\d))")
        val PIECE = Regex("\\d*\\.?\\d+|[+\\-*/×÷^%()−]|[^\\s\\d+\\-*/×÷^%()−]+")
        val SYMBOLS =
            mapOf(
                "+" to Op.PLUS,
                "-" to Op.MINUS,
                "−" to Op.MINUS,
                "*" to Op.TIMES,
                "×" to Op.TIMES,
                "/" to Op.DIVIDE,
                "÷" to Op.DIVIDE,
                "^" to Op.POWER,
                "%" to Op.PERCENT,
                "(" to Op.OPEN,
                ")" to Op.CLOSE,
            )
    }
}
