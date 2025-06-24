package org.dandelion.classic.commands.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ArgRange(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE
)