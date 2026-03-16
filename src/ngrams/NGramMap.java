package ngrams;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Scanner;
import java.io.File;

import static ngrams.TimeSeries.MAX_YEAR;
import static ngrams.TimeSeries.MIN_YEAR;

/**
 * An object that provides utility methods for making queries on the
 * Google NGrams dataset (or a subset thereof).
 *
 * An NGramMap stores pertinent data from a "words file" and a "counts
 * file". It is not a map in the strict sense, but it does provide additional
 * functionality.
 *
 * @author Josh Hug
 */
public class NGramMap {

    public TreeMap<String, TimeSeries> wordMap = new TreeMap<>();
    TimeSeries counts = new TimeSeries();

    /**
     * Constructs an NGramMap from WORDSFILENAME and COUNTSFILENAME.
     */
    public NGramMap(String wordsFilename, String countsFilename) {
        File countFile = new File( countsFilename);
        try {
            Scanner readCounts = new Scanner(countFile);
            while (readCounts.hasNextLine()) {
                String[] entry = readCounts.nextLine().split(",");
                counts.put(Integer.parseInt(entry[0]), Double.parseDouble(entry[1]));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        File wordFile = new File(wordsFilename);
        Scanner read = null;
        try {
            read = new Scanner(wordFile);
            while (read.hasNextLine()) {
                String line = read.nextLine();
                String[] tokens = line.split("\t");
                String word = tokens[0];
                Integer year = Integer.parseInt(tokens[1]);
                Double appearance = Double.parseDouble(tokens[2]);
                if (wordMap.containsKey(word)) {
                    TimeSeries ts = wordMap.get(word);
                    ts.put(year, appearance);
                } else {
                    TimeSeries ts = new TimeSeries();
                    ts.put(year, appearance);
                    wordMap.put(word, ts);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        /* i hate this. this def creates weird bug that's hard fix.
           null pointer and value error stuff
         */
    }

    /**
     * Provides the history of WORD between STARTYEAR and ENDYEAR, inclusive of both ends. The
     * returned TimeSeries should be a copy, not a link to this NGramMap's TimeSeries. In other
     * words, changes made to the object returned by this function should not also affect the
     * NGramMap. This is also known as a "defensive copy". If the word is not in the data files,
     * returns an empty TimeSeries.
     */
    public TimeSeries countHistory(String word, int startYear, int endYear) {
        if (wordMap.containsKey(word)) {
            return new TimeSeries(wordMap.get(word), startYear, endYear);
        } else return new TimeSeries();
    }

    /**
     * Provides the history of WORD. The returned TimeSeries should be a copy, not a link to this
     * NGramMap's TimeSeries. In other words, changes made to the object returned by this function
     * should not also affect the NGramMap. This is also known as a "defensive copy". If the word
     * is not in the data files, returns an empty TimeSeries.
     */
    public TimeSeries countHistory(String word) {
        if (! wordMap.containsKey(word)) return new TimeSeries();
        TimeSeries ts = wordMap.get(word);
        return new TimeSeries(ts, ts.firstKey(), ts.lastKey());

    }

    /**
     * Returns a defensive copy of the total number of words recorded per year in all volumes.
     */
    public TimeSeries totalCountHistory() {
        return new TimeSeries(counts, counts.firstKey(), counts.lastKey());
    }

    /**
     * Provides a TimeSeries containing the relative frequency per year of WORD between STARTYEAR
     * and ENDYEAR, inclusive of both ends. If the word is not in the data files, returns an empty
     * TimeSeries.
     */
    public TimeSeries weightHistory(String word, int startYear, int endYear) {
        if (!wordMap.containsKey(word)) return new TimeSeries();
        TimeSeries ts = countHistory(word, startYear, endYear).dividedBy(counts);
        return new TimeSeries(ts, ts.firstKey(), ts.lastKey());
    }

    /**
     * Provides a TimeSeries containing the relative frequency per year of WORD compared to all
     * words recorded in that year. If the word is not in the data files, returns an empty
     * TimeSeries.
     */
    public TimeSeries weightHistory(String word) {
        // TODO: Fill in this method.
        if (!wordMap.containsKey(word)) return new TimeSeries();
        TimeSeries ts = wordMap.get(word).dividedBy(counts);
        return new TimeSeries(ts, ts.firstKey(), ts.lastKey());
    }

    /**
     * Provides the summed relative frequency per year of all words in WORDS between STARTYEAR and
     * ENDYEAR, inclusive of both ends. If a word does not exist in this time frame, ignore it
     * rather than throwing an exception.
     */
    public TimeSeries summedWeightHistory(Collection<String> words,
                                          int startYear, int endYear) {
        // TODO: Fill in this method.
        TimeSeries ts = new TimeSeries();
        for (String word : words) {
            TimeSeries copy = new TimeSeries(wordMap.get(word), startYear, endYear);
            ts = copy.plus(ts);
        }
        return ts.dividedBy(counts);
    }

    /**
     * Returns the summed relative frequency per year of all words in WORDS. If a word does not
     * exist in this time frame, ignore it rather than throwing an exception.
     */
    public TimeSeries summedWeightHistory(Collection<String> words) {
        // TODO: Fill in this method.
        TimeSeries ts = new TimeSeries();
        for (String word : words) {
            ts = wordMap.get(word).plus(ts);
        }
        return ts.dividedBy(counts);
//        return null;
    }

    // TODO: Add any private helper methods.
    // TODO: Remove all TODO comments before submitting.
    // no way
}
