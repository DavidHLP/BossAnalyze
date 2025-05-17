package com.david.hlp.crawler.ai.entity;

import java.util.Comparator;
import java.util.PriorityQueue;
public class CompareQueue<T> {
    private final PriorityQueue<T> queue;
    private final int maxSize;
    private final Comparator<? super T> comparator;

    /**
     * 构造方法 - 使用自定义比较器
     * @param maxSize 队列最大容量
     * @param comparator 自定义比较器
     */
    public CompareQueue(int maxSize, Comparator<? super T> comparator) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("队列大小必须大于0");
        }
        this.maxSize = maxSize;
        this.comparator = comparator;
        this.queue = new PriorityQueue<>(maxSize, comparator);
    }

    /**
     * 构造方法 - 使用元素的自然顺序
     * @param maxSize 队列最大容量
     */
    public CompareQueue(int maxSize) {
        this(maxSize, null);
    }

    /**
     * 添加元素到队列
     * @param element 要添加的元素
     * @return 如果队列已修改则返回true
     */
    public boolean add(T element) {
        if (element == null) {
            throw new NullPointerException("不能添加null元素");
        }

        if (queue.size() < maxSize) {
            return queue.offer(element);
        } else {
            // 获取队列中最小的元素（根据比较器）
            T minElement = queue.peek();
            
            // 比较新元素与最小元素
            int compareResult;
            if (comparator != null) {
                compareResult = comparator.compare(element, minElement);
            } else {
                @SuppressWarnings("unchecked")
                Comparable<? super T> comparableElement = (Comparable<? super T>) element;
                compareResult = comparableElement.compareTo(minElement);
            }

            // 如果新元素"大于"最小元素（根据比较器定义）
            if (compareResult > 0) {
                queue.poll(); // 移除最小元素
                return queue.offer(element); // 添加新元素
            }
            return false;
        }
    }

    /**
     * 获取队列中的元素数组
     * @return 包含队列元素的数组
     */
    @SuppressWarnings("unchecked")
    public T[] toArray() {
        return (T[]) queue.toArray();
    }

    /**
     * 获取队列当前大小
     * @return 队列中的元素数量
     */
    public int size() {
        return queue.size();
    }

    /**
     * 清空队列
     */
    public void clear() {
        queue.clear();
    }

    /**
     * 检查队列是否为空
     * @return 如果队列为空返回true
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 获取但不移除队列头元素
     * @return 队列头元素
     */
    public T peek() {
        return queue.peek();
    }

    /**
     * 获取并移除队列头元素
     * @return 队列头元素
     */
    public T poll() {
        return queue.poll();
    }
}