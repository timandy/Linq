package com.bestvike.linq.enumerable;

import com.bestvike.collections.generic.IArray;
import com.bestvike.collections.generic.IList;
import com.bestvike.function.Func1;
import com.bestvike.function.IndexFunc2;
import com.bestvike.linq.IEnumerable;
import com.bestvike.linq.IEnumerator;
import com.bestvike.linq.exception.ExceptionArgument;
import com.bestvike.linq.exception.ThrowHelper;
import com.bestvike.linq.util.ArrayUtils;
import com.bestvike.linq.util.ListUtils;
import com.bestvike.linq.util.Utilities;
import com.bestvike.out;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 许崇雷 on 2018-05-03.
 */
public final class Select {
    private Select() {
    }

    public static <TSource, TResult> IEnumerable<TResult> select(IEnumerable<TSource> source, Func1<TSource, TResult> selector) {
        if (source == null)
            ThrowHelper.throwArgumentNullException(ExceptionArgument.source);
        if (selector == null)
            ThrowHelper.throwArgumentNullException(ExceptionArgument.selector);

        if (source instanceof Iterator) {
            Iterator<TSource> iterator = (Iterator<TSource>) source;
            return iterator._select(selector);
        }

        if (source instanceof IList) {
            if (source instanceof IArray) {
                IArray<TSource> array = (IArray<TSource>) source;
                return array._getCount() == 0
                        ? EmptyPartition.instance()
                        : new SelectArrayIterator<>(array, selector);
            }
            IList<TSource> list = (IList<TSource>) source;
            return list._getCount() == 0
                    ? EmptyPartition.instance()
                    : new SelectIListIterator<>(list, selector);
        }

        if (source instanceof IPartition) {
            IPartition<TSource> partition = (IPartition<TSource>) source;
            return new SelectIPartitionIterator<>(partition, selector);
        }

        return new SelectEnumerableIterator<>(source, selector);
    }

    public static <TSource, TResult> IEnumerable<TResult> select(IEnumerable<TSource> source, IndexFunc2<TSource, TResult> selector) {
        if (source == null)
            ThrowHelper.throwArgumentNullException(ExceptionArgument.source);

        if (selector == null)
            ThrowHelper.throwArgumentNullException(ExceptionArgument.selector);

        return new SelectIterator<>(source, selector);
    }
}


final class SelectIterator<TSource, TResult> extends AbstractIterator<TResult> {
    private final IEnumerable<TSource> source;
    private final IndexFunc2<TSource, TResult> selector;
    private IEnumerator<TSource> enumerator;
    private int index;

    SelectIterator(IEnumerable<TSource> source, IndexFunc2<TSource, TResult> selector) {
        this.source = source;
        this.selector = selector;
    }

    @Override
    public AbstractIterator<TResult> clone() {
        return new SelectIterator<>(this.source, this.selector);
    }

    @Override
    public boolean moveNext() {
        switch (this.state) {
            case 1:
                this.index = -1;
                this.enumerator = this.source.enumerator();
                this.state = 2;
            case 2:
                if (this.enumerator.moveNext()) {
                    TSource item = this.enumerator.current();
                    this.index = Math.addExact(this.index, 1);
                    this.current = this.selector.apply(item, this.index);
                    return true;
                }
                this.close();
                return false;
            default:
                return false;
        }
    }

    @Override
    public void close() {
        if (this.enumerator != null) {
            this.enumerator.close();
            this.enumerator = null;
        }
        super.close();
    }
}


final class SelectEnumerableIterator<TSource, TResult> extends Iterator<TResult> implements IIListProvider<TResult> {
    private final IEnumerable<TSource> source;
    private final Func1<TSource, TResult> selector;
    private IEnumerator<TSource> enumerator;

    SelectEnumerableIterator(IEnumerable<TSource> source, Func1<TSource, TResult> selector) {
        assert source != null;
        assert selector != null;
        this.source = source;
        this.selector = selector;
    }

    @Override
    public Iterator<TResult> clone() {
        return new SelectEnumerableIterator<>(this.source, this.selector);
    }

