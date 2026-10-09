package com.ivy.piechart

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class PieChartStatisticPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot Reports expenses`() {
        snapshot(theme) {
            PieChartStatisticUiTest(isDark = theme == PaparazziTheme.Dark)
        }
    }

    @Test
    fun `snapshot Reports invested`() {
        snapshot(theme) {
            PieChartStatisticUiTest(isDark = theme == PaparazziTheme.Dark, invested = true)
        }
    }

    @Test
    fun `snapshot Reports income`() {
        snapshot(theme) {
            PieChartStatisticUiTest(isDark = theme == PaparazziTheme.Dark, income = true)
        }
    }
}
