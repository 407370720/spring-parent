package com.emily.infrastructure.rpc.core.protocol;


/**
 * @program: spring-parent
 * @description: 自定义RPC传输协议
 * @author: Emily
 * @create: 2021/09/17
 */
public class InvokerProtocol {
    /**
     * 类名
     */
    private String className;
    /**
     * 函数名称
     */
    private String methodName;
    /**
     * 参数类型
     */
    private Class<?>[] types;
    /**
     * 参数列表
     */
    private Object[] params;


    public InvokerProtocol(){}

    public InvokerProtocol(String className, String methodName, Class<?>[] types, Object[] params) {
        this.className = className;
        this.methodName = methodName;
        this.types = types;
        this.params = params;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getTypes() {
        return types;
    }

    public void setTypes(Class<?>[] types) {
        this.types = types;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

}
