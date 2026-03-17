package main;

import browser.NgordnetQuery;
import browser.NgordnetQueryHandler;
import browser.NgordnetQueryType;
import graph.WordNet;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HyponymsHandler extends NgordnetQueryHandler {
    WordNet net;
    public HyponymsHandler(String hyponyms, String synsets, String wordFile, String countFile) {
        net = new WordNet(hyponyms, synsets, wordFile, countFile);
    }

    @Override
    public String handle(NgordnetQuery q) {
        List<String> words = q.words();
        int start = q.startYear();
        int end = q.endYear();
        int k = q.k();
        NgordnetQueryType type = q.ngordnetQueryType();
        return
            switch (type) {
                case NgordnetQueryType.HYPONYMS -> String.valueOf(net.hyponyms(words, start, end, k));
                case NgordnetQueryType.ANCESTORS -> String.valueOf(net.ancestors(words, start, end, k));
            }; // what is this valueOf
    }
}
