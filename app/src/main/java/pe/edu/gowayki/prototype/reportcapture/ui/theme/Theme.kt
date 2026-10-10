package pe.edu.gowayki.prototype.reportcapture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GoWaykiColorScheme = lightColorScheme(
    primary = GoWaykiTerracotta,
    onPrimary = GoWaykiSurface,
    secondary = GoWaykiBlueGray,
    onSecondary = GoWaykiCharcoal,
    tertiary = GoWaykiCharcoal,
    background = GoWaykiBackground,
    onBackground = GoWaykiCharcoal,
    surface = GoWaykiSurface,
    onSurface = GoWaykiCharcoal,
    outline = GoWaykiBorder
)

@Composable
fun GoWaykiReportCaptureTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GoWaykiColorScheme,
        typography = Typography,
        content = content
    )
}
