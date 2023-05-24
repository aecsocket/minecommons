package io.github.aecsocket.alexandria.paper

import cloud.commandframework.bukkit.CloudBukkitCapabilities
import cloud.commandframework.context.CommandContext
import cloud.commandframework.execution.CommandExecutionCoordinator
import cloud.commandframework.paper.PaperCommandManager
import io.github.aecsocket.alexandria.hook.HookCommand
import org.bukkit.command.CommandSender

typealias Context = CommandContext<CommandSender>

abstract class AlexandriaCommand(
    private val hook: AlexandriaPlugin<*>,
    manager: PaperCommandManager<CommandSender> = PaperCommandManager(
        hook,
        CommandExecutionCoordinator.simpleCoordinator(),
        { it }, { it },
    ).apply {
        if (hasCapability(CloudBukkitCapabilities.BRIGADIER))
            registerBrigadier()
    },
) : HookCommand<CommandSender>(hook, manager)
