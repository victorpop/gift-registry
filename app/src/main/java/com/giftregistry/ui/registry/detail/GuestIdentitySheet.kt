package com.giftregistry.ui.registry.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giftregistry.R
import com.giftregistry.domain.model.GuestUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestIdentitySheet(
    initial: GuestUser?,
    onDismiss: () -> Unit,
    onSubmit: (GuestUser) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var firstName by remember { mutableStateOf(initial?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(initial?.lastName.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.reservation_guest_sheet_title))
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(stringResource(R.string.reservation_guest_first_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(R.string.reservation_guest_last_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.reservation_guest_email)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank()) {
                        onSubmit(GuestUser(firstName.trim(), lastName.trim(), email.trim()))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reservation_guest_submit))
            }
        }
    }
}
