package dk.schulz.quiettype.accessibility

data class OverlayPosition(
    val xDp: Int,
    val yDp: Int,
)

object OverlayPlacementPolicy {
    val DefaultPosition = OverlayPosition(
        xDp = 16,
        yDp = 320,
    )

    fun toPx(position: OverlayPosition, density: Float): Pair<Int, Int> =
        (position.xDp * density).toInt().coerceAtLeast(0) to
            (position.yDp * density).toInt().coerceAtLeast(0)

    fun fromPx(x: Int, y: Int, density: Float): OverlayPosition = OverlayPosition(
        xDp = (x / density).toInt().coerceAtLeast(0),
        yDp = (y / density).toInt().coerceAtLeast(0),
    )
}