    @Override
    public boolean moveNext() {
        switch (this.state) {
            case 1:
                this.enumerator = this.source.enumerator();
                this.state = 2;
            case 2:
                if (this.enumerator.moveNext()) {
                    this.current = this.selector.apply(this.enumerator.current());
                    return true;
                }
                this.close();
                return false;
            default:
                return false;
        }
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
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectEnumerableIterator<>(this.source, Utilities.combineSelectors(this.selector, selector));
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        LargeArrayBuilder<TResult> builder = new LargeArrayBuilder<>();
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext())
                builder.add(this.selector.apply(e.current()));
        }

        return builder.toArray(clazz);
    }

    @Override
    public Object[] _toArray() {
        LargeArrayBuilder<TResult> builder = new LargeArrayBuilder<>();
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext())
                builder.add(this.selector.apply(e.current()));
        }

        return builder.toArray();
    }

    @Override
    public List<TResult> _toList() {
        List<TResult> list = new ArrayList<>();
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext())
                list.add(this.selector.apply(e.current()));
        }

        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of
        // the selector, run it provided `onlyIfCheap` is false.
        if (onlyIfCheap)
            return -1;

        int count = 0;
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext()) {
                this.selector.apply(e.current());
                count = Math.addExact(count, 1);
            }
        }

        return count;
    }
}


final class SelectArrayIterator<TSource, TResult> extends Iterator<TResult> implements IPartition<TResult> {
    private final IArray<TSource> source;
    private final Func1<TSource, TResult> selector;

    SelectArrayIterator(IArray<TSource> source, Func1<TSource, TResult> selector) {
        assert source != null;
        assert selector != null;
        assert source._getCount() > 0; // Caller should check this beforehand and return a cached result
        this.source = source;
        this.selector = selector;
    }

    @Override
    public Iterator<TResult> clone() {
        return new SelectArrayIterator<>(this.source, this.selector);
    }

    @Override
    public boolean moveNext() {
        if (this.state < 1 | this.state == this.source._getCount() + 1) {
            this.close();
            return false;
        }

        int index = this.state++ - 1;
        this.current = this.selector.apply(this.source.get(index));
        return true;
    }

    @Override
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectArrayIterator<>(this.source, Utilities.combineSelectors(this.selector, selector));
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        // See assert : constructor.
        // Since source should never be empty, we don't check for 0/return Array.Empty.
        assert this.source._getCount() > 0;

        TResult[] results = ArrayUtils.newInstance(clazz, this.source._getCount());
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(this.source.get(i));

        return results;
    }

    @Override
    public Object[] _toArray() {
        // See assert : constructor.
        // Since source should never be empty, we don't check for 0/return Array.Empty.
        assert this.source._getCount() > 0;

        Object[] results = new Object[this.source._getCount()];
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(this.source.get(i));
        return results;
    }

    @Override
    public List<TResult> _toList() {
        int count = this.source._getCount();
        List<TResult> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            results.add(this.selector.apply(this.source.get(i)));
        return results;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of
        // the selector, run it provided `onlyIfCheap` is false.
        int count = this.source._getCount();

        if (!onlyIfCheap) {
            for (int i = 0; i < count; i++)
                this.selector.apply(this.source.get(i));
        }

        return count;
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        return count >= this.source._getCount()
                ? EmptyPartition.instance()
                : new SelectListPartitionIterator<>(this.source, this.selector, count, Integer.MAX_VALUE);
    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        return count >= this.source._getCount()
                ? this
                : new SelectListPartitionIterator<>(this.source, this.selector, 0, count - 1);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.source._getCount()) < 0) {
            found.value = true;
            return this.selector.apply(this.source.get(index));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        assert this.source._getCount() > 0; // See assert : constructor

        found.value = true;
        return this.selector.apply(this.source.get(0));
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        assert this.source._getCount() > 0; // See assert : constructor

        found.value = true;
        return this.selector.apply(this.source.get(this.source._getCount() - 1));
    }
}


final class SelectRangeIterator<TResult> extends Iterator<TResult> implements IPartition<TResult> {
    private final int start;
    private final int end;
    private final Func1<Integer, TResult> selector;

