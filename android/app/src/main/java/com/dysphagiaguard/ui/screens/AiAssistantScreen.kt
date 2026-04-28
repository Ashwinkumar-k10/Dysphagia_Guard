package com.dysphagiaguard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dysphagiaguard.ui.components.BottomNavBar
import com.dysphagiaguard.ui.theme.*
import com.dysphagiaguard.viewmodel.MonitorViewModel
import kotlinx.coroutines.launch

// ─── Data model ──────────────────────────────────────────────────────────────
private data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: Long = System.currentTimeMillis()
)

// ─── Screen ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: MonitorViewModel,
    navController: NavController
) {
    val event by viewModel.eventStream.collectAsState()
    val totalSwallows by viewModel.totalSwallows.collectAsState()
    val unsafeSwallows by viewModel.unsafeSwallows.collectAsState()
    val coughCount by viewModel.coughCount.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Greeting on first load
    LaunchedEffect(Unit) {
        messages.add(ChatMessage(buildGreeting(event.classification, totalSwallows, unsafeSwallows, coughCount), false))
    }

    // Quick-action chips
    val quickActions = listOf(
        "What does COUGH mean?",
        "Is it safe to eat now?",
        "Explain UNSAFE event",
        "What is silent aspiration?",
        "Emergency steps",
        "Session summary"
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(text, true))
        val reply = generateAiReply(text, event.classification, totalSwallows, unsafeSwallows, coughCount)
        messages.add(ChatMessage(reply, false))
        scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkBackground, DarkSurfaceElevated)))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(PremiumTeal, PremiumEmerald))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SmartToy, null, tint = DarkBackground, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("DysphagiaGuard AI", style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary, fontWeight = FontWeight.Bold))
                        Text("Clinical assistant · Context-aware", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                    Spacer(Modifier.weight(1f))
                    // Live status dot
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(
                            when (event.classification) {
                                "SAFE" -> PremiumEmerald; "UNSAFE" -> PremiumCrimson
                                "COUGH" -> CoughOrange; else -> TextSecondary
                            }
                        )
                    )
                }

                Divider(color = GlassmorphismBorder)

                // ── Chat messages ────────────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInHorizontally(initialOffsetX = { if (msg.isUser) it else -it })
                        ) {
                            ChatBubble(msg)
                        }
                    }
                }

                // ── Quick action chips ───────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LazyColumn(Modifier.height(0.dp)) {} // no-op to satisfy syntax
                }
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(quickActions) { action ->
                        SuggestionChip(
                            onClick = { sendMessage(action) },
                            label = { Text(action, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = GlassmorphismBackground,
                                labelColor = PremiumTeal
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                borderColor = PremiumTeal.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // ── Input bar ────────────────────────────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about the patient's condition…", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)) },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumTeal,
                            unfocusedBorderColor = GlassmorphismBorder,
                            focusedContainerColor = GlassmorphismBackground,
                            unfocusedContainerColor = GlassmorphismBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage(inputText); inputText = "" }),
                        maxLines = 3
                    )
                    IconButton(
                        onClick = { sendMessage(inputText); inputText = "" },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(PremiumTeal)
                    ) {
                        Icon(Icons.Default.Send, null, tint = DarkBackground)
                    }
                }
            }
        }
    }
}

// ─── Chat Bubble ─────────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(PremiumTeal, PremiumEmerald))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, tint = DarkBackground, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Card(
            shape = RoundedCornerShape(
                topStart = if (msg.isUser) 18.dp else 4.dp,
                topEnd = if (msg.isUser) 4.dp else 18.dp,
                bottomStart = 18.dp, bottomEnd = 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (msg.isUser) PremiumTeal.copy(alpha = 0.15f) else GlassmorphismBackground
            ),
            border = BorderStroke(1.dp, if (msg.isUser) PremiumTeal.copy(0.3f) else GlassmorphismBorder),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                msg.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, lineHeight = 22.sp)
            )
        }
    }
}

// ─── AI Brain — Context-aware rule engine ────────────────────────────────────
private fun buildGreeting(cls: String, safe: Int, unsafe: Int, coughs: Int): String {
    val total = safe + unsafe + coughs
    return buildString {
        appendLine("👋 Hello! I'm the DysphagiaGuard clinical assistant.")
        appendLine()
        if (total == 0) {
            appendLine("No swallow events recorded yet. I'm monitoring in real-time and will give you instant guidance whenever an event is detected.")
        } else {
            appendLine("📊 Current session: $total events — $safe safe, $unsafe unsafe, $coughs coughs.")
            if (unsafe >= 2) appendLine("⚠️ Elevated unsafe events — I recommend pausing oral feeding.")
            if (coughs >= 2) appendLine("🟠 Multiple coughs detected — possible silent aspiration risk.")
        }
        appendLine()
        appendLine("Ask me anything or tap a quick-action below.")
    }
}

