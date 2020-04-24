package org.openredstone.managers

import org.javacord.api.DiscordApi
import org.openredstone.commands.*
import org.openredstone.listeners.DiscordCommandListener
import org.openredstone.listeners.IrcCommandListener
import org.openredstone.model.entity.CommandEntity
import org.openredstone.model.entity.ConfigEntity

data class AttemptedCommand(val reply: String, val privateReply: Boolean)

class CommandManager(val discordApi: DiscordApi, private val config: ConfigEntity) {
    private val commands = mutableMapOf<String, Command>()
    private val listeners = listOf(
        DiscordCommandListener(this),
        IrcCommandListener(this, config)
    )

    fun startListeners() {
        listeners.forEach { it.listen() }
    }

    fun addCommands(vararg commandsToAdd: Command) {
        commandsToAdd.forEach { commands[it.name] = it }
    }

    fun addStaticCommands(vararg commandEntities: CommandEntity) {
        commandEntities.forEach {
            val staticCommand = StaticCommand(it.context, it.name, it.reply)
            commands[staticCommand.name] = staticCommand
        }
    }

    fun getAttemptedCommand(commandContext: CommandContext, message: String): AttemptedCommand? {
        if (message.isEmpty() || message[0] != config.commandChar) {
            return null
        }

        val args = message.split(" ")

        val executedCommand = commands[parseCommandName(args)]?.let {
            if (it.type.appliesTo(commandContext)) it else null
        } ?: ErrorCommand

        return if (args.size - 1 < executedCommand.requireParameters) {
            AttemptedCommand(
                "Invalid number of arguments passed to command `${executedCommand.name}`",
                executedCommand.privateReply
            )
        } else {
            AttemptedCommand(
                executedCommand.runCommand(args.drop(1)),
                executedCommand.privateReply
            )
        }
    }


    private fun CommandContext.appliesTo(other: CommandContext) = when (this) {
        CommandContext.BOTH -> true
        else -> other == this
    }

    private fun parseCommandName(parts: List<String>) = parts[0].substring(1)
}


