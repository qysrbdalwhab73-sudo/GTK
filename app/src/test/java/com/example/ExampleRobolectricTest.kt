package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.analyzer.GameBinaryAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GameReverse", appName)
    }

    @Test
    fun `test unity binary analyzer detection`() {
        val unityHeader = "UnityFS\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(64)
        val (analysis, artifact) = GameBinaryAnalyzer.analyzeByteArray(unityHeader, "Weapon.asset")
        assertTrue(analysis.detectedEngine.contains("Unity"))
        assertNotNull(artifact)
        assertEquals("Weapon.asset Deconstruction", artifact.title)
    }
}
