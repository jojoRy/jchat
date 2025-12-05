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
        var channelSender: ((String) -> Boolean)? = null
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
            } ?: findSingleton(senderKlass) ?: findFromKoin(senderKlass)

            sendMethod = findSendMethod(senderKlass)
            isBound = packetSender != null && sendMethod != null
            if (!isBound) logger.log("hq-netty-bind-failed: PacketSender not found")
            isBound
        } catch (t: Throwable) {
            Bukkit.getLogger().warning("[JChat] HQ Netty 연결 불가: ${t.message}")
            false
        }
    }

    fun initHandlers(onReceive: (String) -> Unit) {
        recv = onReceive
    }

    fun send(payload: String): Boolean {
        if (tryBind()) {
            try {
                sendMethod!!.invoke(packetSender, payload)
                logger.log("xserver-send: $payload")
                return true
            } catch (t: Throwable) {
                Bukkit.getLogger().warning("[JChat] HQ Netty send 실패: ${t.message}")
            }
        }

        val sender = channelSender ?: run {
            logger.log("xserver-send(no-netty): $payload")
            return false
        }
        return try {
            sender.invoke(payload)
        } catch (t: Throwable) {
            Bukkit.getLogger().warning("[JChat] Channel send 실패: ${t.message}")
            false
        }
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
    private fun findFromKoin(targetKlass: Class<*>): Any? {
        val globalContextKlass = resolveClass("org.koin.core.context.GlobalContext") ?: return null
        val reflectionKlass = resolveClass("kotlin.jvm.internal.Reflection") ?: return null
        val kClassKlass = resolveClass("kotlin.reflect.KClass") ?: return null

        val context = runCatching { globalContextKlass.getMethod("get").invoke(null) }.getOrNull() ?: return null
        val koin = runCatching {
            context.javaClass.methods.firstOrNull { method ->
                method.name == "get" && method.parameterCount == 0 && method.returnType.name == "org.koin.core.Koin"
            }?.apply { isAccessible = true }?.invoke(context)
        }.getOrNull() ?: return null

        val getOrCreate = runCatching { reflectionKlass.getMethod("getOrCreateKotlinClass", Class::class.java) }.getOrNull()
            ?: return null
        val kClass = runCatching { getOrCreate.invoke(null, targetKlass) }.getOrNull() ?: return null

        val qualifierKlass = resolveClass("org.koin.core.qualifier.Qualifier")
        val function0Klass = resolveClass("kotlin.jvm.functions.Function0")
        val koinClass = koin.javaClass

        val defaultMethod = runCatching {
            if (qualifierKlass != null && function0Klass != null) {
                koinClass.getMethod(
                    "getOrNull\$default",
                    koinClass,
                    kClassKlass,
                    qualifierKlass,
                    function0Klass,
                    Int::class.javaPrimitiveType,
                    Any::class.java
                )
            } else null
        }.getOrNull()

        if (defaultMethod != null) {
            defaultMethod.isAccessible = true
            val resolved = runCatching { defaultMethod.invoke(null, koin, kClass, null, null, 3, null) }.getOrNull()
            if (resolved != null) return resolved
        }

        val directMethod = runCatching {
            koinClass.methods.firstOrNull { method ->
                method.name == "getOrNull" && method.parameterCount >= 1 && method.parameterTypes[0].name == "kotlin.reflect.KClass"
            }?.apply { isAccessible = true }
        }.getOrNull() ?: return null

        val args = when (directMethod.parameterCount) {
            1 -> arrayOf(kClass)
            2 -> arrayOf(kClass, null)
            else -> arrayOf(kClass, null, null)
        }

        return runCatching { directMethod.invoke(koin, *args) }.getOrNull()
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

