package theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

object Theme {
    private val LightColorScheme = lightColorScheme(
        primary = soleil_red,
        secondary = white
    )

    @Composable
    fun AppTheme(
        content: @Composable () -> Unit
    ) {
        val colorScheme = LightColorScheme

        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
