package org.tron.common.storage;

import java.util.LinkedList;

/**
 * Deposit Queue (FIFO) based on LinkedList
 *
 * @author Guo Yonggang
 * @since 28.04.2018
 */
public class DepositQueue<E> {

    public static int MAX_DEPOSIT_SIZE = 100;
    public static int DEFAULT_DEPOSIT_SIZE = 20;

    private int maxDepositSize = 0;
    private LinkedList<E> list = new LinkedList<>();

    public DepositQueue(int maxSize) {
        if (maxSize <= 0 || maxSize > MAX_DEPOSIT_SIZE) {
            maxDepositSize = DEFAULT_DEPOSIT_SIZE;
        } else {
            maxDepositSize = maxSize;
        }
    }

    /**
     *
     * @param obj
     */
    public void put(E obj) {
        list.addLast(obj);
    }

    /**
     *
     * @return
     */
    public E get() {
        if (list.isEmpty()) return null;

        return list.removeFirst();
    }

    /**
     *
     * @return
     */
    public E peek() {
        if (list.isEmpty()) return null;
        return list.getFirst();
    }

    /**
     *
     * @return
     */
    public E last() {
        if (list.isEmpty()) return null;
        return list.getLast();
    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     *
     * @return
     */
    public int size() {
        return list.size();
    }

    /**
     *
     */
    public E removeLast() {
        if (list.isEmpty()) return null;
        return list.removeLast();
    }

}
