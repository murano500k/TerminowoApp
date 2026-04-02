package com.stc.terminowo.presentation.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stc.terminowo.presentation.theme.LocalExtendedColors
import org.jetbrains.compose.resources.stringResource
import terminowo.shared.generated.resources.Res
import terminowo.shared.generated.resources.consent_accept_privacy
import terminowo.shared.generated.resources.consent_accept_tos
import terminowo.shared.generated.resources.consent_continue
import terminowo.shared.generated.resources.consent_privacy_heading
import terminowo.shared.generated.resources.consent_read_full
import terminowo.shared.generated.resources.consent_title
import terminowo.shared.generated.resources.consent_tos_heading

private const val TOS_URL = "https://terminowo.app/docs/terms-of-service.html"
private const val PRIVACY_URL = "https://terminowo.app/docs/privacy-policy.html"

@Composable
fun ConsentScreen(
    onAccepted: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var tosAccepted by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }
    val accentRed = LocalExtendedColors.current.accentRed

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.consent_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Terms of Service section
                Text(
                    text = stringResource(Res.string.consent_tos_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { uriHandler.openUri(TOS_URL) }) {
                    Text(
                        text = stringResource(Res.string.consent_read_full),
                        color = accentRed
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy Policy section
                Text(
                    text = stringResource(Res.string.consent_privacy_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { uriHandler.openUri(PRIVACY_URL) }) {
                    Text(
                        text = stringResource(Res.string.consent_read_full),
                        color = accentRed
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Checkboxes and button pinned to the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = tosAccepted,
                        onCheckedChange = { tosAccepted = it }
                    )
                    Text(
                        text = stringResource(Res.string.consent_accept_tos),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = privacyAccepted,
                        onCheckedChange = { privacyAccepted = it }
                    )
                    Text(
                        text = stringResource(Res.string.consent_accept_privacy),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAccepted,
                    enabled = tosAccepted && privacyAccepted,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.consent_continue),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
