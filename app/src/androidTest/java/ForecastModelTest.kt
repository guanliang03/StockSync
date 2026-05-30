import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ui.item.runXGBoostInference

@RunWith(AndroidJUnit4::class)
class ForecastModelTest {

    @Test
    fun testEmptySalesLogReturnsZeroDemandRate() {
        val prediction = runXGBoostInference(salesLog = "", currentQty = 10)
        assertEquals(0.0, prediction.forecastedRate, 0.001)
        assertTrue(prediction.daysRemaining.isInfinite())
    }

    @Test
    fun testSingleSalesLogReturnsDefaultDemandRate() {
        val prediction = runXGBoostInference(salesLog = System.currentTimeMillis().toString(), currentQty = 10)
        assertTrue(prediction.forecastedRate > 0.0)
    }

    @Test
    fun testMultipleSalesLogRunsEnsembleAdjustments() {
        val now = System.currentTimeMillis()
        val salesLog = "${now - 600000},$now" // 10 minutes apart
        val prediction = runXGBoostInference(salesLog = salesLog, currentQty = 5)
        assertTrue(prediction.baseRate > 0.0)
        assertTrue(prediction.forecastedRate > 0.0)
        assertTrue(prediction.daysRemaining > 0.0)
    }
}
