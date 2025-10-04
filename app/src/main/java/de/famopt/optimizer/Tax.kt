package de.famopt.optimizer

// Vereinfachte ESt-Schätzung 2025 (Demo; §32a EStG vereinfacht)
object TaxEstimator2025 {
    fun incomeTaxSingle(zvE: Double): Double {
        return when {
            zvE <= 11604.0 -> 0.0
            zvE <= 66259.0 -> {
                val y = (zvE - 11604.0) / 10000.0
                (922.98 * y + 2397.0) * y + 1088.67
            }
            zvE <= 277825.0 -> {
                val y = (zvE - 66259.0) / 10000.0
                (181.19 * y + 216.16) * y + 9136.63
            }
            else -> 0.45 * zvE - 17671.0
        }
    }
    fun incomeTaxMarried(zvE: Double): Double = incomeTaxSingle(zvE / 2.0) * 2.0
}
