package com.tankwar.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 通用对象池（泛型）。
 * <p>高频产生/销毁的对象（如子弹、火花）通过复用减少 GC 压力，
 * 是典型的"享元 / 对象池"设计模式。子类（或工厂参数）负责创建新实例，
 * 取出时由调用方执行 reset 完成状态重置。</p>
 *
 * @param <T> 池化对象类型
 * @author TankWar Team
 */
public class ObjectPool<T> {

    /** 空闲对象栈。 */
    private final Deque<T> freeObjects = new ArrayDeque<T>();
    /** 对象工厂。 */
    private final ObjectFactory<T> factory;

    /**
     * 对象工厂接口。
     *
     * @param <T> 对象类型
     */
    public interface ObjectFactory<T> {
        /** @return 一个新实例。 */
        T create();
    }

    /**
     * 构造对象池。
     *
     * @param factory 新对象工厂
     */
    public ObjectPool(ObjectFactory<T> factory) {
        this.factory = factory;
    }

    /**
     * 取出一个对象：优先复用空闲对象，池空则新建。
     *
     * @return 可用对象（调用方需自行重置字段）
     */
    public T acquire() {
        T obj = freeObjects.pollFirst();
        return obj != null ? obj : factory.create();
    }

    /**
     * 归还对象，供下次复用。
     *
     * @param obj 已用完的对象
     */
    public void release(T obj) {
        if (obj != null) {
            freeObjects.offerFirst(obj);
        }
    }

    /** @return 当前空闲对象数量（监控/测试用）。 */
    public int idleCount() {
        return freeObjects.size();
    }
}
