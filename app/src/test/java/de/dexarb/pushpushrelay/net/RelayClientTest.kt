package de.dexarb.pushpushrelay.net

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RelayClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: RelayClient

    private val secret = "super-secret-value-123"

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = RelayClient(
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build(),
            notifyUrl = server.url("/notify").toString(),
            heartbeatUrl = server.url("/heartbeat").toString(),
            secretProvider = { secret },
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueOk() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
    }

    @Test
    fun postNotificationSuccessPostsJsonWithoutNullsAndSecretHeader() {
        enqueueOk()

        val result = client.postNotification("de.sparkasse", "Titel", "Body text")

        assertEquals(RelayResult.Success, result)
        val recorded = server.takeRequest()
        assertEquals("/notify", recorded.path)
        assertEquals("POST", recorded.method)
        assertEquals(secret, recorded.getHeader("X-Webhook-Secret"))
        assertTrue(recorded.getHeader("Content-Type")!!.startsWith("application/json"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"app\":\"de.sparkasse\""))
        assertTrue(body.contains("\"title\":\"Titel\""))
        assertTrue(body.contains("\"text\":\"Body text\""))
    }

    @Test
    fun postNotificationOmitsNullFieldsFromBody() {
        enqueueOk()

        val result = client.postNotification("com.bank.app", null, null)

        assertEquals(RelayResult.Success, result)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"app\":\"com.bank.app\""))
        assertFalse(body.contains("title"))
        assertFalse(body.contains("text"))
        assertFalse(body.contains("null"))
    }

    @Test
    fun postHeartbeatSuccessPostsDeviceId() {
        enqueueOk()

        val result = client.postHeartbeat("payment-phone-abcd1234")

        assertEquals(RelayResult.Success, result)
        val recorded = server.takeRequest()
        assertEquals("/heartbeat", recorded.path)
        assertEquals(secret, recorded.getHeader("X-Webhook-Secret"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"device_id\":\"payment-phone-abcd1234\""))
    }

    @Test
    fun unauthorizedResponseMapsToHttpError401() {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = client.postHeartbeat("device-1")

        assertEquals(RelayResult.HttpError(401), result)
    }

    @Test
    fun serverErrorMapsToHttpError500() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = client.postNotification("app", "t", "x")

        assertEquals(RelayResult.HttpError(500), result)
    }

    @Test
    fun serverDisconnectMapsToNetworkError() {
        server.enqueue(
            MockResponse()
                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START)
        )

        val result = client.postHeartbeat("device-1")

        assertTrue(result is RelayResult.NetworkError)
    }

    @Test
    fun toStringOfFailureResultsNeverContainsSecret() {
        server.enqueue(MockResponse().setResponseCode(401))
        val httpError = client.postNotification("app", "t", "x")
        assertFalse(httpError.toString().contains(secret))

        server.enqueue(
            MockResponse()
                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START)
        )
        val networkError = client.postHeartbeat("device-1")
        assertFalse(networkError.toString().contains(secret))
        if (networkError is RelayResult.NetworkError) {
            assertFalse(networkError.message?.contains(secret) == true)
        }
    }
}
