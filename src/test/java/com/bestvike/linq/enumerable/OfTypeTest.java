package com.bestvike.linq.enumerable;

import com.bestvike.linq.IEnumerable;
import com.bestvike.linq.IEnumerator;
import com.bestvike.linq.Linq;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Created by 许崇雷 on 2018-05-26.
 */
public class OfTypeTest extends EnumerableTest {
    @Test
    public void SameResultsRepeatCallsIntQuery() {
        IEnumerable<Integer> q = Linq.asEnumerable(new int[]{9999, 0, 888, -1, 66, -777, 1, 2, -12345}).where(x -> x > Integer.MIN_VALUE);

        assertEquals(q.ofType(Integer.class), q.ofType(Integer.class));
    }

    @Test
    public void SameResultsRepeatCallsStringQuery() {
        IEnumerable<String> q = Linq.asEnumerable("!@#$%^", "C", "AAA", "", "Calling Twice", "SoS", Empty)
                .where(EnumerableTest::IsNullOrEmpty);

        assertEquals(q.ofType(Integer.class), q.ofType(Integer.class));
    }

    @Test
    public void EmptySource() {
        IEnumerable<Object> source = Linq.empty();
        Assert.assertEquals(0, source.ofType(Integer.class).count());
    }

    @Test
    public void LongSequenceFromIntSource() {
        IEnumerable<Integer> source = Linq.asEnumerable(new int[]{99, 45, 81});
        Assert.assertEquals(0, source.ofType(Long.class).count());
    }

    @Test
    public void HeterogenousSourceNoAppropriateElements() {
        IEnumerable<Object> source = Linq.asEnumerable("Hello", 3.5, "Test");
        Assert.assertEquals(0, source.ofType(Integer.class).count());
    }

    @Test
    public void HeterogenousSourceOnlyFirstOfType() {
        IEnumerable<Object> source = Linq.asEnumerable(10, "Hello", 3.5, "Test");
        IEnumerable<Integer> expected = Linq.asEnumerable(new int[]{10});

        assertEquals(expected, source.ofType(Integer.class));
    }

    @Test
    public void AllElementsOfNullableTypeNullsSkipped() {
        IEnumerable<Object> source = Linq.asEnumerable(10, -4, null, null, 4, 9);
        IEnumerable<Integer> expected = Linq.asEnumerable(10, -4, 4, 9);

        assertEquals(expected, source.ofType(Integer.class));
    }

    @Test
    public void HeterogenousSourceSomeOfType() {
        IEnumerable<Object> source = Linq.asEnumerable(new BigDecimal("3.5"), -4, "Test", "Check", 4, 8.0, 10.5, 9);
        IEnumerable<Integer> expected = Linq.asEnumerable(new int[]{-4, 4, 9});

        assertEquals(expected, source.ofType(Integer.class));
    }

    @Test
    public void RunOnce() {
        IEnumerable<Object> source = Linq.asEnumerable(new BigDecimal("3.5"), -4, "Test", "Check", 4, 8.0, 10.5, 9);
        IEnumerable<Integer> expected = Linq.asEnumerable(new int[]{-4, 4, 9});

        assertEquals(expected, source.runOnce().ofType(Integer.class));
    }

    @Test
    public void IntFromNullableInt() {
        IEnumerable<Integer> source = Linq.asEnumerable(new int[]{-4, 4, 9});
        IEnumerable<Integer> expected = Linq.asEnumerable(-4, 4, 9);

        assertEquals(expected, source.ofType(Integer.class));
    }

    @Test
    public void IntFromNullableIntWithNulls() {
        IEnumerable<Integer> source = Linq.asEnumerable(null, -4, 4, null, 9);
        IEnumerable<Integer> expected = Linq.asEnumerable(new int[]{-4, 4, 9});

        assertEquals(expected, source.ofType(Integer.class));
    }

    @Test
    public void NullableDecimalFromString() {
        IEnumerable<String> source = Linq.asEnumerable("Test1", "Test2", "Test9");
        Assert.assertEquals(0, source.ofType(BigDecimal.class).count());
    }

    @Test
    public void LongFromDouble() {
        IEnumerable<Long> source = Linq.asEnumerable(new long[]{99L, 45L, 81L});
        Assert.assertEquals(0, source.ofType(Double.class).count());
    }

    @Test
    public void NullSource() {
        assertThrows(NullPointerException.class, () -> ((IEnumerable<Object>) null).ofType(String.class));
    }

    @Test
    public void ForcedToEnumeratorDoesntEnumerate() {
        IEnumerable<Integer> iterator = NumberRangeGuaranteedNotCollectionType(0, 3).ofType(Integer.class);
        // Don't insist on this behaviour, but check it's correct if it happens
        IEnumerator en = as(iterator, IEnumerator.class);
        Assert.assertFalse(en != null && en.moveNext());
    }

    @Test
    public void testOfType() {
        List<Number> numbers = Arrays.asList(2, null, 3.14, 5);
        IEnumerator<Integer> enumerator = Linq.asEnumerable(numbers)
                .ofType(Integer.class)
                .enumerator();
        Assert.assertTrue(enumerator.moveNext());
        Assert.assertEquals(Integer.valueOf(2), enumerator.current());
        Assert.assertTrue(enumerator.moveNext());
        Assert.assertEquals(Integer.valueOf(5), enumerator.current());
        Assert.assertFalse(enumerator.moveNext());
    }
}
