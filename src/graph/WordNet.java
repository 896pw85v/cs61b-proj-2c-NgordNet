package graph;

import ngrams.NGramMap;
import ngrams.TimeSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Represents a directed graph of hyponym relationships.
 * @author me
 *
 */
public class WordNet {
    // int -> set of int
    // look up int -> words in synsets table, handled by Synset
    // for outside world this is not a graph class. So ultimate api is:
    // - String word -> String words[] of all hyponyms
    // - String words[] -> words[] all common hyponyms

    public TreeMap<Integer, TreeSet<Integer>> graph;
    public Synset table;
    public NGramMap map;

    /**
     * Creates a WordNet obj with empty graph.
     * Don't think this is needed, but still want to provide an empty constructor.
     */
    public WordNet() {
        graph = new TreeMap<>();
        table = new Synset();
        map = new NGramMap();
    }

    /**
     * Create a WordNet obj by reading the txt files at the provided path.
     * Runtime would simply be M + N, size of the two dataset
     * @param hyponymPath that points to the hyponym data set
     * @param synsetPath that points to the set of words
     * @author me
     */
    public WordNet(String hyponymPath, String synsetPath, String wordFrequencyPath, String countPath) {
        table = new Synset(synsetPath);
        graph = new TreeMap<>();
        File file = new File(hyponymPath);
        try {
            Scanner read = new Scanner(file);
            while (read.hasNextLine()) {
                String line = read.nextLine();
                String[] tokens = line.split(",");
                String[] words = Arrays.copyOfRange(tokens,1, tokens.length);
                Integer[] wordz = new Integer[words.length];
                for (int i = 0; i < words.length; i++) {
                    wordz[i] = Integer.parseInt(words[i]);
                }
                TreeSet<Integer> hypo = new TreeSet<>(List.of(wordz));
                int i = Integer.parseInt(tokens[0]);
                if (graph.get(i) == null) graph.put(i, hypo);
                else {
                    TreeSet<Integer> set = graph.get(i);
                    set.addAll(hypo);
                    graph.put(i, set);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        map = new NGramMap(wordFrequencyPath, countPath);
    }

    /**
     * get the set of words contained in the current node
     * @param i index of the current node
     * @return a TreeSet of words that are in the current node
     */
    public TreeSet<String> get(int i) {
        return table.get(i);
    }

    /**
     * simply returns the child indices of the current index
     * @param i the current (parent) index
     * @return a set of integers that are indices to the child nodes
     */
    public TreeSet<Integer> getChildKeys(int i) {
        if (!graph.containsKey(i)) return new TreeSet<>();
        return graph.get(i);
    }

    /**
     * a recursive approach that gets all the hyponyms in words not indices. Does not include
     * the current index, ordered in the order of the words being added to the list. Strictly
     * node (synset) based.
     * @param i the current (parent) node index
     * @return a list (ArrayList) of strings that are hyponyms of the current (parent) node
     */
    public List<String> getHyponyms(int i) {

        List<String> list = new ArrayList<>();
        for (int child : this.getChildKeys(i)) {
            list.addAll(table.get(child)); // adding all words in current node
            list.addAll(this.getHyponyms(child)); // adding all the words in the child nodes, recursively
        }
        return list;
    }

    /**
     * Provided a word, searches for the hyponyms of this word, including every synset this word belongs to
     * @param word word to search for
     * @return a list (ArrayList) containing all hyponyms of the word
     */
    public List<String> hyponyms(String word) {
        Set<String> set = new TreeSet<>();
        for (int i : table.getIndices(word)) { // every node containing word
            set.addAll(this.get(i));
            set.addAll(this.getHyponyms(i));
        }
        List<String> list = new ArrayList<>(set);
        list.sort(new WComparator());
        return list;
    }

    public List<String> hyponyms(String word, int startYear, int endYear, int k) {
        if (k == 0) return hyponyms(word);
        List<String> list = hyponyms(word);
        TreeMap<String, Double> mapping = new TreeMap<>();

        for (String w : list) {
            mapping.put(w, map.countHistory(w, startYear, endYear).popularity()); // order garenteed
        }
        List<Map.Entry<String, Double>> toList = new ArrayList<>(mapping.entrySet());
        toList.sort( Map.Entry.comparingByValue());
        toList = toList.reversed();
        Iterator<Map.Entry<String, Double>> it =  toList.iterator();
        List<String> returnList = new ArrayList<>();
        for (k = k; k != 0 && it.hasNext(); k--) returnList.add(it.next().getKey());
        return returnList;


    }

    /**
     * em helper class to alphabetically sort the lists
     */
    public static class WComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    }

    /**
     * Get a list of words that are hyponyms of all the provided words, regardless of meaning.
     * List is constructed by taking the common subset of every hyponym set, therefore order
     * of words passed in is unrelated, list will be unique eventually. Creates N hyponym set
     * for N input words, and compares every word inside every set. If total size of the N sets
     * is M, would compare and add about 2M times. I don't think there's a way to make it faster.
     * <b>Additonally</b>, this method itself cannot deal with k value.
     * @param words the words to find common hyponyms
     * @return list of hyponyms, would include the most common parent word, unless not related
     */
    public List<String> hyponyms(String... words) {
        if (words == null) return new ArrayList<>();
        Set<String> set = new TreeSet<>(this.hyponyms(words[0]));
        Set<String> temp = new TreeSet<>();
        for (String word : words) {
            for (String w : this.hyponyms(word)) {
                if (set.contains(w)) temp.add(w);
            }
            set = temp;
            temp = new TreeSet<>();
        }
        List<String> list = new ArrayList<>(set);
        list.sort(new WComparator());
        return list;
    }

    /**
     * Provide a new api for hyponyms, as the argument passed in can be either arrays, var-args, or List.
     * Empty if passed in null.
     * @param list a list of words to find hyponyms
     * @return a list of String that consists of common hyponyms of the argument
     */
    public List<String> hyponyms(List<String> list) {
        if (list == null) return new ArrayList<>();
        if (list.size() == 1) return hyponyms(list.get(0));
        String[] array = list.toArray(new String[0]);
        return hyponyms(array);
    }

    /**
     * Returns whether node i points to node j in a single direction. Worst case runtime
     * should be the whole graph. However， this method is not yet used in the implementation,
     * due to the design not aligning with the actual problem and data structure.
     * @param i the starting node
     * @param j the target node
     * @return boolean value true if j is a child of i, vice versa
     */
    public boolean isConnected(int i, int j) {
        TreeSet<Integer> set = this.getChildKeys(i);
        if (set.contains(j)) return true;
        boolean connected = false;
        for (int child : set) connected = connected || isConnected(child, j);
        return connected;
    }

    public static class TComparator implements Comparator<TimeSeries> {
        @Override
        public int compare(TimeSeries o1, TimeSeries o2) {
            return (int) o2.popularity() - (int) o1.popularity();
            // casting both double to int. If they are counts they should be x.0, so no loss of information.
        }
    }
{}}
