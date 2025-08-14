package org.dandelion.classic.commands.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CommandDef(
    val name: String,
    val aliases: Array<String> = [],
    val description: String = "",
    val usage: String = "",
)
