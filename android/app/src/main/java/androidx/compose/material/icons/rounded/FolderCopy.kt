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

public val Icons.Rounded.FolderCopy: ImageVector
    get() {
        if (_folderCopy != null) {
            return _folderCopy!!
        }
        _folderCopy = materialIcon(name = "Rounded.FolderCopy") {
            materialPath {
                moveTo(2.0f, 6.0f)
                lineTo(2.0f, 6.0f)
                curveTo(1.45f, 6.0f, 1.0f, 6.45f, 1.0f, 7.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(0.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(7.0f)
                curveTo(3.0f, 6.45f, 2.55f, 6.0f, 2.0f, 6.0f)
                close()
            }
            materialPath {
                moveTo(21.0f, 4.0f)
                horizontalLineToRelative(-7.0f)
                lineToRelative(-1.41f, -1.41f)
                curveTo(12.21f, 2.21f, 11.7f, 2.0f, 11.17f, 2.0f)
                horizontalLineTo(7.0f)
                curveTo(5.9f, 2.0f, 5.01f, 2.9f, 5.01f, 4.0f)
                lineTo(5.0f, 15.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveTo(23.0f, 4.9f, 22.1f, 4.0f, 21.0f, 4.0f)
                close()
            }
        }
        return _folderCopy!!
    }

private var _folderCopy: ImageVector? = null
