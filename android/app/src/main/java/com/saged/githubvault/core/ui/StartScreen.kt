@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.saged.githubvault.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saged.githubvault.feature.auth.VaultState
import com.saged.githubvault.feature.auth.VaultViewModel

/**
 * The first interactive screen. Everything a new user needs lives in one card and there are only
 * two ways forward: sign in (or request an account), or open the app without an account.
 * The previous version stacked a large hero, duplicated headings and seven separate actions.
 */
@Composable
fun StartScreen(s: VaultState, vm: VaultViewModel) {
    var register by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var connection by remember { mutableStateOf(false) }

    if (connection) {
        ConnectionDialog(
            onDismiss = { connection = false },
            onConfirm = { url, key -> vm.configure(url, key); connection = false }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(38.dp)
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    "GitHub Vault",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "ملفاتك معك",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .09f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "آمن على جهازك",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        Text(
            if (register) "خطوة، وتبدأ." else "أهلاً من جديد.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "مكان واحد، وبس.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                ModeSwitch(register = register, onChange = {
                    register = it
                    pass = ""
                    confirm = ""
                })

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.lowercase().take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم المستخدم") },
                    placeholder = { Text("مثال: saged7") },
                    leadingIcon = { Icon(Icons.Rounded.PersonOutline, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    isError = name.isNotEmpty() && !name.matches(Regex("[a-z0-9_]{3,32}")),
                    supportingText = if (register) {
                        { Text("3–32 حرفًا إنجليزيًا") }
                    } else null
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it.take(128) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كلمة المرور") },
                    leadingIcon = { Icon(Icons.Rounded.LockOpen, null) },
                    trailingIcon = {
                        MotionIconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                if (visible) "أخفِ كلمة المرور" else "أظهر كلمة المرور"
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(15.dp)
                )

                AnimatedVisibility(visible = register) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it.take(128) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("تأكيد كلمة المرور") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(15.dp),
                        isError = confirm.isNotEmpty() && confirm != pass,
                        supportingText = { Text("الحد الأدنى 12") }
                    )
                }

                val valid = name.matches(Regex("[a-z0-9_]{3,32}")) &&
                    pass.isNotEmpty() &&
                    (!register || (pass.length in 12..128 && pass == confirm))

                MotionButton(
                    onClick = {
                        if (register) vm.register(name, pass) else vm.login(name, pass)
                        pass = ""
                        confirm = ""
                    },
                    enabled = valid && !s.busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    if (s.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            if (register) "أرسل الطلب" else "دخول",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                    }
                }

                Text(
                    if (register) "المشرف يراجع الطلب."
                    else "بياناتك تبقى بجهازك.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "أو افتح مباشرة",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAccess(
                title = "المستودعات",
                subtitle = "تصفّح العام",
                icon = Icons.Rounded.FolderOpen,
                modifier = Modifier.weight(1f),
                onClick = { vm.tab("github") }
            )
            QuickAccess(
                title = "ملفاتك",
                subtitle = "على الجهاز",
                icon = Icons.Rounded.OfflinePin,
                modifier = Modifier.weight(1f),
                onClick = { vm.tab("local") }
            )
        }

        Spacer(Modifier.height(6.dp))
        MotionTextButton(
            onClick = { connection = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Rounded.SettingsEthernet, null, Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("إعداد الاتصال")
        }
        Text(
            "GitHub Vault 0.7",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ModeSwitch(register: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeOption("دخول", !register, Modifier.weight(1f)) { onChange(false) }
        ModeOption("حساب جديد", register, Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surface
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickAccess(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MotionCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ConnectionDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إعداد الاتصال") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "اربط Supabase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "تنحفظ بجهازك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text("Project URL") },
                    placeholder = { Text("https://…supabase.co") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.trim() },
                    label = { Text("المفتاح العام") },
                    singleLine = true,
                    visualTransformation = if (hidden) PasswordVisualTransformation()
                    else VisualTransformation.None,
                    trailingIcon = {
                        MotionIconButton(onClick = { hidden = !hidden }) {
                            Icon(
                                if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                if (hidden) "أظهر المفتاح" else "أخفِ المفتاح"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            MotionTextButton(
                onClick = { onConfirm(url, key) },
                enabled = url.startsWith("http") && key.length > 20
            ) { Text("احفظ") }
        },
        dismissButton = {
            MotionTextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