    SelectRangeIterator(int start, int end, Func1<Integer, TResult> selector) {
        assert start < end;
        assert Integer.compareUnsigned(end - start, Integer.MAX_VALUE) <= 0;
        assert selector != null;

        this.start = start;
        this.end = end;
        this.selector = selector;
    }


    @Override
    public AbstractIterator<TResult> clone() {
        return new SelectRangeIterator<>(this.start, this.end, this.selector);
    }

    @Override
    public boolean moveNext() {
        if (this.state < 1 || this.state == this.end - this.start + 1) {
            this.close();
            return false;
        }

        int index = this.state++ - 1;
        assert this.start < this.end - index;
        this.current = this.selector.apply(this.start + index);
        return true;
    }

    @Override
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectRangeIterator<>(this.start, this.end, Utilities.combineSelectors(this.selector, selector));
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        TResult[] results = ArrayUtils.newInstance(clazz, this.end - this.start);
        int srcIndex = this.start;
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(srcIndex++);
        return results;
    }

    @Override
    public Object[] _toArray() {
        Object[] results = new Object[this.end - this.start];
        int srcIndex = this.start;
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(srcIndex++);
        return results;
    }

    @Override
    public List<TResult> _toList() {
        List<TResult> list = new ArrayList<>(this.end - this.start);
        for (int i = this.start; i != this.end; i++)
            list.add(this.selector.apply(i));
        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of the selector,
        // run it provided `onlyIfCheap` is false.
        if (!onlyIfCheap) {
            for (int i = this.start; i != this.end; i++)
                this.selector.apply(i);
        }
        return this.end - this.start;
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        return count >= this.end - this.start
                ? EmptyPartition.instance()
                : new SelectRangeIterator<>(this.start + count, this.end, this.selector);
    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        return count >= this.end - this.start
                ? this
                : new SelectRangeIterator<>(this.start, this.start + count, this.selector);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.end - this.start) < 0) {
            found.value = true;
            return this.selector.apply(this.start + index);
        }
        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        assert this.end > this.start;
        found.value = true;
        return this.selector.apply(this.start);
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        assert this.end > this.start;
        found.value = true;
        return this.selector.apply(this.end - 1);
    }
}


final class SelectRepeatIterator<TSource, TResult> extends Iterator<TResult> implements IPartition<TResult> {
    private final TSource element;
    private final int count;
    private final Func1<TSource, TResult> selector;

    SelectRepeatIterator(TSource element, int count, Func1<TSource, TResult> selector) {
        assert count > 0;
        assert selector != null;

        this.element = element;
        this.count = count;
        this.selector = selector;
    }

    @Override
    public AbstractIterator<TResult> clone() {
        return new SelectRepeatIterator<>(this.element, this.count, this.selector);
    }

    @Override
    public boolean moveNext() {
        if (this.state < 1 || this.state == this.count + 1) {
            this.close();
            return false;
        }
        this.state++;
        this.current = this.selector.apply(this.element);
        return true;
    }

    @Override
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectRepeatIterator<>(this.element, this.count, Utilities.combineSelectors(this.selector, selector));
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        TResult[] results = ArrayUtils.newInstance(clazz, this.count);
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(this.element);
        return results;
    }

    @Override
    public Object[] _toArray() {
        Object[] results = new Object[this.count];
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(this.element);
        return results;
    }

    @Override
    public List<TResult> _toList() {
        List<TResult> list = new ArrayList<>(this.count);
        for (int i = 0; i < this.count; i++)
            list.add(this.selector.apply(this.element));
        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of the selector,
        // run it provided `onlyIfCheap` is false.
        if (!onlyIfCheap) {
            for (int i = 0; i < this.count; i++)
                this.selector.apply(this.element);
        }
        return this.count;
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        return count >= this.count
                ? EmptyPartition.instance()
                : new SelectRepeatIterator<>(this.element, this.count - count, this.selector);

    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        return count >= this.count
                ? this
                : new SelectRepeatIterator<>(this.element, count, this.selector);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.count) < 0) {
            found.value = true;
            return this.selector.apply(this.element);
        }
        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        assert this.count > 0;
        found.value = true;
        return this.selector.apply(this.element);
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        assert this.count > 0;
        found.value = true;
        return this.selector.apply(this.element);
    }
}


