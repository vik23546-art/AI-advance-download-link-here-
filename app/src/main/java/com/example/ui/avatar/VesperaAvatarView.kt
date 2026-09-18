package com.example.ui.avatar

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentPink
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.MouthPinkRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterRadialCenter
import com.example.ui.theme.WireframePurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AvatarStyle {
    HINATA_3D,
    WIREFRAME_SPHERE
}

@Composable
fun VesperaAvatarView(
    isSpeaking: Boolean,
    audioAmplitude: Float,
    isVoiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onAvatarTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var avatarStyle by remember { mutableStateOf(AvatarStyle.HINATA_3D) }

    // Infinite animation transition for idle breathing, ambient lighting & lip-sync oscillation
    val infiniteTransition = rememberInfiniteTransition(label = "VesperaAvatarAnimation")

    // Idle breathing vertical sway
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingOffset"
    )

    // Breathing subtle scale
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // High frequency mouth oscillation when speaking
    val mouthOscillation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MouthOscillation"
    )

    // Continuous rotation angle for 3D wireframe mode & ambient cyber aura
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Dynamic lip-sync scale calculated from prototype logic:
    // "if (isSpeaking) { hinataMesh.scale.y = 1 + Math.sin(Date.now() * 0.02) * 0.08; } else { hinataMesh.scale.y = 1; }"
    val dynamicLipSyncScale = if (isSpeaking) {
        val baseWiggle = (mouthOscillation - 0.5f) * 0.12f
        val ampBoost = audioAmplitude * 0.16f
        1f + baseWiggle + ampBoost
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WaterRadialCenter.copy(alpha = if (isSpeaking) 0.95f else 0.80f),
                        DeepMidnight
                    ),
                    radius = 500f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating cyber soundwave particles / aura
        AvatarBackgroundAura(
            isSpeaking = isSpeaking,
            audioAmplitude = audioAmplitude,
            rotation = continuousRotation
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        ) {
            // Avatar Display Container
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .offset(y = breathingOffset.dp)
                    .scale(breathingScale)
                    .testTag("avatar_container")
                    .clickable { onAvatarTap() },
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing halo (matching HTML v1.5 linear-gradient(135deg, #0a84ff, #ff2d55))
                Box(
                    modifier = Modifier
                        .size(184.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0A84FF),
                                    Color(0xFFFF2D55)
                                )
                            )
                        )
                        .border(
                            width = if (isSpeaking) 3.dp else 2.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF0A84FF), Color(0xFFFF2D55))
                            ),
                            shape = CircleShape
                        )
                )

                // Inner avatar rendering
                if (avatarStyle == AvatarStyle.HINATA_3D) {
                    HinataAvatarStage(
                        lipSyncScaleY = dynamicLipSyncScale,
                        isSpeaking = isSpeaking,
                        audioAmplitude = audioAmplitude
                    )
                } else {
                    WireframeSphereStage(
                        rotation = continuousRotation,
                        isSpeaking = isSpeaking,
                        audioAmplitude = audioAmplitude
                    )
                }

                // Dynamic speech wave ring around the avatar
                if (isSpeaking) {
                    Canvas(modifier = Modifier.size(190.dp)) {
                        val radius = size.minDimension / 2f - 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            color = GlowingCyan.copy(alpha = 0.45f + (audioAmplitude * 0.4f)),
                            radius = radius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Companion identity & Mode / Audio Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                // Status pill
                Surface(
                    color = CardSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderViolet.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                 .size(8.dp)
                                 .clip(CircleShape)
                                 .background(if (isSpeaking) GlowingCyan else Color(0xFF2ED573))
                         )
                         Spacer(modifier = Modifier.width(6.dp))
                         Text(
                             text = if (isSpeaking) "Speaking… 🎙️" else "● Online & Unrestricted",
                             style = MaterialTheme.typography.labelSmall.copy(
                                 color = if (isSpeaking) GlowingCyan else Color(0xFF2ED573),
                                 fontWeight = FontWeight.SemiBold,
                                 fontSize = 11.sp
                             )
                         )
                    }
                }

                // Toggle 3D style (Hinata Sprite <-> Wireframe Sphere)
                Surface(
                    color = CardSurface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderViolet.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .testTag("toggle_avatar_mode")
                        .clickable {
                            avatarStyle = if (avatarStyle == AvatarStyle.HINATA_3D) {
                                AvatarStyle.WIREFRAME_SPHERE
                            } else {
                                AvatarStyle.HINATA_3D
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Avatar Mode",
                            tint = SoftLavender,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (avatarStyle == AvatarStyle.HINATA_3D) "3D Hinata" else "Wireframe",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Voice Mute/Unmute toggle
                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CardSurface.copy(alpha = 0.8f))
                        .testTag("toggle_voice_button")
                ) {
                    Icon(
                        imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (isVoiceEnabled) "Voice enabled" else "Voice muted",
                        tint = if (isVoiceEnabled) GlowingCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Hinata 3D styled character avatar stage with dynamic lip-sync mouth scaling
 * based on the user's prompt logic:
 * hinataMesh.scale.y = 1 + Math.sin(Date.now() * 0.02) * 0.08;
 */
@Composable
private fun HinataAvatarStage(
    lipSyncScaleY: Float,
    isSpeaking: Boolean,
    audioAmplitude: Float
) {
    Box(
        modifier = Modifier
            .size(176.dp)
            .clip(CircleShape)
            .background(DeepMidnight),
        contentAlignment = Alignment.Center
    ) {
        // Character Artwork (Bust portrait centered)
        Image(
            painter = painterResource(id = R.drawable.img_hinata_avatar),
            contentDescription = "Vespera Hinata 3D Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Subtle dynamic head bounce / lip-sync reaction
                    scaleY = lipSyncScaleY
                    scaleX = 1f + ((lipSyncScaleY - 1f) * 0.25f)
                }
        )

        // Lip-Sync Soundwave / Mouth visualizer overlay when speaking
        if (isSpeaking) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                MouthAudioWaveform(amplitude = audioAmplitude)
            }
        }
    }
}

