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

public val Icons.Rounded.DriveFolderUpload: ImageVector
    get() {
        if (_driveFolderUpload != null) {
            return _driveFolderUpload!!
        }
        _driveFolderUpload = materialIcon(name = "Rounded.DriveFolderUpload") {
            materialPath {
                moveTo(20.0f, 6.0f)
                horizontalLineToRelative(-8.0f)
                lineToRelative(-1.41f, -1.41f)
                curveTo(10.21f, 4.21f, 9.7f, 4.0f, 9.17f, 4.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 4.0f, 2.01f, 4.9f, 2.01f, 6.0f)
                lineTo(2.0f, 18.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(8.0f)
                curveTo(22.0f, 6.9f, 21.1f, 6.0f, 20.0f, 6.0f)
                close()
                moveTo(13.0f, 13.0f)
                verticalLineToRelative(3.0f)
                curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                horizontalLineToRelative(0.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineTo(9.21f)
                curveToRelative(-0.45f, 0.0f, -0.67f, -0.54f, -0.35f, -0.85f)
                lineToRelative(2.8f, -2.79f)
                curveToRelative(0.2f, -0.2f, 0.51f, -0.19f, 0.71f, 0.0f)
                lineToRelative(2.79f, 2.79f)
                curveTo(15.46f, 12.46f, 15.24f, 13.0f, 14.8f, 13.0f)
                horizontalLineTo(13.0f)
                close()
            }
        }
        return _driveFolderUpload!!
    }

private var _driveFolderUpload: ImageVector? = null
