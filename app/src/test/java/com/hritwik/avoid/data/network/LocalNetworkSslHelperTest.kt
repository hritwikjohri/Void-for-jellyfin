package com.hritwik.avoid.data.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalNetworkSslHelperTest {

    @Test
    fun `local ranges are detected`() {
        val localIps = listOf(
            "10.0.0.1",
            "10.255.255.254",
            "127.0.0.1",
            "169.254.10.20",
            "172.16.5.10",
            "172.31.200.200",
            "192.168.1.100",
            "224.0.0.5",
            "255.255.255.255",
        )

        localIps.forEach { ip ->
            assertThat(LocalNetworkSslHelper.isLocalNetworkHost(ip)).isTrue()
        }
    }

    @Test
    fun `external ranges are rejected`() {
        val externalIps = listOf(
            "8.8.8.8",
            "11.0.0.1",
            "126.255.255.255",
            "170.0.0.1",
            "173.32.0.0",
            "193.0.0.1",
            "223.255.255.255",
        )

        externalIps.forEach { ip ->
            assertThat(LocalNetworkSslHelper.isLocalNetworkHost(ip)).isFalse()
        }
    }

    @Test
    fun `null or blank hosts are rejected`() {
        assertThat(LocalNetworkSslHelper.isLocalNetworkHost(null)).isFalse()
        assertThat(LocalNetworkSslHelper.isLocalNetworkHost(" ")).isFalse()
    }
}
