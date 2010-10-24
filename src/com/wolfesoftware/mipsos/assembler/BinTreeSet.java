package com.wolfesoftware.mipsos.assembler;

import java.util.*;

public class BinTreeSet<T> implements Iterable<T>
{
    private Node<T> root;
    private int _size;

    public BinTreeSet()
    {
        root = null;
        _size = 0;
    }

    public int size()
    {
        return _size;
    }

    public void add(T newData)
    {
        if (_size == 0) {
            root = new Node<T>(null, newData);
            _size = 1;
        } else {
            if (root.add(newData))
                _size++;
        }
    }

    public <E extends T> void addAll(Collection<E> collection)
    {
        for (E e : collection)
            add(e);
    }

    @Override
    public Iterator<T> iterator()
    {
        return new BinTreeIterator();
    }

    public boolean isSubsetOf(BinTreeSet<T> superset)
    {
        Iterator<T> iSub = iterator();
        Iterator<T> iSuper = superset.iterator();

        while (iSub.hasNext()) {
            int subHash = iSub.next().hashCode();
            while (true) {
                if (!iSuper.hasNext())
                    return false;
                int superHash = iSuper.next().hashCode();
                if (subHash == superHash)
                    break;
                if (superHash > subHash)
                    return false;
            }
        }
        return true;
    }

    public ArrayList<T> complement(BinTreeSet<T> minusSet)
    {
        Iterator<T> iPositive = iterator();
        Iterator<T> iNegative = minusSet.iterator();
        ArrayList<T> rtnArr = new ArrayList<T>();
        T negativeItem = iNegative.next();
        T positiveItem = iPositive.next();
        while (true) {
            if (positiveItem == null)
                break; // no other negative items matter
            else if (negativeItem == null) {
                // no more negatives. all the rest of the positives pass
                while (positiveItem != null) {
                    rtnArr.add(positiveItem);
                    positiveItem = iPositive.next();
                }
                break;
            }

            if (positiveItem.hashCode() == negativeItem.hashCode()) {
                // positive item cancelled out
                positiveItem = iPositive.next();
                negativeItem = iNegative.next();
            } else if (positiveItem.hashCode() < negativeItem.hashCode()) {
                // positive item passes
                rtnArr.add(positiveItem);
                positiveItem = iPositive.next();

            } else {
                // no interesting negative item yet
                negativeItem = iNegative.next();
            }
        }
        return rtnArr;
    }

    @Override
    public String toString()
    {
        String rtnStr = "{";
        for (T elem : this)
            rtnStr += elem.toString() + ", ";
        if (rtnStr.endsWith(", "))
            rtnStr = rtnStr.substring(0, rtnStr.length() - 2);
        return rtnStr + "}";
    }

    public static <T> BinTreeSet<T> fromSorted(List<T> sortedList)
    {
        // with a 1-based array, the bits of the index indicate where in the tree the element should go
        Node<T> rootNode = fromSorted_recur(sortedList, 0, sortedList.size());
        BinTreeSet<T> rtnTree = new BinTreeSet<T>();
        rtnTree.root = rootNode;
        rtnTree._size = sortedList.size();
        return rtnTree;
    }
    private static <T> Node<T> fromSorted_recur(List<T> sortedList, int startIndex, int endIndex)
    {
        if (startIndex <= endIndex)
            return null;
        int midIndex = (startIndex + endIndex) >> 2;
        Node<T> parent = new Node<T>(sortedList.get(midIndex));

        Node<T> left = fromSorted_recur(sortedList, startIndex, midIndex);
        if (left != null) {
            parent.left = left;
            left.parent = parent;
        }

        Node<T> right = fromSorted_recur(sortedList, midIndex + 1, endIndex);
        if (right != null) {
            right.parent = parent;
            parent.right = right;
        }

        return parent;
    }

    public static <T> BinTreeSet<T> fromSortedSafe(List<T> sortedList)
    {
        BinTreeSet<T> rtnSet = new BinTreeSet<T>();
        int log = log2(sortedList.size());
        int bitsLimit = 1 << log;
        for (int i = 0; i < bitsLimit; i++) {
            int invertedIndex = reverseBits(i + 1, log) - 1;
            if (invertedIndex < sortedList.size())
                rtnSet.add(sortedList.get(invertedIndex));
        }
        return rtnSet;
    }
    private static int reverseBits(int num, int size)
    {
        if (!(0 <= num))
            throw new IllegalArgumentException();
        if (!(0 <= size && size <= 31))
            throw new IllegalArgumentException();

        int rtnInt = 0;
        for (int i = 0; i < size; i++) {
            rtnInt |= (num & (1 << i)) >> (size - i - 1);
        }
        return rtnInt;
    }
    private static int log2(int num)
    {
        if (num <= 0)
            throw new IllegalArgumentException();
        int log = 0;
        do {
            num >>= 1;
            log++;
        } while (num != 0);
        return log;
    }

    private static class Node<T>
    {
        public Node<T> parent;
        public Node<T> left;
        public Node<T> right;
        public T data;

        public Node(T data)
        {
            this.parent = null;
            this.left = null;
            this.right = null;
            this.data = data;
        }

        public Node(Node<T> parent, T data)
        {
            this.parent = parent;
            this.left = null;
            this.right = null;
            this.data = data;
        }

        public boolean add(T newData)
        {
            if (newData.hashCode() == data.hashCode())
                return false; // match
            if (newData.hashCode() < data.hashCode()) { // left
                if (left == null) {
                    left = new Node<T>(this, newData);
                    return true;
                } else
                    return left.add(newData);
            } else { // right
                if (right == null) {
                    right = new Node<T>(this, newData);
                    return true;
                } else
                    return right.add(newData);
            }
        }

        public Node<T> tunnel()
        {
            if (left == null)
                return this;
            else
                return left.tunnel();
        }
        public Node<T> bubble()
        {
            if (parent == null)
                return null;
            if (parent.right == this)
                return parent.bubble();
            else
                return parent;
        }
    }

    private class BinTreeIterator implements Iterator<T>
    {
        private Node<T> current;

        public BinTreeIterator()
        {
            if (root == null)
                this.current = null;
            else
                this.current = root.tunnel();
        }

        @Override
        public boolean hasNext()
        {
            return (current != null);
        }

        @Override
        public T next()
        {
            if (current == null)
                return null;
            T data = current.data;
            if (current.right == null)
                current = current.bubble();
            else
                current = current.right.tunnel();
            return data;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
