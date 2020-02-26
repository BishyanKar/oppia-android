package org.oppia.app.parser

import android.content.Context
import androidx.annotation.StringRes
import org.oppia.app.R
import org.oppia.app.model.Fraction
import org.oppia.app.model.NumberUnit
import org.oppia.app.model.NumberWithUnits
import org.oppia.app.model.Unit
import org.oppia.domain.util.normalizeWhitespace
import java.lang.Double
import java.util.regex.Pattern

/**
 * This class contains methods that help to parse string to number, check realtime and submit time errors.
 * reference https://github.com/oppia/oppia/blob/develop/core/templates/dev/head/domain/objects/NumberWithUnitsObjectFactory.ts#L220.
 */
class StringToNumberWithUnitsParser {

  private lateinit var value: String
  private lateinit var units: String
  private lateinit var rawInput: String
  private var ind = 0
  private val stringToFractionParser: StringToFractionParser = StringToFractionParser()
  private val stringToNumberParser: StringToNumberParser = StringToNumberParser()

  var type = ""
  var real = 0.0
  // Default fraction value.
  var fractionObj = Fraction.newBuilder().setDenominator(1).build()

  /** @return index of pattern in s or -1, if not found
   */
  fun indexOf(pattern: Pattern, s: String): Int {
    val matcher = pattern.matcher(s)
    return if (matcher.find()) matcher.start() else -1
  }

  /** Returns a [NumberWithUnits] parse from the specified raw text string. */
  fun parseNumberWithUnits(inputText: String): NumberWithUnits? {
    // Normalize whitespace to ensure that answer follows a simpler subset of possible patterns.
    rawInput = inputText.normalizeWhitespace()
    rawInput = rawInput.trim()
    return parseNumberWithUnitsNotStartingWithCurrency(rawInput)
      ?: parseNumberWithUnitsStartsWithCurrency(rawInput)

  }

  fun parseNumberWithUnitsNotStartingWithCurrency(rawInput: String): NumberWithUnits? {

    // Start with digit when there is no currency unit.
    if (rawInput.matches("^\\-?\\d.*\$".toRegex())) {
      ind = indexOf(Pattern.compile("[a-z(₹$]"), rawInput)
      if (ind == -1) {
        // There is value with no units.
        value = rawInput
        units = ""
      } else {
        ind -= 1
        value = rawInput.substring(0, ind).trim()
        units = rawInput.substring(ind).trim()
      }
    } else return null
    if (value.contains('/')) {
      type = "fraction"
      fractionObj = StringToFractionParser().parseFractionFromString(value)
    } else {
      type = "real"
      real = Double.parseDouble(value)
    }
    if (type.equals("fraction"))
      return NumberWithUnits.newBuilder().setFraction(fractionObj).addUnit(NumberUnit.newBuilder().setUnit(units))
        .build()
    else
      return NumberWithUnits.newBuilder().setReal(real).addUnit(NumberUnit.newBuilder().setUnit(units)).build()
    return NumberWithUnits.getDefaultInstance()
  }

  fun parseNumberWithUnitsStartsWithCurrency(rawInput: String): NumberWithUnits? {

    if (rawInput.matches("^\\-?\\d.*\$".toRegex()))
      return null
    else {

      var ind = indexOf(Pattern.compile("[0-9]"), rawInput)

      units = rawInput.substring(0, ind).trim()

      units = units + ""
      var ind2 = indexOf(Pattern.compile("[a-z(]"), rawInput.substring(ind))
      if (ind2 != -1) {
        value = rawInput.substring(ind, ind2 - ind).trim()
        units += rawInput.substring(ind2).trim()
      } else {
        value = rawInput.substring(ind).trim()
        units = units.trim()
      }
    }
    if (value.contains('/')) {
      type = "fraction"
      fractionObj = StringToFractionParser().parseFractionFromString(value)
    } else {
      type = "real"
      real = Double.parseDouble(value)
    }
    if (type.equals("fraction"))
      return NumberWithUnits.newBuilder().setFraction(fractionObj).addUnit(NumberUnit.newBuilder().setUnit(units))
        .build()
    else
      return NumberWithUnits.newBuilder().setReal(real).addUnit(NumberUnit.newBuilder().setUnit(units)).build()
    return NumberWithUnits.getDefaultInstance()
  }

  fun getNumberWithUnitsRealTimeError(rawInput: String, context: Context): String? {

    // Start with digit when there is no currency unit.
    if (rawInput.matches("^\\-?\\d.*\$".toRegex())) {
      var ind = indexOf(Pattern.compile("[a-z(₹$]"), rawInput)
      if (ind == -1) {
        // There is value with no units.
        value = rawInput
        units = ""
      } else {
        ind -= 1
        value = rawInput.substring(0, ind).trim()
        units = rawInput.substring(ind).trim()
      }
    } else {
      var ind = indexOf(Pattern.compile("[0-9]"), rawInput)
      if (ind != -1) {
        units = rawInput.substring(0, ind).trim()
        units = units + ""
        var ind2 = indexOf(Pattern.compile("[a-z(]"), rawInput.substring(ind))
        if (ind2 != -1) {
          value = rawInput.substring(ind, ind2 - ind).trim()
          units += rawInput.substring(ind2).trim()
        } else {
          value = rawInput.substring(ind).trim()
          units = units.trim()
        }
      } else {
        units = rawInput
        value = ""
      }
    }
    if (value.contains('/')) {
      type = "fraction"
    } else {
      type = "real"
    }

    if (type.equals("fraction"))
      return stringToFractionParser.getRealTimeAnswerError(rawInput).getErrorMessageFromStringRes(context)
    else
      return stringToNumberParser.getRealTimeAnswerError(rawInput).getErrorMessageFromStringRes(context)
  }

