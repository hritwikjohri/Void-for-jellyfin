package com.hritwik.avoid.data.repository

import android.content.Context
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.connection.ServerConnectionState
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod
import com.hritwik.avoid.domain.model.auth.ServerConnectionType
import com.hritwik.avoid.utils.DataWiper
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import com.hritwik.avoid.utils.helpers.getDeviceName
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @MockK
    private lateinit var preferencesManager: PreferencesManager

    @MockK
    private lateinit var retrofitBuilder: Retrofit.Builder

    @MockK
    private lateinit var dataWiper: DataWiper

    @MockK
    private lateinit var networkMonitor: NetworkMonitor

    @MockK
    private lateinit var serverConnectionManager: ServerConnectionManager

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var priorityDispatcher: PriorityDispatcher

    private val retrofit: Retrofit = mockk()
    private val serviceQueue = ArrayDeque<JellyfinApiService>()
    private val recordedBaseUrls = mutableListOf<String>()
    private val networkState = MutableStateFlow(true)
    private val connectionState = MutableStateFlow(ServerConnectionState())

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        recordedBaseUrls.clear()
        serviceQueue.clear()
        networkState.value = true
        connectionState.value = ServerConnectionState()
        MockKAnnotations.init(this, relaxed = true)
        mockkStatic("com.hritwik.avoid.utils.helpers.GetAppVersionKt")
        every { getDeviceName(context) } returns "device-id"

        every { retrofitBuilder.baseUrl(any()) } answers {
            recordedBaseUrls.add(firstArg())
            retrofitBuilder
        }
        every { retrofitBuilder.build() } returns retrofit
        every { retrofit.create(JellyfinApiService::class.java) } answers {
            serviceQueue.removeFirst()
        }

        every { networkMonitor.isConnected } returns networkState
        every { serverConnectionManager.state } returns connectionState
        every { serverConnectionManager.normalizeUrl(any()) } answers {
            val raw = firstArg<String>()
            raw.trimEnd('/')
        }

        coEvery { serverConnectionManager.markRequestSuccess(any()) } returns Unit
        coEvery { serverConnectionManager.markRequestFailure(any(), any()) } returns Unit

        repository = AuthRepositoryImpl(
            preferencesManager = preferencesManager,
            retrofitBuilder = retrofitBuilder,
            dataWiper = dataWiper,
            networkMonitor = networkMonitor,
            serverConnectionManager = serverConnectionManager,
            context = context,
            priorityDispatcher = priorityDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        recordedBaseUrls.clear()
        serviceQueue.clear()
    }

    @Test
    fun `logout uses refreshed server connection`() = runTest {
        val oldService = mockk<JellyfinApiService>(relaxed = true)
        val newService = mockk<JellyfinApiService>(relaxed = true)
        serviceQueue.add(newService)
        setPrivateField("currentServerUrl", "http://old.server")
        setPrivateField("currentApiService", oldService)

        every { preferencesManager.getAccessToken() } returns flowOf("token")
        every { preferencesManager.getServerUrl() } returns flowOf("http://old.server")
        coEvery { serverConnectionManager.refreshActiveConnection(messageOnSwitch = any(), excludeUrl = any()) } returns ServerConnectionMethod(
            url = "http://new.server",
            type = ServerConnectionType.REMOTE
        )
        coEvery { newService.logout(any()) } returns Unit
        coEvery { preferencesManager.clearAllPreferences() } returns Unit
        coEvery { preferencesManager.clearRecentSearches() } returns Unit
        coEvery { preferencesManager.setFirstRunCompleted(true) } returns Unit
        coEvery { dataWiper.wipeAll() } returns Unit

        val result = repository.logout()

        assertIs<NetworkResult.Success<Unit>>(result)
        assertEquals(listOf("http://new.server/"), recordedBaseUrls)
        coVerify(exactly = 1) { newService.logout(any()) }
        coVerify(exactly = 0) { oldService.logout(any()) }
    }

    @Test
    fun `validate session recreates service when server url changes`() = runTest {
        val oldService = mockk<JellyfinApiService>(relaxed = true)
        val newService = mockk<JellyfinApiService>(relaxed = true)
        serviceQueue.add(newService)
        setPrivateField("currentServerUrl", "http://old.server")
        setPrivateField("currentApiService", oldService)

        every { preferencesManager.getAccessToken() } returns flowOf("token")
        every { preferencesManager.getServerUrl() } returns flowOf("http://new.server")
        connectionState.value = ServerConnectionState(isOffline = false)
        coEvery { newService.getCurrentUser(any()) } returns mockk(relaxed = true)

        val result = repository.validateSession()

        assertIs<NetworkResult.Success<Boolean>>(result)
        assertEquals(true, result.data)
        assertEquals(listOf("http://new.server/"), recordedBaseUrls)
        coVerify(exactly = 1) { newService.getCurrentUser(any()) }
        coVerify(exactly = 0) { oldService.getCurrentUser(any()) }
    }

    private fun setPrivateField(name: String, value: Any?) {
        val field = AuthRepositoryImpl::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(repository, value)
    }
}
