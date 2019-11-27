package com.monnage.gradle.report

internal class TimeFormatter {

    companion object {

        fun formatMillis(millis: Long): String {
            var elapsedTime = "${millis}ms"
            if (millis > 1000) {
                elapsedTime = "${millis / 1000}s"
            }
            if (millis / 1000 > 60) {
                val elapsedMinutes = millis / 1000 / 60
                val secondsRest = millis / 1000 % 60
                elapsedTime = "${elapsedMinutes}m ${secondsRest}s"
            }
            return elapsedTime
        }
    }
}
