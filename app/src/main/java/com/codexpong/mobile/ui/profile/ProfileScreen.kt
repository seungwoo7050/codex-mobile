package com.codexpong.mobile.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codexpong.mobile.R

/**
 * 내 프로필을 조회하고 수정하는 화면.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenHealth: () -> Unit,
    onOpenReplays: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.title_profile),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(id = R.string.label_username) + ": " + (uiState.user?.username ?: ""))
                Text(text = stringResource(id = R.string.label_nickname) + ": " + (uiState.user?.nickname ?: ""))
                Text(text = stringResource(id = R.string.label_rating) + ": " + (uiState.user?.rating?.toString() ?: "-"))
            }
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.nicknameInput,
            onValueChange = viewModel::updateNicknameInput,
            label = { Text(text = stringResource(id = R.string.label_nickname)) }
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.avatarInput,
            onValueChange = viewModel::updateAvatarInput,
            label = { Text(text = stringResource(id = R.string.label_avatar_url)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving && !uiState.isLoading
            ) {
                Text(text = stringResource(id = R.string.action_save_profile))
            }
            Button(
                onClick = { viewModel.refreshProfile() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading
            ) {
                Text(text = stringResource(id = R.string.action_reload_profile))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenHealth, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.title_health))
        }
        Button(onClick = onOpenReplays, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.action_open_replays))
        }
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.title_settings))
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text(text = stringResource(id = R.string.action_logout))
        }
    }
}
