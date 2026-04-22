package com.dysphagiaguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.R
import com.dysphagiaguard.data.model.PatientProfile
import com.dysphagiaguard.data.repository.DeviceRepository
import com.dysphagiaguard.data.repository.SessionRepository
import androidx.compose.ui.graphics.Brush
import com.dysphagiaguard.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSetupScreen(
    sessionRepository: SessionRepository,
    deviceRepository: DeviceRepository,
    onStartMonitoring: (Int, Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val existingProfile by sessionRepository.patientProfile.collectAsState(initial = null)

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var caregiverName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("Post-Stroke") }
    
    val conditions = listOf("Post-Stroke", "Parkinson's", "ALS", "Elderly", "Other")

    val isSilentMode by deviceRepository.isSilentMode.collectAsState(initial = false)
    val isAutoPdf by deviceRepository.isAutoPdfEnabled.collectAsState(initial = true)

    LaunchedEffect(existingProfile) {
        existingProfile?.let {
            name = it.name
            age = it.age
            caregiverName = it.caregiverName
            phone = it.phone
            selectedCondition = it.condition
        }
    }

    val isFormValid = name.isNotBlank() && age.isNotBlank() && caregiverName.isNotBlank() && phone.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurfaceElevated)
                )
            )
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.patient_setup), style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))

        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PremiumTeal,
            unfocusedBorderColor = GlassmorphismBorder,
            focusedContainerColor = GlassmorphismBackground,
            unfocusedContainerColor = GlassmorphismBackground,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
        val textFieldShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.patient_name), color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = textFieldShape)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text(stringResource(R.string.age), color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = textFieldShape)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = caregiverName, onValueChange = { caregiverName = it }, label = { Text(stringResource(R.string.caregiver_name), color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = textFieldShape)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.caregiver_phone), color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, shape = textFieldShape)

        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.condition))
        LazyRow {
            items(conditions) { condition ->
                FilterChip(
                    selected = selectedCondition == condition,
                    onClick = { selectedCondition = condition },
                    label = { Text(condition) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PremiumTeal.copy(alpha = 0.2f),
                        selectedLabelColor = PremiumTeal,
                        selectedTrailingIconColor = PremiumTeal
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (selectedCondition == condition) PremiumTeal else GlassmorphismBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.silent_night_mode), color = TextPrimary)
            Switch(checked = isSilentMode, onCheckedChange = { coroutineScope.launch { deviceRepository.setSilentMode(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumEmerald, checkedTrackColor = PremiumEmerald.copy(alpha = 0.5f)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.auto_pdf), color = TextPrimary)
            Switch(checked = isAutoPdf, onCheckedChange = { coroutineScope.launch { deviceRepository.setAutoPdf(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = PremiumEmerald, checkedTrackColor = PremiumEmerald.copy(alpha = 0.5f)))
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                coroutineScope.launch {
                    val profile = PatientProfile(
                        id = existingProfile?.id ?: 0,
                        name = name,
                        age = age,
                        condition = selectedCondition,
                        caregiverName = caregiverName,
                        phone = phone,
                        silentNightMode = isSilentMode,
                        autoPdf = isAutoPdf
                    )
                    val id = sessionRepository.saveProfile(profile)
                    onStartMonitoring(id.toInt(), deviceRepository.isDemoMode.first())
                }
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.save_and_start), color = DarkBackground, style = MaterialTheme.typography.titleMedium)
        }
    }
}
