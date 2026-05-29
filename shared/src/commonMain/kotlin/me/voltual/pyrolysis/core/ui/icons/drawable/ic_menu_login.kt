package me.voltual.pyrolysis.core.ui.icons.drawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IcMenuLogin: ImageVector
    get() {
        if (_IcMenuLogin != null) return _IcMenuLogin!!
        
        _IcMenuLogin = ImageVector.Builder(
            name = "IcMenuLogin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(11f, 7f)
                lineTo(9.6f, 8.4f)
                lineToRelative(2.6f, 2.6f)
                horizontalLineTo(2f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(10.2f)
                lineToRelative(-2.6f, 2.6f)
                lineTo(11f, 17f)
                lineToRelative(5f, -5f)
                lineToRelative(-5f, -5f)
                close()
                moveToRelative(9f, 12f)
                horizontalLineToRelative(-8f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(8f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-8f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(14f)
                close()
            }
        }.build()
        
        return _IcMenuLogin!!
    }

private var _IcMenuLogin: ImageVector? = null

