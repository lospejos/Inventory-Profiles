package org.anti_ad.mc.ipnext.parser

import org.anti_ad.mc.common.Log
import org.anti_ad.mc.common.Savable
import org.anti_ad.mc.common.TellPlayer
import org.anti_ad.mc.common.annotation.MayThrow
import org.anti_ad.mc.common.extensions.div
import org.anti_ad.mc.common.extensions.exists
import org.anti_ad.mc.common.extensions.listFiles
import org.anti_ad.mc.common.extensions.name
import org.anti_ad.mc.common.extensions.tryOrPrint
import org.anti_ad.mc.common.extensions.writeToFile
import org.anti_ad.mc.common.gui.widgets.ConfigButtonClickHandler
import org.anti_ad.mc.common.integration.registerFromConfigFile
import org.anti_ad.mc.common.profiles.conifg.ProfileData
import org.anti_ad.mc.common.profiles.conifg.ProfilesConfig
import org.anti_ad.mc.common.util.LogicalStringComparator
import org.anti_ad.mc.common.vanilla.Vanilla.mc
import org.anti_ad.mc.common.vanilla.alias.ClientWorld
import org.anti_ad.mc.common.vanilla.glue.VanillaUtil
import org.anti_ad.mc.common.vanilla.glue.loggingPath
import org.anti_ad.mc.ipnext.config.ModSettings
import org.anti_ad.mc.ipnext.event.LockSlotsHandler
import org.anti_ad.mc.ipnext.item.rule.file.RuleFile
import org.anti_ad.mc.ipnext.item.rule.file.RuleFileRegister
import java.nio.file.Path

import kotlin.io.path.readText
import kotlin.io.path.writeText

private val strCmpLogical = LogicalStringComparator.file()

object ReloadRuleFileButtonInfoDelegate : ConfigButtonClickHandler() {

    override fun onClick(guiClick: () -> Unit) {
        TellPlayer.listenLog(Log.LogLevel.INFO) {
            RuleLoader.reload()
        }
        guiClick()
        val fileNames = RuleFileRegister.loadedFileNames.filter { it != RuleLoader.internalFileDisplayName }
        TellPlayer.chat("Reloaded ${fileNames.size} files: $fileNames")
    }
}

private val configFolder = VanillaUtil.configDirectory("inventoryprofilesnext")

private fun getFiles(regex: String) =
    configFolder.listFiles(regex).sortedWith { a, b ->
        strCmpLogical.compare(a.name,
                              b.name)
    }

private val definedLoaders: List<Loader> = listOf(LockSlotsLoader,
                                                  ProfilesLoader,
                                                  RuleLoader,
                                                  HintsLoader)

object ProfilesLoader: Loader, Savable {

    val file: Path
        get() {
            return configFolder / "profiles${serverIdentifier(ModSettings.PROFILES_PER_SERVER.booleanValue)}.txt"
        }

    val profiles = mutableListOf<ProfileData>()
    val savedProfiles = mutableListOf<ProfileData>()

    @MayThrow
    override fun save() {
        file.writeText(ProfilesConfig.asString(profiles) + "\n\n\n\n" + ProfilesConfig.asString(savedProfiles))
    }

    override fun load(clientWorld: Any?) {
        reload(clientWorld as ClientWorld?)
    }

    override fun reload(clientWorld: ClientWorld?) {
        profiles.clear()
        savedProfiles.clear()
        val temp = mutableListOf<ProfileData>()
        if (file.exists()) {
            tryOrPrint({msg->
                           Log.warn(msg)
                           TellPlayer.chat("Loading Profile settings failed: $msg")}) {
                temp.addAll(ProfilesConfig.getProfiles(file.readText()))
            }
        }
        savedProfiles.addAll(temp.filter { it.name.uppercase() == "SAVED" })
        profiles.addAll(temp.filter { it.name.uppercase() != "SAVED" })
    }
}

// ============
// loader
// ============

interface Loader {
    fun reload(clientWorld: ClientWorld? = null)
}

object CustomDataFileLoader {
    private val loaders = mutableListOf<Loader>()

    fun load(clientWorld: ClientWorld) {
        reload(clientWorld)
    }

    fun reload(clientWorld: ClientWorld) {
        loaders.forEach { it.reload(clientWorld) }
    }

