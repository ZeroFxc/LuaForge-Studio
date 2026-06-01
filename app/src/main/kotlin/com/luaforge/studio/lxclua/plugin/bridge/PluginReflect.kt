package com.luaforge.studio.lxclua.plugin.bridge

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Java 反射操作 API
 * 
 * 使用方式: plugin.reflect.loadClass("className")
 */
class PluginReflect {
    
    /**
     * 加载 Java 类
     */
    fun loadClass(className: String): Class<*>? {
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            null
        }
    }
    
    /**
     * 创建类实例（无参构造）
     */
    fun newInstance(className: String): Any? {
        return try {
            val clazz = Class.forName(className)
            clazz.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建类实例（带参数）
     */
    fun newInstance(className: String, args: Array<Any?>): Any? {
        return try {
            val clazz = Class.forName(className)
            val argArray = args
            val constructor = findBestConstructor(clazz, argArray)
            constructor?.apply { isAccessible = true }
            constructor?.newInstance(*convertArgs(constructor.parameterTypes, argArray))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 调用实例方法
     */
    fun callMethod(instance: Any, methodName: String, args: Array<Any?>?): Any? {
        return try {
            val argArray = args ?: emptyArray()
            val method = findBestMethod(instance.javaClass, methodName, argArray)
            if (method == null) {
                return null
            }
            method.isAccessible = true
            val convertedArgs = convertArgs(method.parameterTypes, argArray)
            method.invoke(instance, *convertedArgs)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 调用静态方法
     */
    fun callStaticMethod(className: String, methodName: String, args: Array<Any?>?): Any? {
        return try {
            val clazz = Class.forName(className)
            val argArray = args ?: emptyArray()
            val method = findBestMethod(clazz, methodName, argArray)
            if (method == null) {
                return null
            }
            method.isAccessible = true
            val convertedArgs = convertArgs(method.parameterTypes, argArray)
            method.invoke(null, *convertedArgs)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取实例字段值（支持私有字段）
     */
    fun getField(instance: Any, fieldName: String): Any? {
        return try {
            val field = findField(instance.javaClass, fieldName)
            field?.apply { isAccessible = true }?.get(instance)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 设置实例字段值（支持私有字段）
     */
    fun setField(instance: Any, fieldName: String, value: Any?) {
        try {
            val field = findField(instance.javaClass, fieldName)
            field?.apply { 
                isAccessible = true 
                set(instance, convertValueForField(this, value))
            }
        } catch (e: Exception) {
        }
    }

    /**
     * 获取静态字段值（支持私有字段）
     */
    fun getStaticField(className: String, fieldName: String): Any? {
        return try {
            val clazz = Class.forName(className)
            val field = findField(clazz, fieldName)
            field?.apply { isAccessible = true }?.get(null)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 设置静态字段值（支持私有字段）
     */
    fun setStaticField(className: String, fieldName: String, value: Any?) {
        try {
            val clazz = Class.forName(className)
            val field = findField(clazz, fieldName)
            field?.apply { 
                isAccessible = true 
                set(null, convertValueForField(this, value))
            }
        } catch (e: Exception) {
        }
    }

    // ============ 内部辅助方法 ============
    
    private fun findField(clazz: Class<*>, fieldName: String): Field? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                currentClass = currentClass.superclass
            }
        }
        return null
    }
    
    private fun findBestConstructor(clazz: Class<*>, args: Array<Any?>): Constructor<*>? {
        val constructors = clazz.declaredConstructors
        return constructors.firstOrNull { isCompatible(it.parameterTypes, args) }
    }

    private fun findBestMethod(clazz: Class<*>, methodName: String, args: Array<Any?>): Method? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            val methods = currentClass.declaredMethods.filter { it.name == methodName }
            val matched = methods.firstOrNull { isCompatible(it.parameterTypes, args) }
            if (matched != null) {
                return matched
            }
            currentClass = currentClass.superclass
        }
        return null
    }

    private fun isCompatible(paramTypes: Array<Class<*>>, args: Array<Any?>): Boolean {
        if (paramTypes.size != args.size) return false
        
        for (i in paramTypes.indices) {
            val arg = args[i]
            if (arg == null) {
                if (!paramTypes[i].isPrimitive) continue
                return false
            }
            if (!isTypeCompatible(paramTypes[i], arg.javaClass)) {
                return false
            }
        }
        return true
    }

    private fun isTypeCompatible(paramType: Class<*>, argType: Class<*>): Boolean {
        if (paramType.isAssignableFrom(argType)) return true
        
        if (paramType == Int::class.javaPrimitiveType) {
            return argType == Int::class.java || 
                   argType == Long::class.java || 
                   argType == Short::class.java || 
                   argType == Byte::class.java ||
                   Number::class.java.isAssignableFrom(argType)
        }
        
        if (paramType == Long::class.javaPrimitiveType) {
            return argType == Long::class.java || 
                   argType == Int::class.java ||
                   Number::class.java.isAssignableFrom(argType)
        }
        
        if (paramType == Double::class.javaPrimitiveType) {
            return argType == Double::class.java || 
                   argType == Float::class.java ||
                   Number::class.java.isAssignableFrom(argType)
        }
        
        if (paramType == Float::class.javaPrimitiveType) {
            return argType == Float::class.java || 
                   argType == Double::class.java ||
                   Number::class.java.isAssignableFrom(argType)
        }
        
        if (paramType == Boolean::class.javaPrimitiveType) {
            return argType == Boolean::class.java
        }
        
        if (paramType == Int::class.java) {
            return Number::class.java.isAssignableFrom(argType)
        }
        
        return paramType == argType
    }

    private fun convertArgs(paramTypes: Array<Class<*>>, args: Array<Any?>): Array<Any?> {
        return paramTypes.mapIndexed { index, paramType ->
            convertValue(paramType, args.getOrNull(index))
        }.toTypedArray()
    }

    private fun convertValue(paramType: Class<*>, value: Any?): Any? {
        if (value == null) return null
        
        if (paramType.isAssignableFrom(value.javaClass)) {
            return value
        }
        
        return when (paramType) {
            Int::class.javaPrimitiveType, Int::class.java -> 
                (value as? Number)?.toInt() ?: value
            
            Long::class.javaPrimitiveType, Long::class.java -> 
                (value as? Number)?.toLong() ?: value
            
            Float::class.javaPrimitiveType, Float::class.java -> 
                (value as? Number)?.toFloat() ?: value
            
            Double::class.javaPrimitiveType, Double::class.java -> 
                (value as? Number)?.toDouble() ?: value
            
            Short::class.javaPrimitiveType, Short::class.java -> 
                (value as? Number)?.toShort() ?: value
            
            Byte::class.javaPrimitiveType, Byte::class.java -> 
                (value as? Number)?.toByte() ?: value
            
            Boolean::class.javaPrimitiveType, Boolean::class.java -> 
                value as? Boolean ?: value
            
            String::class.java -> 
                value.toString()
            
            else -> value
        }
    }

    private fun convertValueForField(field: Field, value: Any?): Any? {
        if (value == null) return null
        
        if (field.type.isAssignableFrom(value.javaClass)) {
            return value
        }
        
        return when (field.type) {
            Int::class.javaPrimitiveType, Int::class.java -> 
                (value as? Number)?.toInt() ?: value
            
            Long::class.javaPrimitiveType, Long::class.java -> 
                (value as? Number)?.toLong() ?: value
            
            Float::class.javaPrimitiveType, Float::class.java -> 
                (value as? Number)?.toFloat() ?: value
            
            Double::class.javaPrimitiveType, Double::class.java -> 
                (value as? Number)?.toDouble() ?: value
            
            String::class.java -> 
                value.toString()
            
            else -> value
        }
    }
}
