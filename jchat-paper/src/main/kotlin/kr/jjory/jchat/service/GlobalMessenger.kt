package kr.jjory.jchat.service

import org.bukkit.Bukkit
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class GlobalMessenger(private val config: ConfigService, private val logger: MessageLogManager) {
    private var recv: ((String) -> Unit)? = null
    private var packetSender: Any? = null
    private var sendMethod: Method? = null
    private var isBound = false

    companion object {
        var channelSender: ((String) -> Unit)? = null
    }

    fun tryBind(): Boolean {
        if (isBound) return true
        return try {
            val senderKlass = resolveClass(
                "kr.hqservice.framework.global.netty.api.PacketSender",
                "kr.hqservice.framework.netty.api.PacketSender",
            ) ?: throw ClassNotFoundException("PacketSender class not found")

            val providerKlass = resolveClass(
                "kr.hqservice.framework.global.netty.api.NettyProvider",
                "kr.hqservice.framework.netty.api.NettyProvider",
            )

            val providerInstance = providerKlass?.let { findSingleton(it) }
            packetSender = providerInstance?.let { inst ->
                findAccessor(providerKlass, senderKlass, inst)
            } ?: findSingleton(senderKlass)

            sendMethod = findSendMethod(senderKlass)
            isBound = packetSender != null && sendMethod != null
            if (!isBound) Bukkit.getLogger().warning("[JChat] HQ Netty 바인딩 실패: PacketSender not found")
            isBound
        } catch (t: Throwable) {
            Bukkit.getLogger().warning("[JChat] HQ Netty 연결 불가: ${t.message}")
            false
        }
    }

    fun initHandlers(onReceive: (String) -> Unit) {
        recv = onReceive
    }

    fun send(payload: String) {
        if (tryBind()) {
            try {
                sendMethod!!.invoke(packetSender, payload); logger.log("xserver-send: $payload"); return
            } catch (t: Throwable) {
                Bukkit.getLogger().warning("[JChat] HQ Netty send 실패: ${t.message}")
            }
        }
        channelSender?.invoke(payload) ?: logger.log("xserver-send(no-netty): $payload")
    }

    fun simulateReceive(payload: String) {
        recv?.invoke(payload)
    }

    private fun resolveClass(vararg names: String): Class<*>? = names.firstNotNullOfOrNull { name ->
        try {
            Class.forName(name)
        } catch (_: Throwable) {
            null
        }
    }
    private fun findSingleton(klass: Class<*>): Any? {
        methodsOf(klass)
            .filter { method ->
                Modifier.isStatic(method.modifiers) && method.parameterCount == 0 &&
                        klass.isAssignableFrom(method.returnType) &&
                        (method.name == "getInstance" || method.name == "get" || method.name == "INSTANCE")
            }
            .forEach { method ->
                method.isAccessible = true
                runCatching { method.invoke(null) }.getOrNull()?.let { return it }
            }

        fieldsOf(klass)
            .filter { field ->
                Modifier.isStatic(field.modifiers) && klass.isAssignableFrom(field.type)
            }
            .forEach { field ->
                field.isAccessible = true
                runCatching { field.get(null) }.getOrNull()?.let { return it }
            }

        return null
    }

    private fun findAccessor(holderKlass: Class<*>, targetKlass: Class<*>, instance: Any): Any? {
        methodsOf(holderKlass)
            .filter { method ->
                method.parameterCount == 0 && targetKlass.isAssignableFrom(method.returnType) &&
                        (method.name == "getPacketSender" || method.name == "get" || method.name == "packetSender")
            }
            .forEach { method ->
                method.isAccessible = true
                runCatching { method.invoke(instance) }.getOrNull()?.let { return it }
            }

        fieldsOf(holderKlass)
            .filter { field -> targetKlass.isAssignableFrom(field.type) }
            .forEach { field ->
                field.isAccessible = true
                runCatching { field.get(instance) }.getOrNull()?.let { return it }
            }

        return null
    }

    private fun findSendMethod(senderKlass: Class<*>): Method? {
        return methodsOf(senderKlass)
            .firstOrNull { method ->
                method.parameterCount == 1 &&
                        (method.name == "sendPacketToProxy" || method.name == "send")
            }
            ?.also { it.isAccessible = true }
    }

    private fun methodsOf(klass: Class<*>): Sequence<Method> = sequence {
        klass.methods.forEach { yield(it) }
        klass.declaredMethods.forEach { yield(it) }
    }

    private fun fieldsOf(klass: Class<*>): Sequence<Field> = sequence {
        klass.fields.forEach { yield(it) }
        klass.declaredFields.forEach { yield(it) }
    }
}