    init {
        loaders.addAll(definedLoaders)
    }
}

// ============
// lock slots loader
// ============

object LockSlotsLoader : Loader, Savable {
    private var cachedWorld: ClientWorld? = null
    val file: Path
    get() {
        return configFolder / "lockSlots${serverIdentifier(ModSettings.ENABLE_LOCK_SLOTS_PER_SERVER.booleanValue)}.txt"
    }


    private var cachedValue = listOf<Int>()

    override fun save() {
        try {
            val slotIndices = LockSlotsHandler.lockedInvSlotsStoredValue.sorted()
            if (slotIndices == cachedValue) return
            cachedValue = slotIndices
            slotIndices.joinToString("\n").writeToFile(file)
        } catch (e: Exception) {
            Log.error("Failed to write file ${file.loggingPath}")
        }
    }

    override fun load(clientWorld: Any?) {
        val world = clientWorld as ClientWorld
        internalLoad(world)
    }

    private fun internalLoad(clientWorld: ClientWorld?) {
        this.cachedWorld = clientWorld
        cachedValue = listOf()
        try {
            if (!file.exists()) {
                LockSlotsHandler.lockedInvSlotsStoredValue.clear()
                return
            }
            val content = file.readText()
            val slotIndices = content.lines().mapNotNull { it.trim().toIntOrNull() }
            LockSlotsHandler.lockedInvSlotsStoredValue.apply {
                clear()
                addAll(slotIndices)
            }
            cachedValue = slotIndices
        } catch (e: Exception) {
            Log.error("Failed to read file ${file.loggingPath}")
        }
    }

    override fun reload(clientWorld: ClientWorld?) {
        internalLoad(clientWorld)
    }
}

// ============
// rule loader
// ============
object RuleLoader : Loader {
    const val internalFileDisplayName = "<internal rules.txt>"
    private val internalRulesTxtContent
        get() = VanillaUtil.getResourceAsString("inventoryprofilesnext:config/rules.txt") ?: ""
            .also { Log.error("Failed to load in-jar file inventoryprofilesnext:config/rules.txt") }
    private const val regex = "^rules\\.(?:.*\\.)?txt\$"

    override fun reload(clientWorld: ClientWorld?) {
        Log.clearIndent()
        Log.trace("[-] Rule reloading...")
        val files = getFiles(regex)
        val ruleFiles = mutableListOf(RuleFile(internalFileDisplayName,
                                               internalRulesTxtContent))
        for (file in files) {
            try {
                Log.trace("    Trying to read file ${file.name}")
                val content = file.readText()
                ruleFiles.add(RuleFile(file.name,
                                       content))
            } catch (e: Exception) {
                Log.error("Failed to read file ${file.loggingPath}")
            }
        }
        Log.trace("[-] Total ${ruleFiles.size} rule files (including <internal>)")
        RuleFileRegister.reloadRuleFiles(ruleFiles)
        Log.trace("Rule reload end")

        TemporaryRuleParser.onReload()
    }
}

object HintsLoader: Loader {
    var loaded = false
    override fun reload(clientWorld: ClientWorld?) {
        if (!loaded) {
            loaded = true
            registerFromConfigFile(configFolder / "ModIntegrationHints.json")
        }
    }

}

private fun serverIdentifier(perServer: Boolean): String = when {
    !perServer -> {
        ""
    }
    mc().isInSingleplayer -> {
        (mc().server?.levelName ?: "").sanitized()
    }
    mc().isConnectedToRealms -> {
        (mc().networkHandler?.connection?.address?.toString()?.replace("/","")?.replace(":","&") ?: "").sanitized()
    }
    mc().currentServerEntry != null -> {
        (mc().currentServerEntry?.address?.replace("/","")?.replace(":","&") ?: "").sanitized()
    }
    else -> {
        ""
    }
}

private fun String.sanitized(): String {
    if (this == ".." || this == ".") return "-dot_dot"
    return if (this.isNotEmpty()) {
        "-" + this.replace("/","(slash)")
            .replace("\\","(bslash)")
            .replace(":", "(colon)")
            .replace("<", "(lt)")
            .replace(">","(gt)")
            .replace("|","(pipe)")
            .replace("?","(qm)")
            .replace("*", "(asterisk)")
            .replace("\"","(dquote)")
    } else {
        this
    }
}