/**
 * Three.js inspired 3D Wireframe Cyber-Sphere
 * Directly reflects the user's Three.js scene:
 * THREE.SphereGeometry(1.2, 32, 32) wireframe
 * THREE.BoxGeometry(0.5, 0.1, 0.1) mouth mesh
 */
@Composable
private fun WireframeSphereStage(
    rotation: Float,
    isSpeaking: Boolean,
    audioAmplitude: Float
) {
    Canvas(
        modifier = Modifier
            .size(176.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F0C24))
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension * 0.40f

        // Draw radial cyber glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(WireframePurple.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(centerX, centerY),
                radius = radius * 1.3f
            ),
            radius = radius * 1.2f,
            center = Offset(centerX, centerY)
        )

        // Draw wireframe latitude circles (#5856d6)
        val latitudeLines = 7
        for (i in 1..latitudeLines) {
            val latFraction = i.toFloat() / (latitudeLines + 1)
            val yOffset = (latFraction - 0.5f) * 2f * radius
            val rAtY = kotlin.math.sqrt((radius * radius - yOffset * yOffset).coerceAtLeast(0f))
            drawOval(
                color = WireframePurple.copy(alpha = 0.55f),
                topLeft = Offset(centerX - rAtY, centerY + yOffset - (rAtY * 0.28f)),
                size = Size(rAtY * 2f, rAtY * 0.56f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Draw wireframe longitude rotating ellipses (#5856d6)
        val rotRad = (rotation * PI / 180.0).toFloat()
        val numMeridians = 6
        for (m in 0 until numMeridians) {
            val angle = rotRad + (m * PI.toFloat() / numMeridians)
            val cosA = cos(angle.toDouble()).toFloat()
            val width = (radius * cosA).coerceAtLeast(-radius)
            drawOval(
                color = WireframePurple.copy(alpha = 0.65f),
                topLeft = Offset(centerX - kotlin.math.abs(width), centerY - radius),
                size = Size(kotlin.math.abs(width) * 2f, radius * 2f),
                style = Stroke(width = 1.3.dp.toPx())
            )
        }

        // 3D Mouth mesh (Three.js: BoxGeometry with mouthMat: color 0xff2d55)
        val mouthScaleY = if (isSpeaking) {
            (1f + (audioAmplitude * 2.8f) + (sin(rotation * 0.3) * 0.6).toFloat()).coerceIn(1f, 3.8f)
        } else {
            1f
        }

        val mouthWidth = 42.dp.toPx()
        val mouthBaseHeight = 7.dp.toPx()
        val currentMouthHeight = mouthBaseHeight * mouthScaleY
        val mouthY = centerY + (radius * 0.45f)

        // Mouth mesh (#ff2d55)
        drawRoundRect(
            color = MouthPinkRed,
            topLeft = Offset(centerX - (mouthWidth / 2f), mouthY - (currentMouthHeight / 2f)),
            size = Size(mouthWidth, currentMouthHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(currentMouthHeight / 2f),
        )

        // Expressive Eye Nodes
        val eyeSpacing = radius * 0.42f
        val eyeY = centerY - (radius * 0.2f)
        drawCircle(
            color = WireframePurple,
            radius = 4.dp.toPx(),
            center = Offset(centerX - eyeSpacing, eyeY)
        )
        drawCircle(
            color = WireframePurple,
            radius = 4.dp.toPx(),
            center = Offset(centerX + eyeSpacing, eyeY)
        )
    }
}

/**
 * Animated soundwave equalizer displaying audio intensity when Vespera speaks
 */
@Composable
private fun MouthAudioWaveform(amplitude: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(CardSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, BorderViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        val bars = 5
        for (i in 0 until bars) {
            val factor = when (i) {
                0, 4 -> 0.55f
                1, 3 -> 0.82f
                else -> 1.0f
            }
            val barHeight = (4.dp + (14.dp * amplitude * factor)).coerceIn(4.dp, 18.dp)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(GlowingCyan, NeonPurple)
                        )
                    )
            )
        }
    }
}

/**
 * Decorative background cyber aura
 */
@Composable
private fun AvatarBackgroundAura(
    isSpeaking: Boolean,
    audioAmplitude: Float,
    rotation: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.45f)
        val maxRadius = size.minDimension * 0.48f

        // Orbiting particle rings
        val particles = 8
        for (i in 0 until particles) {
            val angle = (rotation + (i * 360f / particles)) * (PI / 180f).toFloat()
            val dist = maxRadius * (0.85f + (sin((rotation * 0.05f + i).toDouble()) * 0.12f).toFloat())
            val px = center.x + cos(angle.toDouble()).toFloat() * dist
            val py = center.y + sin(angle.toDouble()).toFloat() * (dist * 0.6f)
            
            drawCircle(
                color = if (i % 2 == 0) GlowingCyan.copy(alpha = 0.35f) else SoftLavender.copy(alpha = 0.35f),
                radius = if (isSpeaking) 3.5.dp.toPx() * (1f + audioAmplitude * 0.5f) else 2.5.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}
