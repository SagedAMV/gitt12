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

public val Icons.Rounded.DriveFileRenameOutline: ImageVector
    get() {
        if (_driveFileRenameOutline != null) {
            return _driveFileRenameOutline!!
        }
        _driveFileRenameOutline = materialIcon(name = "Rounded.DriveFileRenameOutline") {
            materialPath {
                moveTo(15.0f, 16.0f)
                lineToRelative(-4.0f, 4.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(0.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(15.0f)
                close()
            }
            materialPath {
                moveTo(12.06f, 7.19f)
                lineToRelative(-8.77f, 8.77f)
                curveTo(3.11f, 16.14f, 3.0f, 16.4f, 3.0f, 16.66f)
                verticalLineTo(19.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(2.34f)
                curveToRelative(0.27f, 0.0f, 0.52f, -0.11f, 0.71f, -0.29f)
                lineToRelative(8.77f, -8.77f)
                lineTo(12.06f, 7.19f)
                close()
            }
            materialPath {
                moveTo(18.71f, 8.04f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineToRelative(-2.34f, -2.34f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineToRelative(-1.83f, 1.83f)
                lineToRelative(3.75f, 3.75f)
                lineTo(18.71f, 8.04f)
                close()
            }
        }
        return _driveFileRenameOutline!!
    }

private var _driveFileRenameOutline: ImageVector? = null
