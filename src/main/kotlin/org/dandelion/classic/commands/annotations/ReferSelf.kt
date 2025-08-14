package org.dandelion.classic.commands.annotations

// what does this do: if the executor is a player
// and the position defined by argposition in the arguments is empty
// the player will "refer itself"
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReferSelf(val argPosition: Int = 0)
