package pg.autyzm.friendlyemotions.child.home

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.child.navigation.ChildScreen
import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.usecase.session.CheckSessionEligibilityUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class ChildHomeViewModelTest {
    private val observeActiveLearningStepUseCase = mockk<ObserveActiveLearningStepUseCase>()
    private val checkSessionEligibilityUseCase = mockk<CheckSessionEligibilityUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { observeActiveLearningStepUseCase() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChildHomeViewModel(observeActiveLearningStepUseCase, checkSessionEligibilityUseCase)

    @Test
    fun `onSessionComplete navigates to the End screen carrying the session result`() {
        val viewModel = viewModel()
        val result = SessionResult(correctCount = 4, totalCount = 5, mode = SessionMode.LEARNING)

        viewModel.onSessionComplete(result)

        assertEquals(ChildScreen.End(result), viewModel.screen.value)
    }
}