  fun getNumberWithUnitsSubmitTimeError(rawInput: String, context: Context): String? {

    // Allow validation only when rawInput is not null or an empty string.
    // Start with digit when there is no currency unit.
    if (rawInput.matches("^\\-?\\d.*\$".toRegex())) {
      var ind = indexOf(Pattern.compile("[a-z(₹$]"), rawInput)
      if (ind == -1) {
        // There is value with no units.
        value = rawInput;
        units = ""
      } else {
        ind -= 1
        value = rawInput.substring(0, ind).trim()
        units = rawInput.substring(ind).trim()
      }

      var keys = (CURRENCY_UNITS).keys
      for (i in keys) {
        for (j in 0 until CURRENCY_UNITS[i]!!.frontUnits.size) {
          if (units.indexOf(CURRENCY_UNITS[i]!!.frontUnits[j]) != -1) {
            return NumberWithUnitsParsingError.INVALID_CURRENCY_FORMAT.getErrorMessageFromStringRes(context)
          }
        }
      }
    } else {
      var startsWithCorrectCurrencyUnit = false
      var keys = (CURRENCY_UNITS).keys
      for (i in keys) {
        for (j in 0 until CURRENCY_UNITS[i]!!.frontUnits.size) {
          if (rawInput.startsWith(CURRENCY_UNITS[i]!!.frontUnits[j])) {
            startsWithCorrectCurrencyUnit = true
            break
          }
        }
      }
      if (startsWithCorrectCurrencyUnit == false) {
        return NumberWithUnitsParsingError.INVALID_CURRENCY.getErrorMessageFromStringRes(context)
      }

      if (ind == -1) {
        return NumberWithUnitsParsingError.INVALID_CURRENCY.getErrorMessageFromStringRes(context)
      }

      startsWithCorrectCurrencyUnit = false
      keys = (CURRENCY_UNITS).keys
      for (i in keys) {
        for (j in 0 until CURRENCY_UNITS[i]!!.frontUnits.size) {
          if (units == (CURRENCY_UNITS[i]!!.frontUnits[j]).trim()) {
            startsWithCorrectCurrencyUnit = true
            break
          }
        }
      }
      if (startsWithCorrectCurrencyUnit == false) {
        return NumberWithUnitsParsingError.INVALID_CURRENCY.getErrorMessageFromStringRes(context)
      }

      // Checking invalid characters in value.
      if (value.matches("[a - z]".toRegex()) || value.matches("[ *^$₹()#@]/".toRegex())) {
        return NumberWithUnitsParsingError.INVALID_VALUE.getErrorMessageFromStringRes(context)
      }


      if (units != "") {
        // Checking invalid characters in units.
        if (units.matches("[^0-9a-z/* ^()₹$-]".toRegex())) {
          return NumberWithUnitsParsingError.INVALID_UNIT_CHARS.getErrorMessageFromStringRes(context)
        }
      }
    }
    if (type.equals("fraction"))
      return stringToFractionParser.getSubmitTimeError(rawInput).getErrorMessageFromStringRes(
        context
      )
    else
      return stringToNumberParser.getSubmitTimeError(rawInput).getErrorMessageFromStringRes(
        context
      )
    return NumberWithUnitsParsingError.VALID.getErrorMessageFromStringRes(context)
  }

  fun getStringOfNumberWithUnits(inputText: String): String {
    var numberWithUnitsString = this.value.trim()
    var unitsString = this.units.trim()
    numberWithUnitsString =
      if (Pattern.compile("^[rs,$,₹,€,£,¥]", Pattern.CASE_INSENSITIVE).matcher(inputText).find())
        unitsString.trim() + " " + numberWithUnitsString else numberWithUnitsString + " " + unitsString.trim()
    numberWithUnitsString = numberWithUnitsString.trim()
    return numberWithUnitsString
  }

  /** Enum to store the errors of [NumberWithUnitsInputInteractionView]. */
  enum class NumberWithUnitsParsingError(@StringRes private var error: Int?) {
    VALID(error = null),
    INVALID_CURRENCY_FORMAT(error = R.string.fraction_error_invalid_chars),
    INVALID_CURRENCY(error = R.string.fraction_error_invalid_chars),
    INVALID_VALUE(error = R.string.fraction_error_invalid_chars),
    INVALID_UNIT_CHARS(error = R.string.fraction_error_invalid_format),
    DIVISION_BY_ZERO(error = R.string.fraction_error_divide_by_zero),
    NUMBER_TOO_LONG(error = R.string.fraction_error_larger_than_seven_digits);

    /** Returns the string corresponding to this error's string resources, or null if there is none. */
    fun getErrorMessageFromStringRes(context: Context): String? {
      return error?.let(context::getString)
    }
  }

  companion object {
    private val CURRENCY_UNITS = mapOf(
      "dollar" to Unit(
        name = "dollar",
        aliases = listOf("$", "dollars", "Dollars", "Dollar", "USD"),
        frontUnits = listOf("$"),
        baseUnit = null
      ),
      "rupee" to Unit(
        name = "rupee",
        aliases = listOf("Rs", "rupees", "₹", "Rupees", "Rupee"),
        frontUnits = listOf("Rs ", "₹"),
        baseUnit = null
      ),
      "cent" to Unit(
        name = "cent",
        aliases = listOf("cents", "Cents", "Cent"),
        frontUnits = listOf(),
        baseUnit = "0.01 dollar"
      ),
      "paise" to Unit(
        name = "paise",
        aliases = listOf("paisa", "Paise", "Paisa"),
        frontUnits = listOf(),
        baseUnit = "0.01 rupee"
      )
    )
  }
}
