package com.netify.app.domain

import com.netify.app.domain.model.ChannelCongestion

/** Turns raw per-channel AP counts into a human-readable recommendation. */
object ChannelAnalyzer {

    fun recommend(congestion: List<ChannelCongestion>): String {
        if (congestion.isEmpty() || congestion.all { it.apCount == 0 }) {
            return "Not enough nearby network data yet — pull to rescan."
        }
        val busiest = congestion.maxByOrNull { it.apCount } ?: return ""
        // 1, 6, 11 are the only non-overlapping 2.4 GHz channels.
        val nonOverlapping = listOf(1, 6, 11)
        val quietest = congestion.filter { it.channel in nonOverlapping }
            .minByOrNull { it.apCount }

        return if (quietest != null && quietest.channel != busiest.channel) {
            "Channel ${busiest.channel} is the busiest nearby (${busiest.apCount} networks) — " +
                "switching your router to channel ${quietest.channel} may reduce interference."
        } else {
            "Channel ${busiest.channel} has the most nearby networks (${busiest.apCount}). Your setup looks reasonably clear otherwise."
        }
    }
}