final class SelectIListIterator<TSource, TResult> extends Iterator<TResult> implements IPartition<TResult> {
    private final IList<TSource> source;
    private final Func1<TSource, TResult> selector;
    private IEnumerator<TSource> enumerator;

    SelectIListIterator(IList<TSource> source, Func1<TSource, TResult> selector) {
        assert source != null;
        assert selector != null;
        this.source = source;
        this.selector = selector;
    }

    @Override
    public Iterator<TResult> clone() {
        return new SelectIListIterator<>(this.source, this.selector);
    }

    @Override
    public boolean moveNext() {
        switch (this.state) {
            case 1:
                this.enumerator = this.source.enumerator();
                this.state = 2;
            case 2:
                if (this.enumerator.moveNext()) {
                    this.current = this.selector.apply(this.enumerator.current());
                    return true;
                }
                this.close();
                return false;
            default:
                return false;
        }
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
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectIListIterator<>(this.source, Utilities.combineSelectors(this.selector, selector));
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        int count = this.source._getCount();
        if (count == 0)
            return ArrayUtils.empty(clazz);

        TResult[] results = ArrayUtils.newInstance(clazz, count);
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(this.source.get(i));

        return results;
    }

    @Override
    public Object[] _toArray() {
        int count = this.source._getCount();
        if (count == 0)
            return ArrayUtils.empty();

        Object[] results = new Object[count];
        for (int i = 0; i < results.length; i++)
            results[i] = this.selector.apply(this.source.get(i));

        return results;
    }

    @Override
    public List<TResult> _toList() {
        int count = this.source._getCount();
        ArrayList<TResult> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            results.add(this.selector.apply(this.source.get(i)));

        return results;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of
        // the selector, run it provided `onlyIfCheap` is false.
        int count = this.source._getCount();

        if (!onlyIfCheap) {
            for (int i = 0; i < count; i++)
                this.selector.apply(this.source.get(i));
        }

        return count;
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        return new SelectListPartitionIterator<>(this.source, this.selector, count, Integer.MAX_VALUE);
    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        return new SelectListPartitionIterator<>(this.source, this.selector, 0, count - 1);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.source._getCount()) < 0) {
            found.value = true;
            return this.selector.apply(this.source.get(index));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        if (this.source._getCount() != 0) {
            found.value = true;
            return this.selector.apply(this.source.get(0));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        int len = this.source._getCount();
        if (len != 0) {
            found.value = true;
            return this.selector.apply(this.source.get(len - 1));
        }

        found.value = false;
        return null;
    }
}


final class SelectIPartitionIterator<TSource, TResult> extends Iterator<TResult> implements IPartition<TResult> {
    private final IPartition<TSource> source;
    private final Func1<TSource, TResult> selector;
    private IEnumerator<TSource> enumerator;

    SelectIPartitionIterator(IPartition<TSource> source, Func1<TSource, TResult> selector) {
        assert source != null;
        assert selector != null;
        this.source = source;
        this.selector = selector;
    }

    @Override
    public Iterator<TResult> clone() {
        return new SelectIPartitionIterator<>(this.source, this.selector);
    }

    @Override
    public boolean moveNext() {
        switch (this.state) {
            case 1:
                this.enumerator = this.source.enumerator();
                this.state = 2;
            case 2:
                if (this.enumerator.moveNext()) {
                    this.current = this.selector.apply(this.enumerator.current());
                    return true;
                }
                this.close();
                return false;
            default:
                return false;
        }
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
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectIPartitionIterator<>(this.source, Utilities.combineSelectors(this.selector, selector));
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        return new SelectIPartitionIterator<>(this.source._skip(count), this.selector);
    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        return new SelectIPartitionIterator<>(this.source._take(count), this.selector);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        TSource input = this.source._tryGetElementAt(index, found);
        return found.value ? this.selector.apply(input) : null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        TSource input = this.source._tryGetFirst(found);
        return found.value ? this.selector.apply(input) : null;
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        TSource input = this.source._tryGetLast(found);
        return found.value ? this.selector.apply(input) : null;
    }

