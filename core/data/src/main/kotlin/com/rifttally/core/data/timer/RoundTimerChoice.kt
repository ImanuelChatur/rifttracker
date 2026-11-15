package com.rifttally.core.data.timer

/**
 * Setup's remembered "Round timer" choice: off, or on with [minutes]. [minutes] is kept while off, so
 * turning the timer back on restores the last length.
 */
public data class RoundTimerChoice(val enabled: Boolean = false, val minutes: Int = DEFAULT_MINUTES) {
    public companion object {
        public const val DEFAULT_MINUTES: Int = 50
        public const val MIN_MINUTES: Int = 5
        public const val MAX_MINUTES: Int = 120

        /** One stepper tap. */
        public const val STEP_MINUTES: Int = 5
    }
}
