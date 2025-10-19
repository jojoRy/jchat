package kr.jjory.jchat.service

import org.bukkit.Bukkit

class GlobalMessenger(private val config: ConfigService, private val logger: MessageLogManager) {
    private var recv: ((String) -> Unit)? = null
    private var packetSender: Any? = null
    private var sendMethod: java.lang.reflect.Method? = null
    private var isBound = false
    companion object { var channelSender: ((String) -> Unit)? = null }
    fun tryBind(): Boolean {
        if (isBound) return true
        return try {
            val senderKlass = try { Class.forName("kr.hqservice.framework.global.netty.api.PacketSender") }
                                catch (_: Throwable) { Class.forName("kr.hqservice.framework.netty.api.PacketSender") }
            val providerKlass = try { Class.forName("kr.hqservice.framework.global.netty.api.NettyProvider") } catch (_: Throwable) { null }
            packetSender = when {
                providerKlass != null -> { val inst = providerKlass.getMethod("get").invoke(null); providerKlass.getMethod("getPacketSender").invoke(inst) }
                else -> null
            }
            sendMethod = (senderKlass.methods.firstOrNull { m -> (m.name == "sendPacketToProxy" || m.name == "send") && m.parameterCount == 1 }
                ?: throw NoSuchMethodException("PacketSender.send*(obj) not found"))
            isBound = packetSender != null && sendMethod != null
            if (!isBound) Bukkit.getLogger().warning("[JChat] HQ Netty 바인딩 실패: PacketSender not found")
            isBound
        } catch (t: Throwable) { Bukkit.getLogger().warning("[JChat] HQ Netty 연결 불가: ${t.message}"); false }
    }
    fun initHandlers(onReceive: (String) -> Unit) { recv = onReceive }
    fun send(payload: String) {
        if (tryBind()) {
            try { sendMethod!!.invoke(packetSender, payload); logger.log("xserver-send: $payload"); return }
            catch (t: Throwable) { Bukkit.getLogger().warning("[JChat] HQ Netty send 실패: ${t.message}") }
        }
        channelSender?.invoke(payload) ?: logger.log("xserver-send(no-netty): $payload")
    }
    fun simulateReceive(payload: String) { Bukkit.getLogger().info("[JChat/XServer-Recv] $payload"); recv?.invoke(payload) }
}
