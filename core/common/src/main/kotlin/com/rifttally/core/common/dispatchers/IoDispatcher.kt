package com.rifttally.core.common.dispatchers

import javax.inject.Qualifier

/** Qualifies the [kotlinx.coroutines.CoroutineDispatcher] used for disk and database work. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class IoDispatcher
