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

public val Icons.Rounded.OfflinePin: ImageVector
    get() {
        if (_offlinePin != null) {
            return _offlinePin!!
        }
        _offlinePin = materialIcon(name = "Rounded.OfflinePin") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.5f, 2.0f, 2.0f, 6.5f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.5f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.5f, 10.0f, -10.0f)
                reflectiveCurveTo(17.5f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(16.0f, 18.0f)
                lineTo(8.0f, 18.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                close()
                moveTo(9.59f, 13.29f)
                lineTo(7.7f, 11.4f)
                curveToRelative(-0.39f, -0.39f, -0.39f, -1.01f, 0.0f, -1.4f)
                curveToRelative(0.39f, -0.39f, 1.01f, -0.39f, 1.4f, 0.0f)
                lineToRelative(1.2f, 1.2f)
                lineToRelative(4.6f, -4.6f)
                curveToRelative(0.39f, -0.39f, 1.01f, -0.39f, 1.4f, 0.0f)
                curveToRelative(0.39f, 0.39f, 0.39f, 1.01f, 0.0f, 1.4f)
                lineToRelative(-5.29f, 5.29f)
                curveToRelative(-0.39f, 0.39f, -1.03f, 0.39f, -1.42f, 0.0f)
                close()
            }
        }
        return _offlinePin!!
    }

private var _offlinePin: ImageVector? = null
