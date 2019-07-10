package com.bestvike.linq.enumerable;

import com.bestvike.collections.generic.IList;
import com.bestvike.function.Func1;
import com.bestvike.function.Predicate1;
import com.bestvike.linq.IEnumerable;
import com.bestvike.linq.IEnumerator;
import com.bestvike.linq.util.ArrayUtils;
import com.bestvike.linq.util.ListUtils;
import com.bestvike.out;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 许崇雷 on 2018-05-07.
 */
interface IIListProvider<TElement> extends IEnumerable<TElement> {
    TElement[] _toArray(Class<TElement> clazz);

    Object[] _toArray();

    List<TElement> _toList();

    int _getCount(boolean onlyIfCheap);
}


interface IPartition<TElement> extends IIListProvider<TElement> {
    IPartition<TElement> _skip(int count);

    IPartition<TElement> _take(int count);

    TElement _tryGetElementAt(int index, out<Boolean> found);

    TElement _tryGetFirst(out<Boolean> found);

    TElement _tryGetLast(out<Boolean> found);
}


final class EmptyPartition<TElement> extends Iterator<TElement> implements IPartition<TElement> {
    private static final IPartition INSTANCE = new EmptyPartition();

    private EmptyPartition() {
    }

    public static <TElement> IPartition<TElement> instance() {
        //noinspection unchecked
        return INSTANCE;
    }

    @Override
    public AbstractIterator<TElement> clone() {
        return this;
    }

    @Override
    public boolean moveNext() {
        return false;
    }

    @Override
    public <TResult> IEnumerable<TResult> _select(Func1<TElement, TResult> selector) {
        //noinspection unchecked
        return (IEnumerable<TResult>) this;
    }

    @Override
    public IEnumerable<TElement> _where(Predicate1<TElement> predicate) {
        return this;
    }

    @Override
    public IPartition<TElement> _skip(int count) {
        return this;
    }

    @Override
    public IPartition<TElement> _take(int count) {
        return this;
    }

    @Override
    public TElement _tryGetElementAt(int index, out<Boolean> found) {
        found.value = false;
        return null;
    }

    @Override
    public TElement _tryGetFirst(out<Boolean> found) {
        found.value = false;
        return null;
    }

    @Override
    public TElement _tryGetLast(out<Boolean> found) {
        found.value = false;
        return null;
    }

    @Override
    public TElement[] _toArray(Class<TElement> clazz) {
        return ArrayUtils.empty(clazz);
    }

    @Override
    public Object[] _toArray() {
        return ArrayUtils.empty();
    }

    @Override
    public List<TElement> _toList() {
        return ListUtils.empty();
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        return 0;
    }
}


final class OrderedPartition<TElement> implements IPartition<TElement> {
    private final AbstractOrderedEnumerable<TElement> source;
    private final int minIndexInclusive;
    private final int maxIndexInclusive;

    OrderedPartition(AbstractOrderedEnumerable<TElement> source, int minIdxInclusive, int maxIdxInclusive) {
        this.source = source;
        this.minIndexInclusive = minIdxInclusive;
        this.maxIndexInclusive = maxIdxInclusive;
    }

