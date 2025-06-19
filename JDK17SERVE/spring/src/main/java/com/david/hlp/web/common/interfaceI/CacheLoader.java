package com.david.hlp.web.common.interfaceI;

/**
 * 缓存加载接口
 *
 * @param <T> 缓存值的类型
 */
public interface CacheLoader<T> {
    /**
     * 加载数据
     *
     * @return 加载的数据
     */
    T load();

    /**
     * 获取类型信息
     * 
     * @return 泛型类型的 Class 对象
     */
    Class<T> getType();

    /**
     * 创建一个空的 T 类型实例
     * 实现类必须提供具体的空实例，不能返回 null
     *
     * @return 非空的 T 类型实例
     * @throws UnsupportedOperationException 如果无法创建空实例
     */
    default T emptyInstance() {
        try {
            return getType().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnsupportedOperationException("无法创建空实例: " + getType().getName(), e);
        }
    }
}
