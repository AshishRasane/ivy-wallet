package com.ivy.wallet.android.notification

import com.ivy.wallet.android.notification.LowBalanceAlerts.Decision
import io.kotest.matchers.shouldBe
import org.junit.Test

class LowBalanceAlertsTest {

    @Test
    fun `alerts once when the balance drops below the limit`() {
        LowBalanceAlerts.decide(balance = 1400.0, threshold = 2000.0, alreadyAlerted = false) shouldBe Decision.Notify
        LowBalanceAlerts.decide(balance = 1300.0, threshold = 2000.0, alreadyAlerted = true) shouldBe Decision.None
    }

    @Test
    fun `resets after a top-up so the next drop alerts again`() {
        LowBalanceAlerts.decide(balance = 5000.0, threshold = 2000.0, alreadyAlerted = true) shouldBe Decision.Clear
        LowBalanceAlerts.decide(balance = 2000.0, threshold = 2000.0, alreadyAlerted = true) shouldBe Decision.Clear
        LowBalanceAlerts.decide(balance = 5000.0, threshold = 2000.0, alreadyAlerted = false) shouldBe Decision.None
    }
}
