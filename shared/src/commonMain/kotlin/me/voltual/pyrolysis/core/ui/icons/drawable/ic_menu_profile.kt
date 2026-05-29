package me.voltual.pyrolysis.core.ui.icons.drawable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IcMenuProfile: ImageVector
    get() {
        if (_IcMenuProfile != null) return _IcMenuProfile!!
        
        _IcMenuProfile = ImageVector.Builder(
            name = "IcMenuProfile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(12f, 12f)
                curveToRelative(-2.21f, 0f, -4f, -1.79f, -4f, -4f)
                reflectiveCurveToRelative(1.79f, -4f, 4f, -4f)
                reflectiveCurveToRelative(4f, 1.79f, 4f, 4f)
                reflectiveCurveToRelative(-1.79f, 4f, -4f, 4f)
                close()
                moveTo(12f, 6f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                reflectiveCurveToRelative(2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                close()
                moveTo(12f, 13f)
                curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-3f)
                curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
                close()
                moveTo(6f, 17f)
                curveToRelative(0.22f, -0.72f, 3.31f, -2f, 6f, -2f)
                curveToRelative(2.7f, 0f, 5.8f, 1.29f, 6f, 2f)
                horizontalLineTo(6f)
                close()
            }
        }.build()
        
        return _IcMenuProfile!!
    }

private var _IcMenuProfile: ImageVector? = null

