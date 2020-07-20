import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.math.BigIntegerMath;

public final class Solver {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // At what point do we also parallelize permuting. 9 characters including 2 blanks seems to be a good threshold.
    private static final int PERMUTATION_PARALLEL_THRESH = 9;

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
                StringUtils.repeat('*', Main.CLI_PAD)));

        ConcurrentMap<String, Boolean> solutions = new ConcurrentHashMap<>();

        List<StringBuilder> combinations = new ArrayList<>();
        AtomicLong totalPerms = new AtomicLong(0L);
        getCombinationswithBlanks(new StringBuilder(input), combinations, totalPerms);

        long numProcessed = 0;
        try (StatusReporter reporter = new StatusReporter(totalPerms.get())) {
            reporter.start();

            if (m_parallel) {
                combinations.parallelStream().forEach(combination -> {
                    if (combination.length() >= PERMUTATION_PARALLEL_THRESH) {
                        List<StringBuilder> permStartPoints = new ArrayList<>();
                        for (int i = 0; i < combination.length(); i++) {
                            swap(combination, 0, i);
                            permStartPoints.add(new StringBuilder(combination));
                            swap(combination, 0, i);
                        }

                        permStartPoints.parallelStream().forEach(
                            startPoint -> permute(startPoint, 1, solutions, reporter));
                    } else {
                        permute(combination, 0, solutions, reporter);
                    }
                });
            } else {
                combinations.forEach(combination -> permute(combination, 0, solutions, reporter));
            }

            numProcessed = reporter.get();
        }

        System.out.println(String.format("%s\nFound %,d solutions for %s\nProcessed %,d permutations",
                StringUtils.repeat('*', Main.CLI_PAD),
                solutions.size(),
                input,
                numProcessed));
    }

    private void populateDictionary() throws IOException {
        InputStream in = getClass().getResourceAsStream("/dictionary.txt");
        m_dictionary.addAll(CharStreams.readLines(new InputStreamReader(in)));
    }

    private static void getCombinationswithBlanks(
        StringBuilder s, List<StringBuilder> combinations, AtomicLong totalPermutations) {

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    s.setCharAt(i, c);
                    getCombinationswithBlanks(s, combinations, totalPermutations);
                }

                return;
            }
        }

        getCombinations(s, new StringBuilder(), 0, combinations, totalPermutations);
    }

    private static void getCombinations(
        StringBuilder s, StringBuilder build, int idx, List<StringBuilder> combinations, AtomicLong totalPermutations) {

        for (int i = idx; i < s.length(); i++) {
            build.append(s.charAt(i));

            combinations.add(new StringBuilder(build));
            totalPermutations.addAndGet(BigIntegerMath.factorial(build.length()).longValueExact());

            getCombinations(s, build, i + 1, combinations, totalPermutations);
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
                System.out.println(StringUtils.rightPad(str, Main.CLI_PAD));
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
