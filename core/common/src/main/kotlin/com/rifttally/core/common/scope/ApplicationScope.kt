package com.rifttally.core.common.scope

import javax.inject.Qualifier

/** Qualifies the app-wide [kotlinx.coroutines.CoroutineScope] that outlives any screen. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class ApplicationScope
