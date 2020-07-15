import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.io.CharStreams;

public class ScrabbleSolver {
    private static final Set<String> DICTIONARY = new HashSet<>();
    private static final ConcurrentMap<String, Boolean> FOUND = new ConcurrentHashMap<>();

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static String input;
    private static boolean parallel = true;
    private static int minSize = 5;

    private static void swap(StringBuilder s, int idx0, int idx1) {
        char tmp = s.charAt(idx0);
        s.setCharAt(idx0, s.charAt(idx1));
        s.setCharAt(idx1, tmp);
    }

    private static void solve(StringBuilder s, int idx) {
        if (idx == s.length()) {
            return;
        }

        String str = s.toString();

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    s.setCharAt(i, c);
                    solve(s, idx);
                }

                return;
            }
        }

        if (!FOUND.containsKey(str) && DICTIONARY.contains(str)) {
            if (str.length() >= minSize) {
                System.out.println(str);
            }
            FOUND.put(str, true);
        }

        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            solve(s, idx + 1);
            swap(s, idx, i);
        }

        for (int i = idx; i < s.length(); i++) {
            char tmp = s.charAt(i);
            s.deleteCharAt(i);
            solve(s, idx);
            s.insert(i, tmp);
        }
    }

    private static void readDictionary() throws IOException {
        InputStream in = ScrabbleSolver.class.getResourceAsStream("/dictionary.txt");
        DICTIONARY.addAll(CharStreams.readLines(new InputStreamReader(in)));
    }

    private static void parseOptions(String[] args) throws ParseException {
        Options ops = new Options();
        ops.addOption(Option.builder("s").longOpt("sequential").desc("Run in sequential mode").build());
        ops.addOption(Option.builder("n").longOpt("min-characters").desc("Minimum characters for match to print").hasArg().build());
        ops.addOption(Option.builder("i").longOpt("input").desc("Input sequence to solve").hasArg().required().build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(ops, args);

        parallel = !cmd.hasOption("s");
        minSize = cmd.hasOption("n") ? Integer.parseInt(cmd.getOptionValue("n")) : minSize;
        input = cmd.getOptionValue("i");
    }

    public static void main(String[] args) throws IOException, ParseException {
        parseOptions(args);

        StringBuilder s = new StringBuilder(input);
        readDictionary();

        System.out.println(String.format("Running in %s mode\n********************************", parallel ? "parallel" : "sequential"));
        if (parallel) {
            List<StringBuilder> startingPoints = new ArrayList<>();
            for (int i = 0; i < s.length(); i++) {
                swap(s, 0, i);
                startingPoints.add(new StringBuilder(s.toString()));

                char tmp = s.charAt(0);
                s.deleteCharAt(0);
                startingPoints.add(new StringBuilder(s.toString()));
                s.insert(0, tmp);
            }

            startingPoints.parallelStream().forEach(startingPoint -> {
                solve(startingPoint, startingPoint.length() == s.length() ? 1 : 0);
            });
        } else {
            solve(s, 0);
        }

        System.out.println(String.format("********************************\nFound %s words for %s", FOUND.size(), input));
    }
}
