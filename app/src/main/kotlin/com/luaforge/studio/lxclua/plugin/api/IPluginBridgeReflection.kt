package com.luaforge.studio.lxclua.plugin.api

/**
 * Java 反射调用相关 API
 */
interface IPluginBridgeReflection {
    /**
     * 加载 Java 类
     * @param className 完整类名
     * @return Class 对象，找不到返回 null
     */
    fun loadClass(className: String): Class<*>?
    
    /**
     * 创建 Java 对象实例
     * @param className 类名
     * @param args 构造函数参数数组
     * @return 创建的对象实例
     */
    fun newInstance(className: String, args: Array<Any?>?): Any?
    
    /**
     * 调用静态方法
     * @param className 类名
     * @param methodName 方法名
     * @param args 方法参数数组
     * @return 方法返回值
     */
    fun callStaticMethod(className: String, methodName: String, args: Array<Any?>?): Any?
    
    /**
     * 调用实例方法
     * @param obj 对象实例
     * @param methodName 方法名
     * @param args 方法参数数组
     * @return 方法返回值
     */
    fun callMethod(obj: Any, methodName: String, args: Array<Any?>?): Any?
    
    /**
     * 获取静态字段值
     * @param className 类名
     * @param fieldName 字段名
     * @return 字段值
     */
    fun getStaticField(className: String, fieldName: String): Any?
    
    /**
     * 设置静态字段值
     * @param className 类名
     * @param fieldName 字段名
     * @param value 新值
     */
    fun setStaticField(className: String, fieldName: String, value: Any?)
    
    /**
     * 获取实例字段值
     * @param obj 对象实例
     * @param fieldName 字段名
     * @return 字段值
     */
    fun getField(obj: Any, fieldName: String): Any?
    
    /**
     * 设置实例字段值
     * @param obj 对象实例
     * @param fieldName 字段名
     * @param value 新值
     */
    fun setField(obj: Any, fieldName: String, value: Any?)
}