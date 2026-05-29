package me.voltual.pyrolysis.core.ui.icons.drawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IcMenuLogout: ImageVector
    get() {
        if (_IcMenuLogout != null) return _IcMenuLogout!!
        
        _IcMenuLogout = ImageVector.Builder(
            name = "IcMenuLogout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(17f, 7f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(18.17f, 11f)
                horizontalLineTo(8f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(10.17f)
                lineToRelative(-2.58f, 2.58f)
                lineTo(17f, 17f)
                lineToRelative(5f, -5f)
                lineToRelative(-5f, -5f)
                close()
                moveTo(4f, 5f)
                horizontalLineToRelative(8f)
                verticalLineTo(3f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-2f)
                horizontalLineTo(4f)
                verticalLineTo(5f)
                close()
            }
        }.build()
        
        return _IcMenuLogout!!
    }

private var _IcMenuLogout: ImageVector? = null

