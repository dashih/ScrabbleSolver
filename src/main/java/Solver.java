import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

public final class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int PAD = 80;

    private final Set<String> m_dictionary;
    private final boolean m_parallel;
    private final int m_minCharacters;
    private final Pattern m_regex;

    /**
     * ctor
     */
    private Solver(boolean parallel, int minCharacters, Pattern regex) throws IOException {
        m_parallel = parallel;
        m_minCharacters = minCharacters;
        m_regex = Preconditions.checkNotNull(regex);

        m_dictionary = new HashSet<>();
        populateDictionary();
    }

    /**
     * Solve
     */
    void solve(String input) {
        System.out.println(String.format("Input: %s (%d length, %d blanks)\n" +
                        "Running in %s mode\n" +
                        "Outputting matches of %d length or greater\n" +
                        "Outputting matches of pattern: %s\n" +
                        "%s",
                input,
                input.length(),
                StringUtils.countMatches(input, "*"),
                m_parallel ? "parallel" : "sequential",
                m_minCharacters,
                m_regex,
                StringUtils.repeat('*', PAD)));

        ConcurrentMap<String, Boolean> solutions = new ConcurrentHashMap<>();
        long numProcessed = 0;
        try (StatusReporter reporter = new StatusReporter()) {
            List<StringBuilder> combinations = new ArrayList<>();
            getCombinationswithBlanks(new StringBuilder(input), combinations);

            Stream<StringBuilder> stream = m_parallel ? combinations.parallelStream() : combinations.stream();
            stream.forEach(combination -> permute(combination, 0, solutions, reporter));
            numProcessed = reporter.get();
        }

        System.out.println(String.format("%s\nFound %,d solutions for %s\nProcessed %,d",
                StringUtils.repeat('*', PAD),
                solutions.size(),
                input,
                numProcessed));
    }

    private void populateDictionary() throws IOException {
        InputStream in = getClass().getResourceAsStream("/dictionary.txt");
        m_dictionary.addAll(CharStreams.readLines(new InputStreamReader(in)));
    }

    private static void getCombinationswithBlanks(StringBuilder s, List<StringBuilder> combinations) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    s.setCharAt(i, c);
                    getCombinationswithBlanks(s, combinations);
                }

                return;
            }
        }

        getCombinations(s, new StringBuilder(), 0, combinations);
    }

    private static void getCombinations(
        StringBuilder s, StringBuilder build, int idx, List<StringBuilder> combinations) {

        for (int i = idx; i < s.length(); i++) {
            build.append(s.charAt(i));

            combinations.add(new StringBuilder(build));

            getCombinations(s, build, i + 1, combinations);
            build.deleteCharAt(build.length() - 1);
        }
    }

    private void permute(StringBuilder s, int idx, ConcurrentMap<String, Boolean> solutions, StatusReporter reporter) {
        if (idx == s.length()) {
            String str = s.toString();

            // Check if it's a unique solution that meets the criteria.
            if (m_dictionary.contains(str) &&
                    solutions.putIfAbsent(str, true) == null &&
                    str.length() >= m_minCharacters &&
                    m_regex.matcher(str).matches()) {

                // Pad spaces to the right to overwrite any status output.
                System.out.println(StringUtils.rightPad(str, PAD));
            }

            reporter.increment();
            return;
        }

        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            permute(s, idx + 1, solutions, reporter);
            swap(s, idx, i);
        }
    }

    private static void swap(StringBuilder s, int idx0, int idx1) {
        char tmp = s.charAt(idx0);
        s.setCharAt(idx0, s.charAt(idx1));
        s.setCharAt(idx1, tmp);
    }

    static class Builder {
        boolean parallel = true;
        int minCharacters = 6;
        Pattern regex = Pattern.compile("[A-Z]+");

        Builder withParallel(boolean value) {
            parallel = value;
            return this;
        }

        Builder withMinCharaters(int value) {
            minCharacters = value;
            return this;
        }

        Builder withRegex(String value) {
            regex = Pattern.compile(value);
            return this;
        }

        Solver build() throws IOException {
            return new Solver(parallel, minCharacters, regex);
        }
    }
}