    private TResult[] lazyToArray(Class<TResult> clazz) {
        assert this.source._getCount(true) == -1;

        LargeArrayBuilder<TResult> builder = new LargeArrayBuilder<>();
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext())
                builder.add(this.selector.apply(e.current()));
        }

        return builder.toArray(clazz);
    }

    private Object[] lazyToArray() {
        assert this.source._getCount(true) == -1;

        LargeArrayBuilder<TResult> builder = new LargeArrayBuilder<>();
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext())
                builder.add(this.selector.apply(e.current()));
        }

        return builder.toArray();
    }

    private TResult[] preallocatingToArray(Class<TResult> clazz, int count) {
        assert count > 0;
        assert count == this.source._getCount(true);

        TResult[] array = ArrayUtils.newInstance(clazz, count);
        int index = 0;
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext()) {
                array[index] = this.selector.apply(e.current());
                ++index;
            }
        }

        return array;
    }

    private Object[] preallocatingToArray(int count) {
        assert count > 0;
        assert count == this.source._getCount(true);

        Object[] array = new Object[count];
        int index = 0;
        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext()) {
                array[index] = this.selector.apply(e.current());
                ++index;
            }
        }

        return array;
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        int count = this.source._getCount(true);
        switch (count) {
            case -1:
                return this.lazyToArray(clazz);
            case 0:
                return ArrayUtils.empty(clazz);
            default:
                return this.preallocatingToArray(clazz, count);
        }
    }

    @Override
    public Object[] _toArray() {
        int count = this.source._getCount(true);
        switch (count) {
            case -1:
                return this.lazyToArray();
            case 0:
                return ArrayUtils.empty();
            default:
                return this.preallocatingToArray(count);
        }
    }

    @Override
    public List<TResult> _toList() {
        int count = this.source._getCount(true);
        List<TResult> list;
        switch (count) {
            case -1:
                list = new ArrayList<>();
                break;
            case 0:
                return ListUtils.empty();
            default:
                list = new ArrayList<>(count);
                break;
        }

        try (IEnumerator<TSource> e = this.source.enumerator()) {
            while (e.moveNext())
                list.add(this.selector.apply(e.current()));
        }

        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of
        // the selector, run it provided `onlyIfCheap` is false.
        if (!onlyIfCheap) {
            try (IEnumerator<TSource> e = this.source.enumerator()) {
                while (e.moveNext())
                    this.selector.apply(e.current());
            }
        }

        return this.source._getCount(onlyIfCheap);
    }
}


final class SelectListPartitionIterator<TSource, TResult> extends ReverseIterator<TResult> implements IPartition<TResult> {
    private final IList<TSource> source;
    private final Func1<TSource, TResult> selector;
    private final int minIndexInclusive;
    private final int maxIndexInclusive;

    SelectListPartitionIterator(IList<TSource> source, Func1<TSource, TResult> selector, int minIndexInclusive, int maxIndexInclusive) {
        assert source != null;
        assert selector != null;
        assert minIndexInclusive >= 0;
        assert minIndexInclusive <= maxIndexInclusive;
        this.source = source;
        this.selector = selector;
        this.minIndexInclusive = minIndexInclusive;
        this.maxIndexInclusive = maxIndexInclusive;
    }

