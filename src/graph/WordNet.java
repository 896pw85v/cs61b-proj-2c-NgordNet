package graph;

import ngrams.NGramMap;

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
                Integer[] indices = new Integer[words.length];
                for (int i = 0; i < words.length; i++) {
                    indices[i] = Integer.parseInt(words[i]);
                }
                TreeSet<Integer> hypo = new TreeSet<>(List.of(indices));
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

    // reserve as a maybe helpful helper method
    public TreeSet<Integer> getAllChildKeys(int i) {
        if (!graph.containsKey(i)) return new TreeSet<>();
        TreeSet<Integer> directChildren = graph.get(i);
        TreeSet<Integer> res = new TreeSet<>(directChildren);
        for (int directChild : directChildren) {
            res.addAll(getAllChildKeys(directChild));
        }
        return res;
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
        if (list.size() == 1) return hyponyms(list.getFirst());
        String[] array = list.toArray(new String[0]);
        return hyponyms(array);
    }

    /**
     * Return a list of common hyponyms of words provided that are most popular between start and end year.
     * @param words parent words
     * @param startYear start of time period
     * @param endYear end of time period
     * @param k max size of return list
     * @return top k most popular hyponyms in alphabetical order
     */
    public List<String> hyponyms(List<String> words, int startYear, int endYear, int k) {
        List<String> list = hyponyms(words);
        if (k == 0) return list;
        return kProcessor(list, startYear, endYear, k);
    }

    /**
     * gets ancestors of every node containing the given word.
     * @implNote this is the biggest bottleneck, exactly why the design is so wrong, and this implementation is wrong
     * It works by getting every index (node) with word, which is fully O(N)
     * then for every key k in graph G, search all kids of k in O(G), if k contains all the nodes with word, add it to
     * return list.
     * This is exactly what we are not supposed to do. And the isParent() is still unused
     * @param word the child word
     * @return a list of the parents, including the nodes containing the word
     */
    public List<String> ancestors(String word) {
        TreeSet<Integer> children = table.getIndices(word);
        List<String> parents = new ArrayList<>();
        for (int i : children) parents.addAll(get(i));
        for (int k : table.keys()) {
            for (int child : children) {
                if (isParentOf(k, child)) {
                    parents.addAll(get(k));
                }
            }
        }
        parents.sort(new WComparator());
        return parents;
    }

    /**
     * gets ancestors of the all the words provided, works by obtaining the common set of ancestors of different words
     * @param words the list of words to find common ancestors
     * @return list of common ancestors,
     */
    public List<String> ancestors(List<String> words) {
        if (words == null) return new ArrayList<>();
        if (words.size() == 1) return ancestors(words.getFirst());
        Set<String> set = new TreeSet<>(this.hyponyms(words.getFirst()));
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
     * works as the entry for ancestors methods set
     * @param words children
     * @param startYear start
     * @param endYear end of time period
     * @param k max size of result
     * @return common ancestors of provided words
     */
    public List<String> ancestors(List<String> words, int startYear, int endYear, int k) {
        if (k == 0) return ancestors(words);
        return kProcessor(ancestors(words), startYear, endYear, k);
    }

    /**
     * Returns whether node i points to node j in a single direction. Worst case runtime
     * should be the whole graph. However， this method is not yet used in the implementation,
     * due to the design not aligning with the actual problem and data structure.
     * @param i the starting node
     * @param j the target node
     * @return boolean value true if j is a child of i, vice versa
     */
    public boolean isParentOf(int i, int j) {
        TreeSet<Integer> set = this.getChildKeys(i);
        if (set.contains(j)) return true;
        boolean connected = false;
        for (int child : set) connected = connected || isParentOf(child, j);
        return connected;
    }

    /**
     * Processes the provided list of strings by sorting by popularity at first, then extract the top k items, again
     * sort alphabetically.
     * @param list a list of words prepared by other methods
     * @param startYear start of time period
     * @param endYear end of time period
     * @param k max size of result
     * @return a processed list of at most k or less, or all, of the items of the provided list, sorted by popularity
     * @implNote
     * Even though hyponyms/ancestors are not obtained in this list, but ultimately the passed in list costs by G size
     * of graph, gets counts by same G TimeSeries thankfully it's constant time, gets creates mapping of G size, convert
     * to list of G size, sort by G log(G), probably G for reversing, iterating takes at most k times, alphabetical
     * sorting takes G log(G), in total 5G + k + 2G log(G). Maybe this can be cut by one G  but the sorting and mappings,
     * etc., cannot be cut since spec requires so.
     */
    private List<String> kProcessor(List<String> list, int startYear, int endYear, int k) {

        // create a mapping of hyponym -> their appearance count
        TreeMap<String, Double> mapping = new TreeMap<>();
        for (String w : list) {
            mapping.put(w, map.countHistory(w, startYear, endYear).popularity()); // order guaranteed
        }

        // here goes the trick to sort by value of a map
        List<Map.Entry<String, Double>> toList = new ArrayList<>(mapping.entrySet());
        toList.sort( Map.Entry.comparingByValue()); // passed in comparator from Map.Entry two classes imported
        toList = toList.reversed(); // reverse order so higher appearance values at front

        // use iterator to get top k or all elements, add to a list
        Iterator<Map.Entry<String, Double>> it =  toList.iterator();
        List<String> returnList = new ArrayList<>();
        for (int i = k; k != i || it.hasNext(); i++) returnList.add(it.next().getKey());

        // sort again by alphabetical order
        returnList.sort(new WComparator());

        return returnList;
    }
}
