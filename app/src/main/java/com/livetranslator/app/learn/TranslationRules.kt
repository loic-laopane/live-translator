package com.livetranslator.app.learn

object TranslationRules {
    private val FORMULA = Regex("[0-9=²³√×/+−^ⁿˣ⁰¹⁻]")

    /** Mathematical answers (numbers, equations, units with values) are never machine-translated. */
    fun isFormula(text: String): Boolean = FORMULA.containsMatchIn(text)
}
