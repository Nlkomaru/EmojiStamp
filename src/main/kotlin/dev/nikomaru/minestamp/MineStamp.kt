package dev.nikomaru.minestamp

import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.nikomaru.minestamp.command.ColorEmojiCommand
import dev.nikomaru.minestamp.command.PlayerUtilCommand
import dev.nikomaru.minestamp.command.PublishTicketCommand
import dev.nikomaru.minestamp.command.ReloadCommand
import dev.nikomaru.minestamp.data.FileType
import dev.nikomaru.minestamp.data.LocalConfig
import dev.nikomaru.minestamp.files.Config
import dev.nikomaru.minestamp.listener.LoginEvent
import dev.nikomaru.minestamp.listener.TicketInteractEvent
import dev.nikomaru.minestamp.player.AbstractPlayerStampManager
import dev.nikomaru.minestamp.player.LocalPlayerStampManager
import dev.nikomaru.minestamp.player.S3PlayerStampManager
import dev.nikomaru.minestamp.stamp.AbstractStamp
import dev.nikomaru.minestamp.utils.command.StampParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import revxrsal.commands.bukkit.BukkitCommandHandler
import revxrsal.commands.ktx.supportSuspendFunctions
import java.awt.Font
import java.util.*


open class MineStamp: SuspendingJavaPlugin(), KoinComponent {
    lateinit var plugin: JavaPlugin
    override suspend fun onEnableAsync() {
        logger.info("Is starting on Thread:${Thread.currentThread().name}/${Thread.currentThread().id}/primaryThread=${Bukkit.isPrimaryThread()}")
        plugin = this
        setKoin()

        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdir()
        }
        val br = plugin.javaClass.classLoader.getResourceAsStream("emoji.properties")
        val emojiProperties = Properties()
        withContext(Dispatchers.IO) {
            emojiProperties.load(br)
        }

        loadKoinModules(module {
            single { emojiProperties }
            single {
                Font.createFont(
                    Font.TRUETYPE_FONT,
                    plugin.javaClass.classLoader.getResourceAsStream("NotoEmoji-VariableFont_wght.ttf")
                )
            }
        })
        logger.info("command setting")
        setCommand()
        logger.info("config setting")
        Config.loadConfig()
        logger.info("stamp manager setting")
        val stampManager: AbstractPlayerStampManager =
            if (get<LocalConfig>().type == FileType.S3) {
                S3PlayerStampManager()
            } else {
                LocalPlayerStampManager()
            }

        loadKoinModules(module {
            single<AbstractPlayerStampManager> { stampManager }
        })
        logger.info("listener setting")
        setListener()
    }

    private fun setKoin() {
        val appModule = module {
            single<MineStamp> { this@MineStamp }
        }

        GlobalContext.getOrNull() ?: GlobalContext.startKoin {
            printLogger()
            modules(appModule)
        }
    }

    private fun setCommand() {
        val commandHandle = BukkitCommandHandler.create(this)
        loadKoinModules(module {
            single { commandHandle }
        })

        commandHandle.setSwitchPrefix("--")
        commandHandle.setFlagPrefix("--")
        commandHandle.supportSuspendFunctions()

        commandHandle.setHelpWriter { command, _ ->
            java.lang.String.format(
                """
                <color:yellow>command: <color:gray>%s %s
                <color:yellow>description: <color:gray>%s
                
                """.trimIndent(),
                command.path.toList(),
                command.usage,
                command.description,
            )
        }

        commandHandle.registerValueResolver(AbstractStamp::class.java, StampParser)

        with(commandHandle) {
            register(ColorEmojiCommand())
            register(PublishTicketCommand())
            register(ReloadCommand())
            register(PlayerUtilCommand())
        }

    }

    private fun setListener() {
        server.pluginManager.registerSuspendingEvents(LoginEvent(), this)
        server.pluginManager.registerSuspendingEvents(TicketInteractEvent(), this)
    }

}