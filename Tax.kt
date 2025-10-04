
package de.famopt.optimizer

// Simplified German income tax estimation (Grundtabelle) with Splitting by halving income.
// NOTE: Estimator only. For official calculation see §32a EStG.
object TaxEstimator2025 {
    fun incomeTaxSingle(zvE: Double): Double {
        val y: Double
        return when {
            zvE <= 11604.0 -> 0.0
            zvE <= 66259.0 -> {
                val y2 = (zvE - 11604.0) / 10000.0
                (922.98 * y2 + 2397.0) * y2 + 1088.67
            }
            zvE <= 277825.0 -> {
                val y3 = (zvE - 66259.0) / 10000.0
                (181.19 * y3 + 216.16) * y3 + 9136.63
            }
            else -> 0.45 * zvE - 17671.0
        }
    }

    fun incomeTaxMarried(zvE: Double): Double {
        val half = zvE / 2.0
        return incomeTaxSingle(half) * 2.0
    }
}