    @Override
    public Iterator<TResult> clone() {
        return new SelectListPartitionIterator<>(this.source, this.selector, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IEnumerable<TResult> _reverse() {
        return new SelectReverseListPartitionIterator<>(this.source, this.selector, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public boolean moveNext() {
        // this.state - 1 represents the zero-based index into the list.
        // Having a separate field for the index would be more readable. However, we save it
        // into this.state with a bias to minimize field size of the iterator.
        int index = this.state - 1;
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < this.source._getCount() - this.minIndexInclusive) {
            this.current = this.selector.apply(this.source.get(this.minIndexInclusive + index));
            ++this.state;
            return true;
        }

        this.close();
        return false;
    }

    @Override
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectListPartitionIterator<>(this.source, Utilities.combineSelectors(this.selector, selector), this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        int minIndex = this.minIndexInclusive + count;
        return Integer.compareUnsigned(minIndex, this.maxIndexInclusive) > 0
                ? EmptyPartition.instance()
                : new SelectListPartitionIterator<>(this.source, this.selector, minIndex, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        int maxIndex = this.minIndexInclusive + count - 1;
        return Integer.compareUnsigned(maxIndex, this.maxIndexInclusive) >= 0
                ? this
                : new SelectListPartitionIterator<>(this.source, this.selector, this.minIndexInclusive, maxIndex);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < this.source._getCount() - this.minIndexInclusive) {
            found.value = true;
            return this.selector.apply(this.source.get(this.minIndexInclusive + index));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        if (this.source._getCount() > this.minIndexInclusive) {
            found.value = true;
            return this.selector.apply(this.source.get(this.minIndexInclusive));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        int lastIndex = this.source._getCount() - 1;
        if (lastIndex >= this.minIndexInclusive) {
            found.value = true;
            return this.selector.apply(this.source.get(Math.min(lastIndex, this.maxIndexInclusive)));
        }

        found.value = false;
        return null;
    }

    private int _getCount() {
        int count = this.source._getCount();
        if (count <= this.minIndexInclusive)
            return 0;

        return Math.min(count - 1, this.maxIndexInclusive) - this.minIndexInclusive + 1;
    }

    @Override
    public TResult[] _toArray(Class<TResult> clazz) {
        int count = this._getCount();
        if (count == 0)
            return ArrayUtils.empty(clazz);

        TResult[] array = ArrayUtils.newInstance(clazz, count);
        for (int i = 0, curIdx = this.minIndexInclusive; i != array.length; ++i, ++curIdx)
            array[i] = this.selector.apply(this.source.get(curIdx));

        return array;
    }

    @Override
    public Object[] _toArray() {
        int count = this._getCount();
        if (count == 0)
            return ArrayUtils.empty();

        Object[] array = new Object[count];
        for (int i = 0, curIdx = this.minIndexInclusive; i != array.length; ++i, ++curIdx)
            array[i] = this.selector.apply(this.source.get(curIdx));

        return array;
    }

    @Override
    public List<TResult> _toList() {
        int count = this._getCount();
        if (count == 0)
            return new ArrayList<>();

        List<TResult> list = new ArrayList<>(count);
        int end = this.minIndexInclusive + count;
        for (int i = this.minIndexInclusive; i != end; ++i)
            list.add(this.selector.apply(this.source.get(i)));

        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of
        // the selector, run it provided `onlyIfCheap` is false.
        int count = this._getCount();
        if (!onlyIfCheap) {
            int end = this.minIndexInclusive + count;
            for (int i = this.minIndexInclusive; i != end; ++i)
                this.selector.apply(this.source.get(i));
        }

        return count;
    }
}


final class SelectReverseListPartitionIterator<TSource, TResult> extends ReverseIterator<TResult> implements IPartition<TResult> {
    private final IList<TSource> source;
    private final Func1<TSource, TResult> selector;
    private final int minIndexInclusive;
    private final int maxIndexInclusive;

    SelectReverseListPartitionIterator(IList<TSource> source, Func1<TSource, TResult> selector, int minIndexInclusive, int maxIndexInclusive) {
        assert source != null;
        assert selector != null;
        assert minIndexInclusive >= 0;
        assert minIndexInclusive <= maxIndexInclusive;
        this.source = source;
        this.selector = selector;
        this.minIndexInclusive = minIndexInclusive;
        this.maxIndexInclusive = maxIndexInclusive;
    }

    @Override
    public Iterator<TResult> clone() {
        return new SelectReverseListPartitionIterator<>(this.source, this.selector, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IEnumerable<TResult> _reverse() {
        return new SelectListPartitionIterator<>(this.source, this.selector, this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public boolean moveNext() {
        // _state - 1 represents the zero-based index into the list.
        // Having a separate field for the index would be more readable. However, we save it
        // into _state with a bias to minimize field size of the iterator.
        int index = this.state - 1;
        int count = this.source._getCount();
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < count - this.minIndexInclusive) {
            this.current = this.selector.apply(this.source.get(Math.min(count - 1, this.maxIndexInclusive) - index));
            ++this.state;
            return true;
        }

        this.close();
        return false;
    }

    @Override
    public <TResult2> IEnumerable<TResult2> _select(Func1<TResult, TResult2> selector) {
        return new SelectReverseListPartitionIterator<>(this.source, Utilities.combineSelectors(this.selector, selector), this.minIndexInclusive, this.maxIndexInclusive);
    }

    @Override
    public IPartition<TResult> _skip(int count) {
        assert count > 0;
        int maxIndex = Math.min(this.source._getCount() - 1, this.maxIndexInclusive) - count;
        return maxIndex < this.minIndexInclusive ? EmptyPartition.instance() : new SelectReverseListPartitionIterator<>(this.source, this.selector, this.minIndexInclusive, maxIndex);
    }

    @Override
    public IPartition<TResult> _take(int count) {
        assert count > 0;
        int minIndex = Math.min(this.source._getCount() - 1, this.maxIndexInclusive) - count + 1;
        return minIndex <= this.minIndexInclusive ? this : new SelectReverseListPartitionIterator<>(this.source, this.selector, minIndex, this.maxIndexInclusive);
    }

    @Override
    public TResult _tryGetElementAt(int index, out<Boolean> found) {
        int count = this.source._getCount();
        if (Integer.compareUnsigned(index, this.maxIndexInclusive - this.minIndexInclusive) <= 0 && index < count - this.minIndexInclusive) {
            found.value = true;
            return this.selector.apply(this.source.get(Math.min(count - 1, this.maxIndexInclusive) - index));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetFirst(out<Boolean> found) {
        int lastIndex = this.source._getCount() - 1;
        if (lastIndex >= this.minIndexInclusive) {
            found.value = true;
            return this.selector.apply(this.source.get(Math.min(lastIndex, this.maxIndexInclusive)));
        }

        found.value = false;
        return null;
    }

    @Override
    public TResult _tryGetLast(out<Boolean> found) {
        if (this.source._getCount() > this.minIndexInclusive) {
            found.value = true;
            return this.selector.apply(this.source.get(this.minIndexInclusive));
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
    public TResult[] _toArray(Class<TResult> clazz) {
        int count = this.getCount();
        if (count == 0)
            return ArrayUtils.empty(clazz);

        TResult[] array = ArrayUtils.newInstance(clazz, count);
        for (int i = 0, curIdx = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != array.length; ++i, --curIdx)
            array[i] = this.selector.apply(this.source.get(curIdx));

        return array;
    }

    @Override
    public Object[] _toArray() {
        int count = this.getCount();
        if (count == 0)
            return ArrayUtils.empty();

        Object[] array = new Object[count];
        for (int i = 0, curIdx = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != array.length; ++i, --curIdx)
            array[i] = this.selector.apply(this.source.get(curIdx));

        return array;
    }

    @Override
    public List<TResult> _toList() {
        int count = this.getCount();
        if (count == 0)
            return ListUtils.empty();

        List<TResult> list = new ArrayList<>(count);
        int end = this.minIndexInclusive - 1;
        for (int i = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != end; --i)
            list.add(this.selector.apply(this.source.get(i)));

        return list;
    }

    @Override
    public int _getCount(boolean onlyIfCheap) {
        // In case someone uses Count() to force evaluation of
        // the selector, run it provided `onlyIfCheap` is false.
        int count = this.getCount();

        if (!onlyIfCheap) {
            int end = this.minIndexInclusive - 1;
            for (int i = Math.min(this.source._getCount() - 1, this.maxIndexInclusive); i != end; --i)
                this.selector.apply(this.source.get(i));
        }

        return count;
    }
}
