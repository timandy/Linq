package com.bestvike.linq.enumerable;

import com.bestvike.linq.IEnumerable;
import com.bestvike.linq.Linq;
import com.bestvike.linq.entity.IterableDemo;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by 许崇雷 on 2019-05-07.
 */
public class ElementAtOrDefaultTest extends EnumerableTest {
    private static IEnumerable<Object[]> TestData() {
        List<Object[]> lst = new ArrayList<>();

        lst.add(new Object[]{NumberRangeGuaranteedNotCollectionType(9, 1), 0, 9});
        lst.add(new Object[]{NumberRangeGuaranteedNotCollectionType(9, 10), 9, 18});
        lst.add(new Object[]{NumberRangeGuaranteedNotCollectionType(-4, 10), 3, -1});

        lst.add(new Object[]{Linq.asEnumerable(new int[]{1, 2, 3, 4}), 4, null});
        lst.add(new Object[]{Linq.asEnumerable(new int[0]), 0, null});
        lst.add(new Object[]{Linq.asEnumerable(new int[]{-4}), 0, -4});
        lst.add(new Object[]{Linq.asEnumerable(new int[]{9, 8, 0, -5, 10}), 4, 10});

        lst.add(new Object[]{NumberRangeGuaranteedNotCollectionType(-4, 5), -1, null});
        lst.add(new Object[]{NumberRangeGuaranteedNotCollectionType(5, 5), 5, null});
        lst.add(new Object[]{NumberRangeGuaranteedNotCollectionType(0, 0), 0, null});
        return Linq.asEnumerable(lst);
    }

    @Test
    public void SameResultsRepeatCallsIntQuery() {
        IEnumerable<Integer> q = Linq.asEnumerable(new int[]{0, 9999, 0, 888, -1, 66, -1, -777, 1, 2, -12345})
                .where(x -> x > Integer.MIN_VALUE);

        Assert.assertEquals(q.elementAtOrDefault(3), q.elementAtOrDefault(3));
    }

    @Test
    public void SameResultsRepeatCallsStringQuery() {
        IEnumerable<String> q = Linq.asEnumerable(new String[]{"!@#$%^", "C", "AAA", "", "Calling Twice", "SoS", Empty})
                .where(x -> !IsNullOrEmpty(x));

        Assert.assertEquals(q.elementAtOrDefault(4), q.elementAtOrDefault(4));
    }

    @Test
    public void ElementAtOrDefault() {
        for (Object[] objects : TestData()) {
            this.ElementAtOrDefault((IEnumerable<Integer>) objects[0], (int) objects[1], (Integer) objects[2]);
        }
    }

    private void ElementAtOrDefault(IEnumerable<Integer> source, int index, Integer expected) {
        Assert.assertEquals(expected, source.elementAtOrDefault(index));
    }

    @Test
    public void ElementAtOrDefaultRunOnce() {
        for (Object[] objects : TestData()) {
            this.ElementAtOrDefaultRunOnce((IEnumerable<Integer>) objects[0], (Integer) objects[1], (Integer) objects[2]);
        }
    }

    private void ElementAtOrDefaultRunOnce(IEnumerable<Integer> source, int index, Integer expected) {
        Assert.assertEquals(expected, source.runOnce().elementAtOrDefault(index));
    }

    @Test
    public void NullableArray_NegativeIndex_ReturnsNull() {
        Integer[] source = {9, 8};
        Assert.assertNull(Linq.asEnumerable(source).elementAtOrDefault(-1));
    }

    @Test
    public void NullableArray_ValidIndex_ReturnsCorrectObject() {
        Integer[] source = {9, 8, null, -5, 10};

        Assert.assertNull(Linq.asEnumerable(source).elementAtOrDefault(2));
        Assert.assertEquals(-5, (int) Linq.asEnumerable(source).elementAtOrDefault(3));
    }

    @Test
    public void NullSource_ThrowsArgumentNullException() {
        assertThrows(NullPointerException.class, () -> ((IEnumerable<Integer>) null).elementAtOrDefault(2));
    }

    @Test
    public void testElementAtOrDefault() {
        IEnumerable<String> enumerable = Linq.asEnumerable(Arrays.asList("jimi", "mitch"));
        Assert.assertEquals("jimi", enumerable.elementAtOrDefault(0));
        Assert.assertNull(enumerable.elementAtOrDefault(2));
        Assert.assertNull(enumerable.elementAtOrDefault(-1));

        IEnumerable<Long> enumerable2 = Linq.asEnumerable(new IterableDemo(2));
        Assert.assertEquals((Long) 1L, enumerable2.elementAtOrDefault(0));
        Assert.assertNull(enumerable2.elementAtOrDefault(2));
        Assert.assertNull(enumerable2.elementAtOrDefault(-1));
    }
}
