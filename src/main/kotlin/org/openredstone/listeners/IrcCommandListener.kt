package org.openredstone.listeners

import kotlin.concurrent.thread

import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.MessageEvent

import org.openredstone.commands.Commands
import org.openredstone.getAttemptedCommand
import org.openredstone.model.entity.ConfigEntity

class IrcCommandListener(
    private val commands: Commands,
    private val config: ConfigEntity
) : Listener {
    class IrcListener(private val commands: Commands, private val config: ConfigEntity) : ListenerAdapter() {
        override fun onMessage(event: MessageEvent) {
            val command = getAttemptedCommand(config, event.message, commands) ?: return
            if (command.privateReply) {
                event.user?.send()?.message(command.reply)
            } else {
                event.channel.send().message(command.reply)
            }
        }
    }

    override fun listen() {
        val ircBot = PircBotX(
            Configuration.Builder()
                .setName(config.irc.name)
                .addServer(config.irc.server)
                .addAutoJoinChannel(config.irc.channel)
                .setNickservPassword(config.irc.password)
                .addListener(IrcListener(commands, config))
                .setAutoReconnect(true)
                .buildConfiguration()
        )
        thread {
            ircBot.startBot()
        }
    }
}
