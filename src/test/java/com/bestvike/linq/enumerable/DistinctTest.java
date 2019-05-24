package com.bestvike.linq.enumerable;

import com.bestvike.TestCase;
import com.bestvike.collections.generic.EqualityComparer;
import com.bestvike.collections.generic.IEqualityComparer;
import com.bestvike.collections.generic.StringComparer;
import com.bestvike.linq.IEnumerable;
import com.bestvike.linq.IEnumerator;
import com.bestvike.linq.Linq;
import com.bestvike.linq.entity.Employee;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Created by 许崇雷 on 2018-05-10.
 */
public class DistinctTest extends TestCase {
    @Test
    public void SameResultsRepeatCallsIntQuery() {
        IEnumerable<Integer> q = Linq.asEnumerable(new int[]{0, 9999, 0, 888, -1, 66, -1, -777, 1, 2, -12345, 66, 66, -1, -1})
                .where(x -> x > Integer.MIN_VALUE);

        assertEquals(q.distinct(), q.distinct());
    }

    @Test
    public void SameResultsRepeatCallsStringQuery() {
        IEnumerable<String> q = Linq.asEnumerable(new String[]{"!@#$%^", "C", "AAA", "Calling Twice", "SoS"})
                .where(x -> IsNullOrEmpty(x));

        assertEquals(q.distinct(), q.distinct());
    }

    @Test
    public void EmptySource() {
        int[] source = {};
        assertEmpty(Linq.asEnumerable(source).distinct());
    }

    @Test
    public void EmptySourceRunOnce() {
        int[] source = {};
        assertEmpty(Linq.asEnumerable(source).runOnce().distinct());
    }

