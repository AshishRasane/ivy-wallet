package com.ivy.smstransactions.parser

/**
 * Brokers, investment apps and mutual-fund clearing houses. Money sent to them is invested
 * (a transfer to the user's investment account), money from them is a withdrawal.
 */
object InvestmentPayees {
    private val OPTS = setOf(RegexOption.IGNORE_CASE)

    // Groww's company is Nextbillion Technology; Coin/Kuvera SIPs are often debited by
    // BSE StAR MF / Indian Clearing Corporation (ICCL) or NSE Clearing.
    private val PAYEE = Regex(
        "\\bzerodha\\b|\\bgroww\\b|nextbillion|\\bkuvera\\b|\\bupstox\\b|\\bsmallcase\\b|angel ?one|" +
            "paytm money|\\bindmoney\\b|\\biccl\\b|indian clearing|nse clearing|\\bnsccl\\b|" +
            "\\bbse (?:ltd|limited|star)\\b|mutual ?fund|\\bsip\\b",
        OPTS
    )

    // Checked in order: "Zerodha Coin" is Coin, not the Zerodha trading account.
    private val BRANDS = listOf(
        "coin" to Regex("\\bcoin\\b", OPTS),
        "zerodha" to Regex("\\bzerodha\\b", OPTS),
        "groww" to Regex("\\bgroww\\b|nextbillion", OPTS),
        "kuvera" to Regex("\\bkuvera\\b", OPTS),
        "upstox" to Regex("\\bupstox\\b", OPTS),
        "smallcase" to Regex("\\bsmallcase\\b", OPTS),
        "angel" to Regex("angel ?one", OPTS),
        "paytm money" to Regex("paytm money", OPTS),
        "indmoney" to Regex("\\bindmoney\\b", OPTS),
    )

    fun matches(text: String?): Boolean = text != null && PAYEE.containsMatchIn(text)

    /**
     * The app's name in [counterparty] ("Kuvera RZP" -> "kuvera", "NEXTBILLION TECH" -> "groww"),
     * to find the user's account with that name. Null for clearing houses (ICCL, BSE).
     */
    fun brand(counterparty: String?): String? =
        counterparty?.let { name -> BRANDS.firstOrNull { (_, pattern) -> pattern.containsMatchIn(name) }?.first }
}
