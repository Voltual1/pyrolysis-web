package me.voltual.pyrolysis.core.ui.icons.drawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IcCancel: ImageVector
    get() {
        if (_IcCancel != null) return _IcCancel!!
        
        _IcCancel = ImageVector.Builder(
            name = "IcCancel",
            defaultWidth = 256.dp,
            defaultHeight = 256.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(205.7f, 194.3f)
                arcToRelative(8.1f, 8.1f, 0f, false, true, 0f, 11.4f)
                arcToRelative(8.2f, 8.2f, 0f, false, true, -11.4f, 0f)
                lineTo(128f, 139.3f)
                lineTo(61.7f, 205.7f)
                arcToRelative(8.2f, 8.2f, 0f, false, true, -11.4f, 0f)
                arcToRelative(8.1f, 8.1f, 0f, false, true, 0f, -11.4f)
                lineTo(116.7f, 128f)
                lineTo(50.3f, 61.7f)
                arcTo(8.1f, 8.1f, 0f, false, true, 61.7f, 50.3f)
                lineTo(128f, 116.7f)
                lineToRelative(66.3f, -66.4f)
                arcToRelative(8.1f, 8.1f, 0f, false, true, 11.4f, 11.4f)
                lineTo(139.3f, 128f)
                close()
            }
        }.build()
        
        return _IcCancel!!
    }

private var _IcCancel: ImageVector? = null