    @Test
    public void SingleNullElementExplicitlyUseDefaultComparer() {
        String[] source = {null};
        String[] expected = {null};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct(EqualityComparer.Default()));
    }

    @Test
    public void EmptyStringDistinctFromNull() {
        String[] source = {null, null, Empty};
        String[] expected = {null, Empty};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct(EqualityComparer.Default()));
    }

    @Test
    public void CollapsDuplicateNulls() {
        String[] source = {null, null};
        String[] expected = {null};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct(EqualityComparer.Default()));
    }

    @Test
    public void SourceAllDuplicates() {
        int[] source = {5, 5, 5, 5, 5, 5};
        int[] expected = {5};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct());
    }

    @Test
    public void AllUnique() {
        int[] source = {2, -5, 0, 6, 10, 9};

        assertEquals(Linq.asEnumerable(source), Linq.asEnumerable(source).distinct());
    }

    @Test
    public void SomeDuplicatesIncludingNulls() {
        Integer[] source = {1, 1, 1, 2, 2, 2, null, null};
        Integer[] expected = {1, 2, null};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct());
    }

    @Test
    public void SomeDuplicatesIncludingNullsRunOnce() {
        Integer[] source = {1, 1, 1, 2, 2, 2, null, null};
        Integer[] expected = {1, 2, null};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).runOnce().distinct());
    }

    @Test
    public void LastSameAsFirst() {
        int[] source = {1, 2, 3, 4, 5, 1};
        int[] expected = {1, 2, 3, 4, 5};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct());
    }

    // Multiple elements repeat non-consecutively
    @Test
    public void RepeatsNonConsecutive() {
        int[] source = {1, 1, 2, 2, 4, 3, 1, 3, 2};
        int[] expected = {1, 2, 4, 3};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct());
    }

    @Test
    public void RepeatsNonConsecutiveRunOnce() {
        int[] source = {1, 1, 2, 2, 4, 3, 1, 3, 2};
        int[] expected = {1, 2, 4, 3};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).runOnce().distinct());
    }

    @Test
    public void NullComparer() {
        String[] source = {"Bob", "Tim", "bBo", "miT", "Robert", "iTm"};
        String[] expected = {"Bob", "Tim", "bBo", "miT", "Robert", "iTm"};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct());
    }

    @Test
    public void NullSource() {
        IEnumerable<String> source = null;

        assertThrows(NullPointerException.class, () -> source.distinct());
    }

    @Test
    public void NullSourceCustomComparer() {
        IEnumerable<String> source = null;

        assertThrows(NullPointerException.class, () -> source.distinct(StringComparer.Ordinal));
    }

    @Test
    public void CustomEqualityComparer() {
        String[] source = {"Bob", "Tim", "bBo", "miT", "Robert", "iTm"};
        String[] expected = {"Bob", "Tim", "Robert"};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct(new AnagramEqualityComparer()), new AnagramEqualityComparer());
    }

    @Test
    public void CustomEqualityComparerRunOnce() {
        String[] source = {"Bob", "Tim", "bBo", "miT", "Robert", "iTm"};
        String[] expected = {"Bob", "Tim", "Robert"};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).runOnce().distinct(new AnagramEqualityComparer()), new AnagramEqualityComparer());
    }

    private IEnumerable<Object[]> SequencesWithDuplicates() {
        List<Object[]> lst = new ArrayList<>();
        // Validate an array of different numeric data types.
        lst.add(new Object[]{Linq.asEnumerable(new int[]{1, 1, 1, 2, 3, 5, 5, 6, 6, 10})});
        lst.add(new Object[]{Linq.asEnumerable(new long[]{1, 1, 1, 2, 3, 5, 5, 6, 6, 10})});
        lst.add(new Object[]{Linq.asEnumerable(new float[]{1, 1, 1, 2, 3, 5, 5, 6, 6, 10})});
        lst.add(new Object[]{Linq.asEnumerable(new double[]{1, 1, 1, 2, 3, 5, 5, 6, 6, 10})});
        lst.add(new Object[]{Linq.asEnumerable(new BigDecimal(1), new BigDecimal(1), new BigDecimal(1), new BigDecimal(2), new BigDecimal(3), new BigDecimal(5), new BigDecimal(5), new BigDecimal(6), new BigDecimal(6), new BigDecimal(10))});
        // Try strings
        lst.add(new Object[]{Linq.asEnumerable("add",
                "add",
                "subtract",
                "multiply",
                "divide",
                "divide2",
                "subtract",
                "add",
                "power",
                "exponent",
                "hello",
                "class",
                "namespace",
                "namespace",
                "namespace")
        });
        return Linq.asEnumerable(lst);
    }

    @Test
    public void FindDistinctAndValidate() {
        for (Object[] objects : this.SequencesWithDuplicates()) {
            this.FindDistinctAndValidate((IEnumerable<?>) objects[0]);
        }
    }

    private <T> void FindDistinctAndValidate(IEnumerable<T> original) {
        // Convert to list to avoid repeated enumerations of the enumerables.
        List<T> originalList = original.toList();
        List<T> distinctList = Linq.asEnumerable(originalList).distinct().toList();

        // Ensure the result doesn't contain duplicates.
        HashSet<T> hashSet = new HashSet<>();
        for (T i : distinctList)
            Assert.assertTrue(hashSet.add(i));

        HashSet<T> originalSet = new HashSet<>(originalList);
        assertSuperset(originalSet, hashSet);
        assertSubset(originalSet, hashSet);
    }

    @Test
    public void ForcedToEnumeratorDoesntEnumerate() {
        IEnumerable<Integer> iterator = NumberRangeGuaranteedNotCollectionType(0, 3).distinct();
        // Don't insist on this behaviour, but check it's correct if it happens
        IEnumerator<Integer> en = (IEnumerator<Integer>) iterator;
        Assert.assertFalse(en != null && en.moveNext());
    }

    @Test
    public void toArray() {
        Integer[] source = {1, 1, 1, 2, 2, 2, null, null};
        Integer[] expected = {1, 2, null};

        assertEquals(Linq.asEnumerable(expected), Linq.asEnumerable(source).distinct().toArray());
    }

    @Test
    public void toList() {
        Integer[] source = {1, 1, 1, 2, 2, 2, null, null};
        Integer[] expected = {1, 2, null};

        Assert.assertArrayEquals(Linq.asEnumerable(expected).toList().toArray(), Linq.asEnumerable(source).distinct().toList().toArray());
    }

    @Test
    public void Count() {
        Integer[] source = {1, 1, 1, 2, 2, 2, null, null};
        Assert.assertEquals(3, Linq.asEnumerable(source).distinct().count());
    }

    @Test
    public void RepeatEnumerating() {
        Integer[] source = {1, 1, 1, 2, 2, 2, null, null};

        IEnumerable<Integer> result = Linq.asEnumerable(source).distinct();

        assertEquals(result, result);
    }

    @Test
    public void testDistinct() {
        Employee[] emps2 = {
                new Employee(150, "Theodore", 10),
                emps[3],
                emps[0],
                emps[3],
        };
        Assert.assertEquals(3, Linq.asEnumerable(emps2).distinct().count());
    }

    @Test
    public void testDistinctWithEqualityComparer() {
        IEqualityComparer<Employee> comparer = new IEqualityComparer<Employee>() {
            @Override
            public boolean equals(Employee x, Employee y) {
                return Objects.equals(x.deptno, y.deptno);
            }

            @Override
            public int hashCode(Employee obj) {
                return obj.deptno.hashCode();
            }
        };

        Employee[] emps2 = {
                new Employee(150, "Theodore", 10),
                emps[3],
                emps[1],
                emps[3]
        };
        Assert.assertEquals(2, Linq.asEnumerable(emps2).distinct(comparer).count());
    }
}
