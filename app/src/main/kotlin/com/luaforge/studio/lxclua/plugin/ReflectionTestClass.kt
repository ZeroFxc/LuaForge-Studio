package com.luaforge.studio.lxclua.plugin

/**
 * 用于测试插件 Java 反射功能的测试类
 */
@Suppress("unused")
class ReflectionTestClass {
    
    companion object {
        @JvmStatic
        var staticField: String = "静态字段初始值"
        
        @JvmStatic
        var staticCounter: Int = 0
        
        @JvmStatic
        fun staticMethod(): String {
            return "静态方法返回值"
        }
        
        @JvmStatic
        fun staticMethodWithParams(a: Int, b: Int): Int {
            return a + b
        }
        
        @JvmStatic
        fun staticMethodWithException(): String {
            throw RuntimeException("这是一个测试异常")
        }
    }
    
    var instanceField: String = "实例字段初始值"
    
    private var privateField: String = "私有字段"
    
    var counter: Int = 0
    
    constructor()
    
    constructor(initialValue: String) {
        instanceField = initialValue
    }
    
    constructor(a: Int, b: Int) {
        counter = a + b
    }
    
    fun instanceMethod(): String {
        return "实例方法返回值: $instanceField"
    }
    
    fun instanceMethodWithParams(text: String, count: Int): String {
        return "$text 重复 $count 次: ${text.repeat(count)}"
    }
    
    fun increment(): Int {
        return ++counter
    }
    
    fun getPrivateField(): String {
        return privateField
    }
    
    fun setPrivateField(value: String) {
        privateField = value
    }
    
    override fun toString(): String {
        return "ReflectionTestClass(instanceField='$instanceField', counter=$counter)"
    }
}
