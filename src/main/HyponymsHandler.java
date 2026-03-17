package main;

import browser.NgordnetQuery;
import browser.NgordnetQueryHandler;
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
        return String.valueOf(net.hyponyms(words.get(0), start, end, k));
//        return String.join(",", net.hyponyms(words));
    }
}
