package pg.autyzm.friendlyemotions.therapist.welcome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TherapistWelcomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `auto-advances to home after the splash duration`() =
        runTest(dispatcher) {
            val viewModel = TherapistWelcomeViewModel()
            var advanced = false
            val collectJob = launch { viewModel.navigateToHome.collect { advanced = true } }

            dispatcher.scheduler.advanceTimeBy(5_000)
            dispatcher.scheduler.runCurrent()

            assertTrue(advanced)
            collectJob.cancel()
        }

    @Test
    fun `onContinueClicked advances immediately and cancels the pending timer`() =
        runTest(dispatcher) {
            val viewModel = TherapistWelcomeViewModel()
            var advanceCount = 0
            val collectJob = launch { viewModel.navigateToHome.collect { advanceCount++ } }

            viewModel.onContinueClicked()
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.advanceTimeBy(10_000)
            dispatcher.scheduler.runCurrent()

            assertEquals(1, advanceCount)
            collectJob.cancel()
        }
}
