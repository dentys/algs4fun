package com.github.dtyshchenko.algs4fun.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Original task is
 * <a href="https://leetcode.com/problems/lfu-cache/">LFU Cache</a>
 *
 * Implementation reflects java api of Map for put operation.
 * Implementation idea taken from the following resources :
 * <ul>
 *     <li><a href="http://dhruvbird.com/lfu.pdf"/>http://dhruvbird.com/lfu.pdf</li>
 *     <li><a href="http://www.laurentluce.com/posts/least-frequently-used-cache-eviction-scheme-with-complexity-o1-in-python/"/>
 *     http://www.laurentluce.com/posts/least-frequently-used-cache-eviction-scheme-with-complexity-o1-in-python/</li>
 * </ul>
 *
 * Implementation uses O(1) complexity for put and get operations.
 *
 * Created by denis on 1/30/17.
 */
public class LFUCache<K, V> {
    private final int maxSize;
    private final Map<K, Node<K, V>> storage;
    private FreqNode<K, V> head;

    public LFUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max number of cache elements should be greater than zero");
        }
        this.maxSize = maxSize;
        storage = new HashMap<>(1 << maxSize);
    }

    public V put(K key, V value) {
        if (maxSize <= storage.size()) {
            //trigger eviction
            //in case of eviction head will be always non null as maxSize can't be 0
            Node<K, V> lru = head.removeLeastRecentlyUsed();
            //update head if frequency node were removed due to lru nodes absence
            head = head.isEmpty() ? head.next : head;

            storage.remove(lru.key);
        }
        Node<K, V> node = new Node<>(key, value);

        if (head == null) {
            head = new FreqNode<>(1);
        }
        //node with frequency 1 may be already removed
        //if all elements with frequency one were promoted
        if (head.freq != 1) {
            FreqNode<K, V> first = new FreqNode<>(1);
            head.prev = first;
            first.next = head;
            head = first;
        }

        head.addAsMostRecentlyUsed(node);

        Node<K, V> previousNode = storage.put(key, node);
        //TODO: ensure previousNode is not current node (requires adding equals to Node)
        // to avoid unnecessary downgrade of node from higher to lower frequency
        // with potential risk of removal on subsequent insertions
        if (previousNode == null) {
            return null;
        }

        previousNode.removeFromCurrentFrequency();
        //update head if frequency node were removed due to lru nodes absence
        head = head.isEmpty() ? head.next : head;

        return previousNode.value;
    }

    public V get(K key) {
        Node<K, V> node = storage.get(key);
        if (node  == null) {
            return null;
        }
        node.promote();
        //update head if frequency node were removed due to lru nodes absence
        head = head.isEmpty() ? head.next : head;

        return node.value;
    }

    //TODO: contains should also have impact on element frequency access
    public boolean contains(K key) {
        return storage.containsKey(key);
    }

    private class FreqNode<KEY, VAL> {
        Node<KEY, VAL> mostRecentlyUsed;
        Node<KEY, VAL> leastRecentlyUsed;

        final int freq;

        FreqNode<KEY, VAL> prev;
        FreqNode<KEY, VAL> next;

        private FreqNode(int freq) {
            this.freq = freq;
        }

        void addAsMostRecentlyUsed(Node<KEY, VAL> node) {
            node.freqNode = this;
            if (leastRecentlyUsed == null) {
                mostRecentlyUsed = leastRecentlyUsed = node;
            } else {
                mostRecentlyUsed.moreRecentlyUsed = node;
                node.lessRecentlyUsed = mostRecentlyUsed;
                mostRecentlyUsed = node;
            }
        }

        boolean isEmpty() {
            return mostRecentlyUsed == null;
        }
        /**
         * Removes current freq node from doubly linked list
         * Should be triggered if there is no more node (lru) items in list
         */
        void removeFromFrequencyList() {
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }
        }

        Node<KEY, VAL> removeLeastRecentlyUsed() {
            Node<KEY, VAL> tmp = leastRecentlyUsed;
            removeFromLruList(leastRecentlyUsed);
            //TODO: clear frequency list if there is no elements any more
            return tmp;
        }

        void removeFromLruList(Node<KEY, VAL> node) {
            if (leastRecentlyUsed == mostRecentlyUsed) {
                //only one element in list and it is gonna be removed
                leastRecentlyUsed = mostRecentlyUsed = null;
                removeFromFrequencyList();
                return;
            }
            if (node != leastRecentlyUsed) {
                node.lessRecentlyUsed.moreRecentlyUsed = node.moreRecentlyUsed;
            } else {
                leastRecentlyUsed = node.moreRecentlyUsed;
                //forget link on node that is being removed
                leastRecentlyUsed.lessRecentlyUsed = null;
            }
            if (node != mostRecentlyUsed) {
                node.moreRecentlyUsed.lessRecentlyUsed = node.lessRecentlyUsed;
            } else {
                mostRecentlyUsed = node.lessRecentlyUsed;
                //forget link on node that is being removed
                mostRecentlyUsed.moreRecentlyUsed = null;
            }
        }
    }

    private class Node<KEY, VAL> {
        final KEY key;
        final VAL value;
        Node<KEY, VAL> moreRecentlyUsed;
        Node<KEY, VAL> lessRecentlyUsed;
        FreqNode<KEY, VAL> freqNode;

        public Node(KEY key, VAL value) {
            this.key = key;
            this.value = value;
        }

        void removeFromCurrentFrequency() {
            freqNode.removeFromLruList(this);
            //forget about previous siblings, new lru list, new life...
            moreRecentlyUsed = null;
            lessRecentlyUsed = null;
        }

        void promote() {
            freqNode.removeFromLruList(this);
            int nextFreq = freqNode.freq + 1;

            if (freqNode.next != null) {
                if (freqNode.next.freq != nextFreq) {
                    FreqNode<KEY, VAL> tmp = new FreqNode<>(nextFreq);
                    freqNode.next.prev = tmp;
                    tmp.next = freqNode.next;
                    freqNode.next = tmp;
                }
            } else {
                freqNode.next = new FreqNode<>(nextFreq);
                freqNode.next.prev = freqNode;
            }

            //Update pointer to frequency for current node
            freqNode = freqNode.next;
            //forget about previous siblings, new lru list, new life...
            moreRecentlyUsed = null;
            lessRecentlyUsed = null;
            freqNode.addAsMostRecentlyUsed(this);
        }
    }
}