    @Override
    public IEnumerator<TElement> enumerator() {
        return this.source.enumerator(this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TElement> _skip(int count) {
        int minIndex = this.minIndexInclusive + count;
        return Integer.compareUnsigned(minIndex, this.maxIndexInclusive) > 0 ? EmptyPartition.instance() : new OrderedPartition<>(this.source, minIndex, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TElement> _take(int count) {
        int maxIndex = this.minIndexInclusive + count - 1;
        if (Integer.compareUnsigned(maxIndex, this.maxIndexInclusive) >= 0)
            return this;
        return new OrderedPartition<>(this.source, this.minIndexInclusive, maxIndex);
    }

    @Override
    public TElement _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0)
            return this.source._tryGetElementAt(index + this.minIndexInclusive, found);

        found.value = false;
        return null;
    }

    @Override
    public TElement _tryGetFirst(out<Boolean> found) {
        return this.source._tryGetElementAt(this.minIndexInclusive, found);
    }

    @Override
    public TElement _tryGetLast(out<Boolean> found) {
        return this.source._tryGetLast(this.minIndexInclusive, this.maxIndexInclusive, found);
    }

    @Override
    public TElement[] _toArray(Class<TElement> clazz) {
        return this.source._toArray(clazz, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public Object[] _toArray() {
        return this.source._toArray(this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public List<TElement> _toList() {
        return this.source._toList(this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        return this.source._getCount(this.minIndexInclusive, this.maxIndexInclusive, onlyIfCheap);
    }
}


final class ListPartition<TSource> extends ReverseIterator<TSource> implements IPartition<TSource> {
    private final IList<TSource> source;
    private final int minIndexInclusive;
    private final int maxIndexInclusive;

    ListPartition(IList<TSource> source, int minIndexInclusive, int maxIndexInclusive) {
        assert source != null;
        assert minIndexInclusive >= 0;
        assert minIndexInclusive <= maxIndexInclusive;

        this.source = source;
        this.minIndexInclusive = minIndexInclusive;
        this.maxIndexInclusive = maxIndexInclusive;
    }

    @Override
    public Iterator<TSource> clone() {
        return new ListPartition<>(this.source, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IEnumerable<TSource> _reverse() {
        return new ReverseListPartition<>(this.source, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public boolean moveNext() {
        // _state - 1 represents the zero-based index into the list.
        // Having a separate field for the index would be more readable. However, we save it
        // into _state with a bias to minimize field size of the iterator.
        if (this.state == -1)
            return false;
        int index = this.state - 1;
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < this.source._getCount() - this.minIndexInclusive) {
            this.current = this.source.get(this.minIndexInclusive + index);
            ++this.state;
            return true;
        }

        this.close();
        return false;
    }

    @Override
    public <TResult> IEnumerable<TResult> _select(Func1<TSource, TResult> selector) {
        return new SelectListPartitionIterator<>(this.source, selector, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TSource> _skip(int count) {
        int minIndex = this.minIndexInclusive + count;
        return Integer.compareUnsigned(minIndex, this.maxIndexInclusive) > 0 ? EmptyPartition.instance() : new ListPartition<>(this.source, minIndex, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TSource> _take(int count) {
        int maxIndex = this.minIndexInclusive + count - 1;
        return Integer.compareUnsigned(maxIndex, this.maxIndexInclusive) >= 0 ? this : new ListPartition<>(this.source, this.minIndexInclusive, maxIndex);
    }

    @Override
    public TSource _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < this.source._getCount() - this.minIndexInclusive) {
            found.value = true;
            return this.source.get(this.minIndexInclusive + index);
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource _tryGetFirst(out<Boolean> found) {
        if (this.source._getCount() > this.minIndexInclusive) {
            found.value = true;
            return this.source.get(this.minIndexInclusive);
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource _tryGetLast(out<Boolean> found) {
        int lastIndex = this.source._getCount() - 1;
        if (lastIndex >= this.minIndexInclusive) {
            found.value = true;
            return this.source.get(Math.min(lastIndex, this.maxIndexInclusive));
        }

        found.value = false;
        return null;
    }

    private int getCount() {
        int count = this.source._getCount();
        if (count <= this.minIndexInclusive)
            return 0;

        return Math.min(count - 1, this.maxIndexInclusive) - this.minIndexInclusive + 1;
    }

    @Override
    public TSource[] _toArray(Class<TSource> clazz) {
        int count = this.getCount();
        if (count == 0)
            return ArrayUtils.empty(clazz);

        TSource[] array = ArrayUtils.newInstance(clazz, count);
        for (int i = 0, curIdx = this.minIndexInclusive; i != array.length; ++i, ++curIdx)
            array[i] = this.source.get(curIdx);
        return array;
    }

    @Override
    public Object[] _toArray() {
        int count = this.getCount();
        if (count == 0)
            return ArrayUtils.empty();

        Object[] array = new Object[count];
        for (int i = 0, curIdx = this.minIndexInclusive; i != array.length; ++i, ++curIdx)
            array[i] = this.source.get(curIdx);
        return array;
    }

    @Override
    public List<TSource> _toList() {
        int count = this.getCount();
        if (count == 0)
            return ListUtils.empty();

        List<TSource> list = new ArrayList<>(count);
        int end = this.minIndexInclusive + count;
        for (int i = this.minIndexInclusive; i != end; ++i)
            list.add(this.source.get(i));
        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        return this.getCount();
    }
}


final class ReverseListPartition<TSource> extends ReverseIterator<TSource> implements IPartition<TSource> {
    private final IList<TSource> source;
    private final int minIndexInclusive;
    private final int maxIndexInclusive;

    ReverseListPartition(IList<TSource> source, int minIndexInclusive, int maxIndexInclusive) {
        assert source != null;
        assert minIndexInclusive >= 0;
        assert minIndexInclusive <= maxIndexInclusive;
        this.source = source;
        this.minIndexInclusive = minIndexInclusive;
        this.maxIndexInclusive = maxIndexInclusive;
    }

    @Override
    public Iterator<TSource> clone() {
        return new ReverseListPartition<>(this.source, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IEnumerable<TSource> _reverse() {
        return new ListPartition<>(this.source, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public boolean moveNext() {
        // _state - 1 represents the zero-based index into the list.
        // Having a separate field for the index would be more readable. However, we save it
        // into _state with a bias to minimize field size of the iterator.
        int index = this.state - 1;
        int count = this.source._getCount();
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < count - this.minIndexInclusive) {
            this.current = this.source.get(Math.min(count - 1, this.maxIndexInclusive) - index);
            ++this.state;
            return true;
        }

        this.close();
        return false;
    }

    @Override
    public <TResult> IEnumerable<TResult> _select(Func1<TSource, TResult> selector) {
        return new SelectReverseListPartitionIterator<>(this.source, selector, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TSource> _skip(int count) {
        assert count > 0;
        int maxIndex = Math.min(this.source._getCount() - 1, this.maxIndexInclusive) - count;
        return maxIndex < this.minIndexInclusive ? EmptyPartition.instance() : new ReverseListPartition<>(this.source, this.minIndexInclusive, maxIndex);
    }

    @Override
    public IPartition<TSource> _take(int count) {
        assert count > 0;
        int minIndex = Math.min(this.source._getCount() - 1, this.maxIndexInclusive) - count + 1;
        return minIndex <= this.minIndexInclusive ? this : new ReverseListPartition<>(this.source, minIndex, this.maxIndexInclusive);
    }

    @Override
    public TSource _tryGetElementAt(int index, out<Boolean> found) {
        int count = this.source._getCount();
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < count - this.minIndexInclusive) {
            found.value = true;
            return this.source.get(Math.min(count - 1, this.maxIndexInclusive) - index);
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource _tryGetFirst(out<Boolean> found) {
        int lastIndex = this.source._getCount() - 1;
        if (lastIndex >= this.minIndexInclusive) {
            found.value = true;
            return this.source.get(Math.min(lastIndex, this.maxIndexInclusive));
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource _tryGetLast(out<Boolean> found) {
        if (this.source._getCount() > this.minIndexInclusive) {
            found.value = true;
            return this.source.get(this.minIndexInclusive);
        }

        found.value = false;
        return null;
    }

    private int getCount() {
        int count = this.source._getCount();
        if (count <= this.minIndexInclusive)
            return 0;

        return Math.min(count - 1, this.maxIndexInclusive) - this.minIndexInclusive + 1;
    }

    @Override
    public TSource[] _toArray(Class<TSource> clazz) {
        int count = this.getCount();
        if (count == 0)
            return ArrayUtils.empty(clazz);

        TSource[] array = ArrayUtils.newInstance(clazz, count);
        for (int i = 0, curIdx = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != array.length; ++i, --curIdx)
            array[i] = this.source.get(curIdx);

        return array;
    }

    @Override
    public Object[] _toArray() {
        int count = this.getCount();
        if (count == 0)
            return ArrayUtils.empty();

        Object[] array = new Object[count];
        for (int i = 0, curIdx = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != array.length; ++i, --curIdx)
            array[i] = this.source.get(curIdx);

        return array;
    }

    @Override
    public List<TSource> _toList() {
        int count = this.getCount();
        if (count == 0)
            return ListUtils.empty();

        List<TSource> list = new ArrayList<>(count);
        int end = this.minIndexInclusive - 1;
        for (int i = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != end; --i)
            list.add(this.source.get(i));

        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        return this.getCount();
    }
}


final class EnumerablePartition<TSource> extends Iterator<TSource> implements IPartition<TSource> {
    private final IEnumerable<TSource> source;
    private final int minIndexInclusive;// -1 if we want everything past _minIndexInclusive.
    private final int maxIndexInclusive;// If this is -1, it's impossible to set a limit on the count.
    private IEnumerator<TSource> enumerator;

    EnumerablePartition(IEnumerable<TSource> source, int minIndexInclusive, int maxIndexInclusive) {
        assert source != null;
        assert !(source instanceof IList);//, $"The caller needs to check for {nameof(IList<TSource>)}.");
        assert minIndexInclusive >= 0;
        assert maxIndexInclusive >= -1;
        // Note that although maxIndexInclusive can't grow, it can still be int.MaxValue.
        // We support partitioning enumerables with > 2B elements. For example, e.Skip(1).Take(int.MaxValue) should work.
        // But if it is int.MaxValue, then minIndexInclusive must != 0. Otherwise, our count may overflow.
        assert maxIndexInclusive == -1 || maxIndexInclusive - minIndexInclusive < Integer.MAX_VALUE;//, $"{nameof(Limit)} will overflow!");
        assert maxIndexInclusive == -1 || minIndexInclusive <= maxIndexInclusive;

        this.source = source;
        this.minIndexInclusive = minIndexInclusive;
        this.maxIndexInclusive = maxIndexInclusive;
    }

    private static <TSource> boolean skipBefore(int index, IEnumerator<TSource> en) {
        return skipAndCount(index, en) == index;
    }

    private static <TSource> int skipAndCount(int index, IEnumerator<TSource> en) {
        assert index >= 0;
        return (int) skipAndCount((long) index, en);
    }

    private static <TSource> long skipAndCount(long index, IEnumerator<TSource> en) {
        assert en != null;

        for (long i = 0; i < index; i++) {
            if (!en.moveNext())
                return i;
        }
        return index;
    }

    // If this is true (e.g. at least one Take call was made), then we have an upper bound
    // on how many elements we can have.
    private boolean hasLimit() {
        return this.maxIndexInclusive != -1;
    }

    private int getLimit() {
        return this.maxIndexInclusive + 1 - this.minIndexInclusive; // This is that upper bound.
    }

    @Override
    public Iterator<TSource> clone() {
        return new EnumerablePartition<>(this.source, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public void close() {
        if (this.enumerator != null) {
            this.enumerator.close();
            this.enumerator = null;
        }
        super.close();
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        if (onlyIfCheap)
            return -1;

        if (!this.hasLimit()) {
            // If HasLimit is false, we contain everything past _minIndexInclusive.
            // Therefore, we have to iterate the whole enumerable.
            return Math.max(this.source.count() - this.minIndexInclusive, 0);
        }

        try (IEnumerator<TSource> en = this.source.enumerator()) {
            // We only want to iterate up to _maxIndexInclusive + 1.
            // Past that, we know the enumerable will be able to fit this partition,
            // so the count will just be _maxIndexInclusive + 1 - _minIndexInclusive.

            // Note that it is possible for _maxIndexInclusive to be int.MaxValue here,
            // so + 1 may result in signed integer overflow. We need to handle this.
            // At the same time, however, we are guaranteed that our max count can fit
            // in an int because if that is true, then _minIndexInclusive must > 0.
            long count = skipAndCount(this.maxIndexInclusive + 1L, en);
            assert count != Integer.MAX_VALUE + 1L || this.minIndexInclusive > 0;// "Our return value will be incorrect.");
            return Math.max((int) count - this.minIndexInclusive, 0);
        }
    }

    @Override
    public boolean moveNext() {
        // Cases where GetEnumerator has not been called or Dispose has already
        // been called need to be handled explicitly, due to the default: clause.
        int taken = this.state - 3;
        if (taken < -2) {
            this.close();
            return false;
        }

        switch (this.state) {
            case 1:
                this.enumerator = this.source.enumerator();
                this.state = 2;
            case 2:
                if (!this.skipBeforeFirst(this.enumerator)) {
                    // Reached the end before we finished skipping.
                    break;
                }
                this.state = 3;
            default:
                if ((!this.hasLimit() || taken < this.getLimit()) && this.enumerator.moveNext()) {
                    if (this.hasLimit()) {
                        // If we are taking an unknown number of elements, it's important not to increment _state.
                        // _state - 3 may eventually end up overflowing & we'll hit the Dispose branch even though
                        // we haven't finished enumerating.
                        this.state++;
                    }
                    this.current = this.enumerator.current();
                    return true;
                }
                break;
        }

        this.close();
        return false;
    }

    @Override
    public <TResult> IEnumerable<TResult> _select(Func1<TSource, TResult> selector) {
        return new SelectIPartitionIterator<>(this, selector);
    }

    @Override
    public IPartition<TSource> _skip(int count) {
        int minIndex = this.minIndexInclusive + count;

        if (!this.hasLimit()) {
            if (minIndex < 0) {
                // If we don't know our max count and minIndex can no longer fit in a positive int,
                // then we will need to wrap ourselves in another iterator.
                // This can happen, for example, during e.Skip(int.MaxValue).Skip(int.MaxValue).
                return new EnumerablePartition<>(this, count, -1);
            }
        } else if (Integer.compareUnsigned(minIndex, this.maxIndexInclusive) > 0) {
            // If minIndex overflows and we have an upper bound, we will go down this branch.
            // We know our upper bound must be smaller than minIndex, since our upper bound fits in an int.
            // This branch should not be taken if we don't have a bound.
            return EmptyPartition.instance();
        }

        assert minIndex >= 0;//$ "We should have taken care of all cases when {nameof(minIndex)} overflows.");
        return new EnumerablePartition<>(this.source, minIndex, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TSource> _take(int count) {
        int maxIndex = this.minIndexInclusive + count - 1;
        if (!this.hasLimit()) {
            if (maxIndex < 0) {
                // If we don't know our max count and maxIndex can no longer fit in a positive int,
                // then we will need to wrap ourselves in another iterator.
                // Note that although maxIndex may be too large, the difference between it and
                // _minIndexInclusive (which is count - 1) must fit in an int.
                // Example: e.Skip(50).Take(int.MaxValue).

                return new EnumerablePartition<>(this, 0, count - 1);
            }
        } else if (Integer.compareUnsigned(maxIndex, this.maxIndexInclusive) >= 0) {
            // If we don't know our max count, we can't go down this branch.
            // It's always possible for us to contain more than count items, as the rest
            // of the enumerable past _minIndexInclusive can be arbitrarily long.
            return this;
        }

        assert maxIndex >= 0;//$ "We should have taken care of all cases when {nameof(maxIndex)} overflows.");
        return new EnumerablePartition<>(this.source, this.minIndexInclusive, maxIndex);
    }

    @Override
    public TSource _tryGetElementAt(int index, out<Boolean> found) {
        // If the index is negative or >= our max count, return early.
        if (index >= 0 && (!this.hasLimit() || index < this.getLimit())) {
            try (IEnumerator<TSource> en = this.source.enumerator()) {
                assert this.minIndexInclusive + index >= 0;//$ "Adding {nameof(index)} caused {nameof(_minIndexInclusive)} to overflow.");

                if (skipBefore(this.minIndexInclusive + index, en) && en.moveNext()) {
                    found.value = true;
                    return en.current();
                }
            }
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource _tryGetFirst(out<Boolean> found) {
        try (IEnumerator<TSource> en = this.source.enumerator()) {
            if (this.skipBeforeFirst(en) && en.moveNext()) {
                found.value = true;
                return en.current();
            }
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource _tryGetLast(out<Boolean> found) {
        try (IEnumerator<TSource> en = this.source.enumerator()) {
            if (this.skipBeforeFirst(en) && en.moveNext()) {
                int remaining = this.getLimit() - 1; // Max number of items left, not counting the current element.
                int comparand = this.hasLimit() ? 0 : Integer.MIN_VALUE; // If we don't have an upper bound, have the comparison always return true.
                TSource result;

                do {
                    remaining--;
                    result = en.current();
                } while (remaining >= comparand && en.moveNext());

                found.value = true;
                return result;
            }
        }

        found.value = false;
        return null;
    }

    @Override
    public TSource[] _toArray(Class<TSource> clazz) {
        try (IEnumerator<TSource> en = this.source.enumerator()) {
            if (this.skipBeforeFirst(en) && en.moveNext()) {
                int remaining = this.getLimit() - 1; // Max number of items left, not counting the current element.
                int comparand = this.hasLimit() ? 0 : Integer.MIN_VALUE; // If we don't have an upper bound, have the comparison always return true.

                int maxCapacity = this.hasLimit() ? this.getLimit() : Integer.MAX_VALUE;
                LargeArrayBuilder<TSource> builder = new LargeArrayBuilder<>(maxCapacity);

                do {
                    remaining--;
                    builder.add(en.current());
                } while (remaining >= comparand && en.moveNext());

                return builder.toArray(clazz);
            }
        }

        return ArrayUtils.empty(clazz);
    }

    @Override
    public Object[] _toArray() {
        try (IEnumerator<TSource> en = this.source.enumerator()) {
            if (this.skipBeforeFirst(en) && en.moveNext()) {
                int remaining = this.getLimit() - 1; // Max number of items left, not counting the current element.
                int comparand = this.hasLimit() ? 0 : Integer.MIN_VALUE; // If we don't have an upper bound, have the comparison always return true.

                int maxCapacity = this.hasLimit() ? this.getLimit() : Integer.MAX_VALUE;
                LargeArrayBuilder<TSource> builder = new LargeArrayBuilder<>(maxCapacity);

                do {
                    remaining--;
                    builder.add(en.current());
                } while (remaining >= comparand && en.moveNext());

                return builder.toArray();
            }
        }

        return ArrayUtils.empty();
    }

    @Override
    public List<TSource> _toList() {
        List<TSource> list = new ArrayList<>();
        try (IEnumerator<TSource> en = this.source.enumerator()) {
            if (this.skipBeforeFirst(en) && en.moveNext()) {
                int remaining = this.getLimit() - 1; // Max number of items left, not counting the current element.
                int comparand = this.hasLimit() ? 0 : Integer.MIN_VALUE; // If we don't have an upper bound, have the comparison always return true.

                do {
                    remaining--;
                    list.add(en.current());
                } while (remaining >= comparand && en.moveNext());
            }
        }
        return list;
    }

    private boolean skipBeforeFirst(IEnumerator<TSource> en) {
        return skipBefore(this.minIndexInclusive, en);
    }
}
