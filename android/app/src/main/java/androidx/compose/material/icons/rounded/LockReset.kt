/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Rounded.LockReset: ImageVector
    get() {
        if (_lockReset != null) {
            return _lockReset!!
        }
        _lockReset = materialIcon(name = "Rounded.LockReset") {
            materialPath {
                moveTo(13.26f, 3.0f)
                curveTo(8.17f, 2.86f, 4.0f, 6.94f, 4.0f, 12.0f)
                horizontalLineTo(2.21f)
                curveToRelative(-0.45f, 0.0f, -0.67f, 0.54f, -0.35f, 0.85f)
                lineToRelative(2.79f, 2.79f)
                curveToRelative(0.2f, 0.2f, 0.51f, 0.2f, 0.71f, 0.0f)
                lineToRelative(2.8f, -2.79f)
                curveTo(8.46f, 12.54f, 8.24f, 12.0f, 7.79f, 12.0f)
                horizontalLineTo(6.0f)
                curveToRelative(0.0f, -3.89f, 3.2f, -7.06f, 7.1f, -7.0f)
                curveToRelative(3.71f, 0.05f, 6.84f, 3.18f, 6.9f, 6.9f)
                curveToRelative(0.06f, 3.91f, -3.1f, 7.1f, -7.0f, 7.1f)
                curveToRelative(-1.59f, 0.0f, -3.05f, -0.53f, -4.23f, -1.43f)
                curveToRelative(-0.4f, -0.3f, -0.96f, -0.27f, -1.31f, 0.09f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(-0.43f, 0.43f, -0.39f, 1.14f, 0.09f, 1.5f)
                curveTo(9.06f, 20.31f, 10.95f, 21.0f, 13.0f, 21.0f)
                curveToRelative(5.06f, 0.0f, 9.14f, -4.17f, 9.0f, -9.25f)
                curveTo(21.87f, 7.05f, 17.95f, 3.13f, 13.26f, 3.0f)
                close()
                moveTo(15.0f, 11.0f)
                verticalLineToRelative(-1.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(1.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(3.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(4.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-3.0f)
                curveTo(16.0f, 11.45f, 15.55f, 11.0f, 15.0f, 11.0f)
                close()
                moveTo(14.0f, 11.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-1.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                verticalLineTo(11.0f)
                close()
            }
        }
        return _lockReset!!
    }

private var _lockReset: ImageVector? = null
