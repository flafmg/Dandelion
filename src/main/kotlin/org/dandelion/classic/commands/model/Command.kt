package org.dandelion.classic.commands.model

/**
 * Command is a marker interface for all command types in the system.
 *
 * Classes implementing this interface represent specific commands that can be registered and executed by the command registry.
 * To define your command, use the [org.dandelion.classic.commands.annotations.CommandDef] annotation on your class.
 * To define the executor method, use [org.dandelion.classic.commands.annotations.OnExecute] annotation on your method.
 * To define a subcommand, use [org.dandelion.classic.commands.annotations.OnSubCommand] annotation on your method.
 */
interface Command