package com.example.medisync.ui.components

import android.graphics.RectF
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp

sealed class BodyZone(val label: String) {
    object HeadNeck : BodyZone("Head & Neck")
    object UpperChest : BodyZone("Upper Chest")
    object Abdomen : BodyZone("Abdomen")
    object Pelvis : BodyZone("Pelvis")
    object Arms : BodyZone("Arms")
    object Legs : BodyZone("Legs")
}

@Composable
fun TriageMannequinView(
    modifier: Modifier = Modifier,
    onZoneSelected: (BodyZone) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val strokeColor = Color(0xFF4353FF)
    val strokeWidth = 2.dp

    // Interactive paths for hit testing
    val paths = remember { mutableStateMapOf<BodyZone, Path>() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    paths.forEach { (zone, path) ->
                        if (isPointInPath(offset, path)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onZoneSelected(zone)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val scale = height / 500f // Adjusted scale for better fitting
        
        // Draw Front Mannequin
        drawSegmentedMannequin(
            centerOffset = Offset(width * 0.3f, height * 0.45f),
            scale = scale,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth.toPx(),
            onPathGenerated = { zone, path -> paths[zone] = path }
        )

        // Draw Back Mannequin
        drawSegmentedMannequin(
            centerOffset = Offset(width * 0.7f, height * 0.45f),
            scale = scale,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth.toPx(),
            onPathGenerated = { _, _ -> } // Interaction only on primary zones map
        )
    }
}

private fun isPointInPath(offset: Offset, path: Path): Boolean {
    val androidPath = path.asAndroidPath()
    val rectF = RectF()
    androidPath.computeBounds(rectF, true)
    val region = Region()
    region.setPath(androidPath, Region(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt()))
    return region.contains(offset.x.toInt(), offset.y.toInt())
}

fun DrawScope.drawSegmentedMannequin(
    centerOffset: Offset,
    scale: Float,
    strokeColor: Color,
    strokeWidth: Float,
    onPathGenerated: (BodyZone, Path) -> Unit
) {
    val cx = centerOffset.x
    val cy = centerOffset.y
    val style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // 1. Head & Neck
    val headPath = Path().apply {
        addOval(Rect(center = Offset(cx, cy - 180f * scale), radius = 22f * scale))
        // Neck
        moveTo(cx - 8f * scale, cy - 158f * scale)
        lineTo(cx - 8f * scale, cy - 145f * scale)
        moveTo(cx + 8f * scale, cy - 158f * scale)
        lineTo(cx + 8f * scale, cy - 145f * scale)
    }
    drawPath(headPath, strokeColor, style = style)
    onPathGenerated(BodyZone.HeadNeck, headPath)

    // 2. Upper Chest (Trapezoid)
    val chestPath = Path().apply {
        moveTo(cx - 45f * scale, cy - 145f * scale)
        lineTo(cx + 45f * scale, cy - 145f * scale)
        lineTo(cx + 40f * scale, cy - 90f * scale)
        lineTo(cx - 40f * scale, cy - 90f * scale)
        close()
    }
    drawPath(chestPath, strokeColor, style = style)
    onPathGenerated(BodyZone.UpperChest, chestPath)

    // 3. Abdomen
    val abdomenPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(cx - 38f * scale, cy - 85f * scale, cx + 38f * scale, cy - 35f * scale),
                cornerRadius = CornerRadius(4f * scale)
            )
        )
    }
    drawPath(abdomenPath, strokeColor, style = style)
    onPathGenerated(BodyZone.Abdomen, abdomenPath)

    // 4. Pelvis (Inverted Trapezoid)
    val pelvisPath = Path().apply {
        moveTo(cx - 38f * scale, cy - 30f * scale)
        lineTo(cx + 38f * scale, cy - 30f * scale)
        lineTo(cx + 42f * scale, cy + 10f * scale)
        lineTo(cx - 42f * scale, cy + 10f * scale)
        close()
    }
    drawPath(pelvisPath, strokeColor, style = style)
    onPathGenerated(BodyZone.Pelvis, pelvisPath)

    // 5. Arms (Segmented: Upper and Lower)
    val armsPath = Path().apply {
        // Left Arm
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx - 65f * scale, cy - 140f * scale, cx - 50f * scale, cy - 70f * scale), CornerRadius(6f * scale)))
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx - 65f * scale, cy - 65f * scale, cx - 50f * scale, cy + 10f * scale), CornerRadius(6f * scale)))

        // Right Arm
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx + 50f * scale, cy - 140f * scale, cx + 65f * scale, cy - 70f * scale), CornerRadius(6f * scale)))
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx + 50f * scale, cy - 65f * scale, cx + 65f * scale, cy + 10f * scale), CornerRadius(6f * scale)))
    }
    drawPath(armsPath, strokeColor, style = style)
    onPathGenerated(BodyZone.Arms, armsPath)

    // 6. Legs (Segmented: Thigh and Calf)
    val legsPath = Path().apply {
        // Left Leg
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx - 35f * scale, cy + 15f * scale, cx - 15f * scale, cy + 120f * scale), CornerRadius(8f * scale)))
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx - 33f * scale, cy + 125f * scale, cx - 17f * scale, cy + 240f * scale), CornerRadius(8f * scale)))

        // Right Leg
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx + 15f * scale, cy + 15f * scale, cx + 35f * scale, cy + 120f * scale), CornerRadius(8f * scale)))
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(cx + 17f * scale, cy + 125f * scale, cx + 33f * scale, cy + 240f * scale), CornerRadius(8f * scale)))
    }
    drawPath(legsPath, strokeColor, style = style)
    onPathGenerated(BodyZone.Legs, legsPath)
}
