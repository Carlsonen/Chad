package org.openredstone.managers

import org.javacord.api.DiscordApi
import org.openredstone.commands.*
import org.openredstone.listeners.DiscordCommandListener
import org.openredstone.listeners.IrcCommandListener
import org.openredstone.model.entity.CommandEntity
import org.openredstone.model.entity.ConfigEntity

class CommandManager(val discordApi: DiscordApi, private val config: ConfigEntity) {
    private val commands = mutableListOf<Command>()
    private val listeners = listOf(
        DiscordCommandListener(this),
        IrcCommandListener(this, config)
    )

    fun startListeners() {
        listeners.forEach { it.listen() }
    }

    fun addCommands(vararg commandsToAdd: Command) {
        commands.addAll(commandsToAdd)
    }

    fun addStaticCommands(commandEntities: List<CommandEntity>) {
        commandEntities.forEach { commands.add(StaticCommand(it.context, it.name, it.reply)) }
    }

    fun getAttemptedCommand(commandContext: CommandContext, message: String): Command? {
        if (message.isEmpty() || message[0] != config.commandChar) {
            return null
        }

        val args = message.split(" ")

        val executedCommand = commands.asSequence()
            .filter { command ->
                command.name == parseCommandName(args) && commandContext.appliesTo(command.type)
            }.firstOrNull() ?: ErrorCommand()

        if (args.size - 1 < executedCommand.requireParameters) {
            executedCommand.reply = "Invalid number of arguments passed to command `${executedCommand.name}`"
        } else {
            executedCommand.runCommand(args.drop(1))
        }
        return executedCommand
    }


    private fun CommandContext.appliesTo(other: CommandContext) = when (this) {
        CommandContext.BOTH -> true
        else -> other == this
    }

    private fun parseCommandName(parts: List<String>) = parts[0].substring(1)
}


