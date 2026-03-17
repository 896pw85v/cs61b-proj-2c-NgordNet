package ngrams;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * An object for mapping a year number (e.g. 1996) to numerical data. Provides
 * utility methods useful for data analysis.
 *
 * @author Josh Hug
 */
public class TimeSeries extends TreeMap<Integer, Double> {

    /** If it helps speed up your code, you can assume year arguments to your NGramMap
     * are between 1400 and 2100. We've stored these values as the constants
     * MIN_YEAR and MAX_YEAR here. */
    public static final int MIN_YEAR = 1400;
    public static final int MAX_YEAR = 2100;

    /**
     * Constructs a new empty TimeSeries.
     */
    public TimeSeries() {
        super();
    }

    /**
     * Creates a copy of TS, but only between STARTYEAR and ENDYEAR,
     * inclusive of both end points.
     */
    public TimeSeries(TimeSeries ts, int startYear, int endYear) {
        super();
        this.putAll(ts.subMap(startYear, true, endYear, true));
    }

    /**
     * Returns all years for this TimeSeries (in any order).
     */
    public List<Integer> years() {
        return new ArrayList<>(this.keySet());
        // started from an iteration to three lines of code to just one line of very concise func call
//        return null;
    }

    /**
     * Returns all data for this TimeSeries (in any order).
     * Must be in the same order as years().
     */
    public List<Double> data() {
        // same order as years, so just use years
//        List<Double> list = new ArrayList<>();
//        for (int i : this.years()) {
//            list.add(this.get(i));
//        }
//        return list;
        // or. cuz it's also method from java so should work fine
        return new ArrayList<>(this.values());
//        return null;
    }

    /**
     * Returns the year-wise sum of this TimeSeries with the given TS. In other words, for
     * each year, sum the data from this TimeSeries with the data from TS. Should return a
     * new TimeSeries (does not modify this TimeSeries).
     *
     * If both TimeSeries don't contain any years, return an empty TimeSeries.
     * If one TimeSeries contains a year that the other one doesn't, the returned TimeSeries
     * should store the value from the TimeSeries that contains that year.
     */
    public TimeSeries plus(TimeSeries ts) {
        if (this.isEmpty()) {
            if (ts.isEmpty()) return new TimeSeries();
            else {
                TimeSeries copy =  new TimeSeries();
                return (TimeSeries) copy.clone();
            }
        } else if (ts.isEmpty()) {
            return (TimeSeries) this.clone();
        }
        TimeSeries newts = new TimeSeries();
        Integer i = this.firstKey();
        Integer j = ts.firstKey();
        while (i != null && j != null) {
            if (i == null) {
                newts.putAll(ts.tailMap(j));
                break;
            } else if (j == null) {
                newts.putAll((this.tailMap(i)));
                break;
            } else if (i > j) {
                newts.put(j, ts.get(j));
                j = ts.higherKey(j);
            } else if (j > i) {
                newts.put(i, this.get(i));
                i = this.higherKey(i);
            } else {
                newts.put(i, this.get(i) + ts.get(j));
                i = this.higherKey(i);
                j = ts.higherKey(j);
            }
        } // i believe this is perfect
        return newts;
//        return null;
    }

    /**
     * Returns the quotient of the value for each year this TimeSeries divided by the
     * value for the same year in TS. Should return a new TimeSeries (does not modify this
     * TimeSeries).
     *
     * If TS is missing a year that exists in this TimeSeries, throw an
     * IllegalArgumentException.
     * If TS has a year that is not in this TimeSeries, ignore it.
     */
    public TimeSeries dividedBy(TimeSeries ts) {
        TimeSeries newts = new TimeSeries();
        // i will start with a check
        for (Integer k : this.keySet()) {
            if (!ts.containsKey(k)) {
                throw new IllegalArgumentException();
            }
        }
        for (Integer k : this.keySet()) {
            newts.put(k, this.get(k) / ts.get(k));
        }
        return newts;
    }

    /**
     * Supposedly returns the total appearance of this Ts over its entire time period. Not considering start and end
     * year because in design the Ts should have proper time period when created.
     * @return the total appearance count of this, word?
     */
    public double popularity() {
        double sum = 0;
        for (Double value : this.values()) sum += value;
        return sum;
    }

    // I don't think I used any helper methods
}
