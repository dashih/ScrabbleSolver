import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ScrabbleSolver {
    private static final boolean PARALLEL = true;
    private static final int MIN_SIZE = 5;
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final Set<String> DICTIONARY = new HashSet<>();
    private static final ConcurrentMap<String, Boolean> FOUND = new ConcurrentHashMap<>();

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
            if (str.length() >= MIN_SIZE) {
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = reader.readLine();
            while (line != null) {
                DICTIONARY.add(line);
                line = reader.readLine();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Pass input.");
            System.exit(1);
        }

        String input = args[0];
        StringBuilder s = new StringBuilder(input);
        readDictionary();

        if (PARALLEL) {
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
