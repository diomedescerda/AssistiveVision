package com.unimagdalena.assistivevision.model

import android.content.Context
import com.unimagdalena.assistivevision.R

/**
 * Resolves localized spoken text (object class names and spatial zones)
 * from Android string resources.
 *
 * This is the Context-touching layer; the pure sentence assembly lives in
 * [SpeechFormatter].
 */
object LocalizedLabels {

    /**
     * Returns the localized name for a COCO class id used by the detector.
     */
    fun classLabel(context: Context, classId: Int): String = when (classId) {
        1 -> context.getString(R.string.class_person)
        62 -> context.getString(R.string.class_chair)
        63 -> context.getString(R.string.class_couch)
        65 -> context.getString(R.string.class_bed)
        67 -> context.getString(R.string.class_table)
        70 -> context.getString(R.string.class_toilet)
        72 -> context.getString(R.string.class_tv)
        73 -> context.getString(R.string.class_computer)
        81 -> context.getString(R.string.class_sink)
        82 -> context.getString(R.string.class_refrigerator)
        else -> context.getString(R.string.class_person)
    }

    /**
     * Returns the localized label for a spatial zone string produced by
     * [com.unimagdalena.assistivevision.modules.LocalizationModule].
     */
    fun zoneLabel(context: Context, zone: String): String = when (zone) {
        "left_top" -> context.getString(R.string.zone_left_top)
        "left_bottom" -> context.getString(R.string.zone_left_bottom)
        "center_top" -> context.getString(R.string.zone_center_top)
        "center_bottom" -> context.getString(R.string.zone_center_bottom)
        "right_top" -> context.getString(R.string.zone_right_top)
        "right_bottom" -> context.getString(R.string.zone_right_bottom)
        else -> context.getString(R.string.zone_center_bottom)
    }

    /**
     * Short form of a zone for the compact on-screen label (speech uses the
     * longer phrasing from [zoneLabel]).
     */
    fun zoneShortLabel(context: Context, zone: String): String = when (zone) {
        "left_top" -> context.getString(R.string.zone_short_left_top)
        "left_bottom" -> context.getString(R.string.zone_short_left_bottom)
        "center_top" -> context.getString(R.string.zone_short_center_top)
        "center_bottom" -> context.getString(R.string.zone_short_center_bottom)
        "right_top" -> context.getString(R.string.zone_short_right_top)
        "right_bottom" -> context.getString(R.string.zone_short_right_bottom)
        else -> context.getString(R.string.zone_short_center_bottom)
    }
}