private fun generateAiReply(
    query: String,
    currentCls: String,
    totalSwallows: Int,
    unsafeCount: Int,
    coughCount: Int
): String {
    val q = query.lowercase()
    val total = totalSwallows + coughCount
    val coughRatio = if (total > 0) coughCount.toFloat() / total else 0f

    // ── COUGH questions ──────────────────────────────────────────────────────
    if (q.contains("cough") && (q.contains("what") || q.contains("mean") || q.contains("explain"))) {
        return """
🟠 Cough vs. Swallow — How we classify it:

Our dual-sensor system distinguishes coughs from swallows using two key signals:

1️⃣ IMU (motion) pattern:
   • Swallow → smooth 120-200ms bell-curve (laryngeal elevation)
   • Cough → DOUBLE-PEAK < 80ms (explosive diaphragm contraction)

2️⃣ Microphone pattern:
   • Swallow → single smooth peak, low turbulence
   • Cough → HIGH zero-crossing rate (ZCR > 25/50 samples), turbulent airflow burst

⚕️ Clinical significance: Coughing during or after swallowing can indicate "silent aspiration" — food or liquid entering the airway without the patient realising it.

Current session: $coughCount cough(s) detected.
        """.trimIndent()
    }

    if (q.contains("silent aspiration")) {
        return """
🔬 Silent Aspiration Explained:

Silent aspiration occurs when food, liquid, or saliva enters the airway (below the vocal cords) WITHOUT triggering a cough reflex.

It's called "silent" because the patient shows no obvious choking sign, making it extremely dangerous and hard to detect without sensors.

🎯 How DysphagiaGuard detects it:
• A cough during/after swallowing = airway protection attempt
• UNSAFE classification = high IMU + high mic energy consistent with laryngeal penetration
• Multiple coughs → elevated aspiration probability

Current cough ratio: ${(coughRatio * 100).toInt()}% of events.
${if (coughRatio > 0.3f) "⚠️ HIGH — recommend SLP evaluation." else "✅ Within acceptable range."}
        """.trimIndent()
    }

    // ── Safe to eat ──────────────────────────────────────────────────────────
    if (q.contains("safe") && (q.contains("eat") || q.contains("feed") || q.contains("food"))) {
        return when {
            currentCls == "UNSAFE" -> """
❌ NOT SAFE to eat right now.

The last event was classified UNSAFE — possible laryngeal penetration detected.

Immediate steps:
1. Pause all oral feeding
2. Sit patient upright (90°)
3. Encourage a dry swallow or throat clear
4. Wait 3-5 minutes and retry with small sips
5. If repeated UNSAFE events → contact SLP or physician
            """.trimIndent()
            coughCount >= 3 || coughRatio > 0.35f -> """
⚠️ CAUTION — elevated cough activity this session.

$coughCount cough event(s) suggest the patient's airway protection is active. This may indicate aspiration risk.

Recommendations:
• Use thickened liquids (IDDSI Level 3-4)
• Chin-tuck posture during swallowing
• Smaller, slower bolus sizes
• Monitor for 5 more swallows before continuing
            """.trimIndent()
            unsafeCount == 0 && coughCount <= 1 -> """
✅ Current indicators suggest SAFE to continue eating.

Session stats: $totalSwallows swallows, $unsafeCount unsafe, $coughCount cough(s).

Maintain:
• Upright posture (90°)
• Appropriate food consistency
• Slow, deliberate swallows
• Alternate solid and liquid boluses
            """.trimIndent()
            else -> """
⚠️ Mixed signals — proceed with CAUTION.

$unsafeCount unsafe + $coughCount cough events this session.

Reduce oral intake rate and monitor closely. If 2 more unsafe events occur, pause feeding.
            """.trimIndent()
        }
    }

    // ── UNSAFE explanation ───────────────────────────────────────────────────
    if (q.contains("unsafe")) {
        return """
⚠️ UNSAFE Event — What it means:

Our classifier flagged a potential aspiration risk based on:

📡 Sensors detected:
• IMU RMS > 0.6g (strong laryngeal/chest movement)
• Mic envelope > 0.8 (high acoustic energy)
• Low zero-crossing rate (wet/gurgling quality)
• Duration 80-200ms (swallow timeframe)

This pattern matches a "wet gurgle" — the hallmark of laryngeal penetration or aspiration.

Current session: $unsafeCount UNSAFE event(s).
${if (unsafeCount >= 3) "🚨 3+ UNSAFE events — EMERGENCY protocol recommended." else ""}
        """.trimIndent()
    }

    // ── Emergency ────────────────────────────────────────────────────────────
    if (q.contains("emergency") || q.contains("choking")) {
        return """
🚨 EMERGENCY PROTOCOL:

IF CHOKING (can't speak/breathe):
1. Call emergency services IMMEDIATELY (102 / 112)
2. Perform Heimlich maneuver (5 abdominal thrusts)
3. If unconscious → CPR

IF ASPIRATION SUSPECTED (coughing, wet voice after eating):
1. STOP all oral feeding
2. Sit upright or lean forward
3. Encourage strong cough
4. Suction if available
5. Contact physician/SLP today

IF REPEATED UNSAFE EVENTS (3+):
→ DysphagiaGuard auto-sends SMS to caregiver
→ Switch to non-oral nutrition until SLP evaluation
        """.trimIndent()
    }

    // ── Session summary ──────────────────────────────────────────────────────
    if (q.contains("summary") || q.contains("session") || q.contains("report")) {
        val safeSwallows = totalSwallows - unsafeCount
        val risk = when {
            unsafeCount >= 3 || coughRatio > 0.4f -> "HIGH RISK"
            unsafeCount >= 1 || coughRatio > 0.2f -> "MODERATE RISK"
            else -> "LOW RISK"
        }
        return """
📊 Current Session Summary:

Total events:    $total
Safe swallows:   $safeSwallows
Unsafe events:   $unsafeCount
Cough events:    $coughCount
Cough ratio:     ${(coughRatio * 100).toInt()}%

Overall risk:    $risk

${when (risk) {
    "HIGH RISK" -> "🚨 Recommend: Pause oral feeding, contact SLP urgently."
    "MODERATE RISK" -> "⚠️ Recommend: Thickened liquids, slow pace, monitor closely."
    else -> "✅ Swallow pattern appears normal. Continue with standard diet."
}}
        """.trimIndent()
    }

    // ── Classification questions ─────────────────────────────────────────────
    if (q.contains("noise") || q.contains("artifact")) {
        return """
〰️ NOISE / Artifact Event:

This classification means the sensor detected motion or sound that does NOT match a swallow pattern:
• Too short duration (< 40ms)
• Inconsistent IMU + mic pattern
• Low confidence score (< 40%)

Common causes:
• Patient movement, head turn
• External ambient noise (speech, cough from nearby person)
• Sensor displacement

NOISE events are filtered out and NOT counted as swallows. No clinical action needed.
        """.trimIndent()
    }

    if (q.contains("how") && (q.contains("work") || q.contains("detect"))) {
        return """
🔬 How DysphagiaGuard Detects Swallows:

Hardware: ESP32 + MPU6050 IMU + Analog Microphone

Step 1 — Acquisition (1kHz)
  The IMU captures laryngeal movement; the mic captures pharyngeal sounds.

Step 2 — Signal Processing (20Hz windows)
  • IMU RMS: root mean square of 50-sample window
  • Mic Envelope: peak amplitude of window
  • Zero Crossing Rate: turbulence indicator

Step 3 — Classification Engine
  • Onset detected when IMU RMS > threshold
  • Duration measured from onset to offset
  • Confidence = weighted IMU + MIC score

Step 4 — Decision
  SAFE   → 100-200ms, moderate signals, low ZCR
  UNSAFE → 80-200ms, HIGH signals, low ZCR (wet gurgle)
  COUGH  → < 80ms, double-peak, HIGH ZCR (turbulent)
  NOISE  → irregular, low confidence

Step 5 → Alert via app (vibration) + SMS (caregiver)
        """.trimIndent()
    }

    // ── Fallback ─────────────────────────────────────────────────────────────
    return """
I understand you're asking about: "$query"

Current patient status: $currentCls
Session: $totalSwallows swallows | $unsafeCount unsafe | $coughCount coughs

Try asking:
• "Is it safe to eat now?"
• "What does COUGH mean?"
• "Explain UNSAFE event"
• "What is silent aspiration?"
• "Session summary"

Or describe what you're observing and I'll give clinical guidance.
    """.trimIndent()
}
