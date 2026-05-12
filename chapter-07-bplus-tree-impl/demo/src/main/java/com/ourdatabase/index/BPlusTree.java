package com.ourdatabase.index;

import java.util.*;

/**
 * B+树 —— 数据库索引的核心数据结构（完整Java实现）
 *
 * B+树的特性：
 * 1. 所有数据都存在叶子节点中
 * 2. 内部节点只做索引用（导航），不存实际数据
 * 3. 叶子节点之间通过链表连接（支持高效范围查询）
 * 4. 树是自平衡的（插入删除时会自动调整）
 *
 * @param <K> 键的类型（必须可比较,例如Integer、String）
 * @param <V> 值的类型
 */
public class BPlusTree<K extends Comparable<K>, V> {

    private final int order;
    private final int internalMaxKeys;
    private final int leafMaxKeys;
    private final int internalMinKeys;
    private final int leafMinEntries;
    private Node<K, V> root;
    private LeafNode<K, V> firstLeaf;

    public BPlusTree(int order) {
        this.order = order;
        this.internalMaxKeys = order - 1;
        this.leafMaxKeys = order - 1;
        this.internalMinKeys = (int) Math.ceil(order / 2.0) - 1;
        this.leafMinEntries = (int) Math.ceil(order / 2.0);
        this.root = new LeafNode<>();
        this.firstLeaf = (LeafNode<K, V>) root;
    }

    public BPlusTree() { this(4); }

    // ==================== 插入 ====================

    public void insert(K key, V value) {
        LeafNode<K, V> leaf = findLeaf(key);
        leaf.insertEntry(key, value);
        if (leaf.entryCount() > leafMaxKeys) {
            splitLeaf(leaf);
        }
    }

    // ==================== 查找 ====================

    public V search(K key) {
        LeafNode<K, V> leaf = findLeaf(key);
        return leaf.getValue(key);
    }

    public List<Map.Entry<K, V>> rangeSearch(K startKey, K endKey) {
        List<Map.Entry<K, V>> result = new ArrayList<>();
        LeafNode<K, V> current = findLeaf(startKey);

        while (current != null) {
            for (int i = 0; i < current.entryCount(); i++) {
                K key = current.getKey(i);
                if (key.compareTo(startKey) >= 0 && key.compareTo(endKey) <= 0) {
                    result.add(new AbstractMap.SimpleEntry<>(key, current.getValue(i)));
                }
                if (key.compareTo(endKey) > 0) return result;
            }
            current = current.nextLeaf;
        }
        return result;
    }

    // ==================== 删除 ====================

    public boolean delete(K key) {
        LeafNode<K, V> leaf = findLeaf(key);
        boolean deleted = leaf.deleteEntry(key);
        if (!deleted) return false;
        if (leaf != root && leaf.entryCount() < leafMinEntries) {
            handleLeafUnderflow(leaf);
        }
        return true;
    }

    // ==================== 内部实现 ====================

    private LeafNode<K, V> findLeaf(K key) {
        Node<K, V> current = root;
        while (!current.isLeaf()) {
            InternalNode<K, V> internal = (InternalNode<K, V>) current;
            int childIdx = internal.findChildIndex(key);
            current = internal.getChild(childIdx);
        }
        return (LeafNode<K, V>) current;
    }

    private void splitLeaf(LeafNode<K, V> leaf) {
        LeafNode<K, V> newLeaf = new LeafNode<>();
        int splitPoint = leaf.entryCount() / 2;

        for (int i = splitPoint; i < leaf.entryCount(); ) {
            newLeaf.insertEntry(leaf.getKey(i), leaf.getValue(i));
            leaf.deleteEntry(leaf.getKey(i));
        }

        newLeaf.nextLeaf = leaf.nextLeaf;
        leaf.nextLeaf = newLeaf;

        K promoteKey = newLeaf.getKey(0);

        if (leaf == root) {
            InternalNode<K, V> newRoot = new InternalNode<>();
            newRoot.insertKey(promoteKey);
            newRoot.setChild(0, leaf);
            newRoot.setChild(1, newLeaf);
            root = newRoot;
        } else {
            insertIntoParent(leaf, promoteKey, newLeaf);
        }
    }

