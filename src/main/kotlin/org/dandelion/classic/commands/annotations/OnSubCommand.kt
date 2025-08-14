package org.dandelion.classic.commands.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnSubCommand(
    val name: String,
    val aliases: Array<String> = [""],
    val description: String = "",
    val usage: String = "",
)
