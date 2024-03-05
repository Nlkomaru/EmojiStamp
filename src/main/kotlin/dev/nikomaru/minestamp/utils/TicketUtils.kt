package dev.nikomaru.minestamp.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nikomaru.minestamp.MineStamp
import dev.nikomaru.minestamp.stamp.AbstractStamp
import dev.nikomaru.minestamp.stamp.EmojiStamp
import dev.nikomaru.minestamp.utils.Utils.mm
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object TicketUtils: KoinComponent {
    val plugin: MineStamp by inject()

    fun getRandomTicket(jwt: String): ItemStack {
        val ticket = ItemStack(Material.PAPER)
        val meta = ticket.itemMeta
        meta.displayName(mm.deserialize("<rainbow>ランダムチケット"))
        meta.lore(listOf(mm.deserialize("<green>右クリックを押して、スタンプチケットを生成")))
        val namespaceKey = NamespacedKey(plugin, "ticket")
        meta.persistentDataContainer.set(namespaceKey, PersistentDataType.STRING, jwt)
        ticket.itemMeta = meta
        return ticket
    }

    fun getUniqueTicket(algorithm : Algorithm, stamp: AbstractStamp): ItemStack {
        val ticket = ItemStack(Material.PAPER)
        val jwt = JWT.create().withIssuer("minestamp")
            .withClaim("type", "unique")
            .withClaim("shortCode", stamp.shortCode)
            .sign(algorithm)

        val meta = ticket.itemMeta
        lateinit var type: String
        lateinit var preview: String
        if (stamp is EmojiStamp) {
            type = "絵文字"
            preview = stamp.char
        } else {
            type = "画像"
            preview = ""
        }

        meta.displayName(mm.deserialize("絵文字チケット $preview"))
        meta.lore(
            listOf(
                mm.deserialize("<green>右クリックを押して、スタンプを取得"),
                mm.deserialize("<gray>タイプ: $type"),
                mm.deserialize("<gray>スタンプ: ${stamp.shortCode}")
            )
        )
        val namespaceKey = NamespacedKey(plugin, "ticket")
        meta.persistentDataContainer.set(namespaceKey, PersistentDataType.STRING, jwt)
        ticket.itemMeta = meta
        return ticket
    }
}