    private void splitInternal(InternalNode<K, V> internal) {
        InternalNode<K, V> newInternal = new InternalNode<>();
        int splitPoint = internal.keyCount() / 2;
        K middleKey = internal.getKey(splitPoint);

        int newChildIdx = 0;
        for (int i = splitPoint + 1; i <= internal.keyCount(); i++) {
            newInternal.setChild(newChildIdx, internal.getChild(i));
            newChildIdx++;
        }

        int newKeyIdx = 0;
        for (int i = splitPoint + 1; i < internal.keyCount(); ) {
            newInternal.insertKey(internal.getKey(i));
            internal.deleteKey(i);
        }
        internal.deleteKey(splitPoint);

        if (internal == root) {
            InternalNode<K, V> newRoot = new InternalNode<>();
            newRoot.insertKey(middleKey);
            newRoot.setChild(0, internal);
            newRoot.setChild(1, newInternal);
            root = newRoot;
        } else {
            insertIntoParent(internal, middleKey, newInternal);
        }
    }

    private void insertIntoParent(Node<K, V> oldNode, K promoteKey, Node<K, V> newNode) {
        InternalNode<K, V> parent = oldNode.parent;

        int pos = -1;
        for (int i = 0; i <= parent.keyCount(); i++) {
            if (parent.getChild(i) == oldNode) { pos = i; break; }
        }

        parent.insertKey(pos, promoteKey);
        parent.setChild(pos + 1, newNode);

        if (parent.keyCount() > internalMaxKeys) {
            splitInternal(parent);
        }
    }

    private void handleLeafUnderflow(LeafNode<K, V> leaf) {
        InternalNode<K, V> parent = leaf.parent;
        if (parent == null) return;

        int pos = -1;
        for (int i = 0; i <= parent.keyCount(); i++) {
            if (parent.getChild(i) == leaf) { pos = i; break; }
        }

        // 从左兄弟借
        if (pos > 0) {
            LeafNode<K, V> leftSibling = (LeafNode<K, V>) parent.getChild(pos - 1);
            if (leftSibling.entryCount() > leafMinEntries) {
                borrowFromLeft(leaf, leftSibling, parent, pos - 1);
                return;
            }
        }

        // 从右兄弟借
        if (pos < parent.keyCount()) {
            LeafNode<K, V> rightSibling = (LeafNode<K, V>) parent.getChild(pos + 1);
            if (rightSibling.entryCount() > leafMinEntries) {
                borrowFromRight(leaf, rightSibling, parent, pos);
                return;
            }
        }

        // 合并
        if (pos > 0) {
            LeafNode<K, V> leftSibling = (LeafNode<K, V>) parent.getChild(pos - 1);
            mergeLeaves(leftSibling, leaf, parent, pos - 1);
        } else {
            LeafNode<K, V> rightSibling = (LeafNode<K, V>) parent.getChild(pos + 1);
            mergeLeaves(leaf, rightSibling, parent, pos);
        }
    }

    private void borrowFromLeft(LeafNode<K, V> target, LeafNode<K, V> leftSibling,
                                InternalNode<K, V> parent, int keyPos) {
        K borrowedKey = leftSibling.getKey(leftSibling.entryCount() - 1);
        V borrowedValue = leftSibling.getValue(leftSibling.entryCount() - 1);
        leftSibling.deleteEntry(borrowedKey);
        target.insertEntry(borrowedKey, borrowedValue);
        parent.setKey(keyPos, borrowedKey);
    }

    private void borrowFromRight(LeafNode<K, V> target, LeafNode<K, V> rightSibling,
                                 InternalNode<K, V> parent, int keyPos) {
        K borrowedKey = rightSibling.getKey(0);
        V borrowedValue = rightSibling.getValue(0);
        rightSibling.deleteEntry(borrowedKey);
        target.insertEntry(borrowedKey, borrowedValue);
        parent.setKey(keyPos, rightSibling.getKey(0));
    }

    private void mergeLeaves(LeafNode<K, V> leftLeaf, LeafNode<K, V> rightLeaf,
                             InternalNode<K, V> parent, int keyPos) {
        while (rightLeaf.entryCount() > 0) {
            leftLeaf.insertEntry(rightLeaf.getKey(0), rightLeaf.getValue(0));
            rightLeaf.deleteEntry(rightLeaf.getKey(0));
        }
        leftLeaf.nextLeaf = rightLeaf.nextLeaf;
        parent.deleteKey(keyPos);
        for (int i = keyPos + 1; i < parent.keyCount() + 1; i++) {
            parent.setChild(i, parent.getChild(i + 1));
        }
        if (parent == root && parent.keyCount() == 0) {
            root = leftLeaf;
            leftLeaf.parent = null;
        }
    }

    // ==================== 打印 ====================

