package com.example

import com.example.data.IniFileEngine
import com.example.model.GameSpeed
import com.example.model.ResolutionOption
import com.example.model.VideoSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun iniFileEngine_preservesUnrelatedSections_andUpdatesVideoAndSettings() {
        val original = """
            [Intro]
            Play=yes
            
            [Video]
            ScreenWidth=800
            ScreenHeight=600
            
            [Audio]
            SoundVolume=0.80
        """.trimIndent()

        val settings = VideoSettings(
            resolution = ResolutionOption(1920, 1080),
            gameSpeed = GameSpeed.FAST,
            stretchMovies = true,
            allowHiResModes = true,
            videoBackBuffer = false
        )

        val updated = IniFileEngine.applyVideoSettings(original, settings)
        val parsed = IniFileEngine.parse(updated)

        assertEquals("yes", IniFileEngine.getValue(parsed, "Intro", "Play"))
        assertEquals("0.80", IniFileEngine.getValue(parsed, "Audio", "SoundVolume"))
        assertEquals("1920", IniFileEngine.getValue(parsed, "Video", "ScreenWidth"))
        assertEquals("1080", IniFileEngine.getValue(parsed, "Video", "ScreenHeight"))
        assertEquals("yes", IniFileEngine.getValue(parsed, "Video", "StretchMovies"))
        assertEquals("yes", IniFileEngine.getValue(parsed, "Video", "AllowHiResModes"))
        assertEquals("no", IniFileEngine.getValue(parsed, "Video", "VideoBackBuffer"))
        assertEquals("1", IniFileEngine.getValue(parsed, "Settings", "GameSpeed"))

        val extracted = IniFileEngine.extractVideoSettings(updated, VideoSettings())
        assertEquals(ResolutionOption(1920, 1080), extracted.resolution)
        assertEquals(GameSpeed.FAST, extracted.gameSpeed)
        assertTrue(extracted.stretchMovies)
    }
}