    public void printStructure() {
        System.out.println("\n===== B+树结构 =====");
        if (root.isLeaf()) {
            System.out.println("（树只有一层，根节点就是叶子节点）");
        } else {
            System.out.print("内部节点层：");
            printInternal((InternalNode<K, V>) root);
            System.out.println();
        }
        System.out.print("叶子节点链表：");
        LeafNode<K, V> current = firstLeaf;
        while (current != null) {
            System.out.print("[");
            for (int i = 0; i < current.entryCount(); i++) {
                System.out.print(current.getKey(i) + "→" + current.getValue(i));
                if (i < current.entryCount() - 1) System.out.print(", ");
            }
            System.out.print("]");
            if (current.nextLeaf != null) System.out.print(" → ");
            current = current.nextLeaf;
        }
        System.out.println();
    }

    private void printInternal(InternalNode<K, V> node) {
        System.out.print("[");
        for (int i = 0; i < node.keyCount(); i++) {
            System.out.print(node.getKey(i));
            if (i < node.keyCount() - 1) System.out.print(" | ");
        }
        System.out.print("] ");
        for (int i = 0; i <= node.keyCount(); i++) {
            Node<K, V> child = node.getChild(i);
            if (child != null && !child.isLeaf()) {
                printInternal((InternalNode<K, V>) child);
            }
        }
    }

    public int size() {
        int count = 0;
        LeafNode<K, V> current = firstLeaf;
        while (current != null) {
            count += current.entryCount();
            current = current.nextLeaf;
        }
        return count;
    }

    // ==================== 抽象节点 ====================

    private static abstract class Node<K extends Comparable<K>, V> {
        protected InternalNode<K, V> parent;
        abstract boolean isLeaf();
    }

    // ==================== 内部节点 ====================

    private static class InternalNode<K extends Comparable<K>, V> extends Node<K, V> {
        private List<K> keyList = new ArrayList<>();
        private List<Node<K, V>> childList = new ArrayList<>();

        InternalNode() {
            for (int i = 0; i < 4; i++) childList.add(null);
        }

        @Override
        boolean isLeaf() { return false; }

        int findChildIndex(K key) {
            for (int i = 0; i < keyList.size(); i++) {
                if (key.compareTo(keyList.get(i)) <= 0) return i;
            }
            return keyList.size();
        }

        void insertKey(K key) { insertKey(keyList.size(), key); }

        void insertKey(int pos, K key) {
            keyList.add(pos, key);
            childList.add(null);
        }

        void deleteKey(int pos) { keyList.remove(pos); }
        void deleteKey(K key) { keyList.remove(key); }
        void setKey(int pos, K key) { keyList.set(pos, key); }
        K getKey(int pos) { return keyList.get(pos); }
        int keyCount() { return keyList.size(); }

        void setChild(int pos, Node<K, V> child) {
            while (childList.size() <= pos) childList.add(null);
            childList.set(pos, child);
            if (child != null) child.parent = this;
        }

        Node<K, V> getChild(int pos) {
            return pos < childList.size() ? childList.get(pos) : null;
        }
    }

    // ==================== 叶子节点 ====================

    private static class LeafNode<K extends Comparable<K>, V> extends Node<K, V> {
        private List<K> keyList = new ArrayList<>();
        private List<V> valueList = new ArrayList<>();
        LeafNode<K, V> nextLeaf = null;

        @Override
        boolean isLeaf() { return true; }

        void insertEntry(K key, V value) {
            int pos = 0;
            while (pos < keyList.size() && keyList.get(pos).compareTo(key) < 0) pos++;

            if (pos < keyList.size() && keyList.get(pos).compareTo(key) == 0) {
                valueList.set(pos, value);
            } else {
                keyList.add(pos, key);
                valueList.add(pos, value);
            }
        }

        boolean deleteEntry(K key) {
            int pos = -1;
            for (int i = 0; i < keyList.size(); i++) {
                if (keyList.get(i).compareTo(key) == 0) { pos = i; break; }
            }
            if (pos == -1) return false;
            keyList.remove(pos);
            valueList.remove(pos);
            return true;
        }

        V getValue(K key) {
            for (int i = 0; i < keyList.size(); i++) {
                if (keyList.get(i).compareTo(key) == 0) return valueList.get(i);
            }
            return null;
        }

        K getKey(int pos) { return keyList.get(pos); }
        V getValue(int pos) { return valueList.get(pos); }
        int entryCount() { return keyList.size(); }
    }
}
