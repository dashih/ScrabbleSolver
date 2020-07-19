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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import com.google.common.io.CharStreams;

public class ScrabbleSolver {
    private static final Set<String> DICTIONARY = new HashSet<>();
    private static final AtomicLong NUM_PROCESSED = new AtomicLong();
    private static final ThreadLocal<Long> NUM_PROCESSED_TL = new ThreadLocal<>();
    private static final ConcurrentMap<String, Boolean> SOLUTIONS = new ConcurrentHashMap<>();

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int PAD = 80;

    private static String input;
    private static boolean parallel = true;
    private static int minSize = 5;
    private static Pattern pattern = Pattern.compile("[A-Z]+");

    private static void swap(StringBuilder s, int idx0, int idx1) {
        char tmp = s.charAt(idx0);
        s.setCharAt(idx0, s.charAt(idx1));
        s.setCharAt(idx1, tmp);
    }

    private static void permute(StringBuilder s, int idx) {
        if (idx == s.length()) {
            String str = s.toString();

            // Check if it's a unique solution that meets the criteria.
            if (DICTIONARY.contains(str) &&
                SOLUTIONS.putIfAbsent(str, true) == null &&
                str.length() >= minSize &&
                pattern.matcher(str).matches()) {

                // Pad spaces to the right to overwrite any status output.
                System.out.println(StringUtils.rightPad(str, PAD));
            }

            NUM_PROCESSED_TL.set(NUM_PROCESSED_TL.get() + 1);
            return;
        }

        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            permute(s, idx + 1);
            swap(s, idx, i);
        }
    }

    private static void solveWithDeletions(StringBuilder s, int idx) {
        // Consider all permutations for this string.
        permute(s, idx);

        // Solve recursively for every permutation of deletions.
        for (int i = idx; i < s.length(); i++) {
            char tmp = s.charAt(i);
            s.deleteCharAt(i);
            solveWithDeletions(s, idx);
            s.insert(i, tmp);
        }
    }

    private static void solve(StringBuilder s, int idx) {
        // Blanks! For each blank, choose A-Z and solve each.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '*') {
                for (char c : ALPHABET.toCharArray()) {
                    s.setCharAt(i, c);
                    solve(s, idx);
                }

                return;
            }
        }

        solveWithDeletions(s, idx);
    }


    private static void readDictionary() throws IOException {
        InputStream in = ScrabbleSolver.class.getResourceAsStream("/dictionary.txt");
        DICTIONARY.addAll(CharStreams.readLines(new InputStreamReader(in)));
    }

    private static void printHelp(Options ops) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar ScrabbleSolver.java",
                            "Solve scrabble sequences\n\n",
                            ops,
                            "",
                            true);
    }

    private static void parseOptions(String[] args) throws ParseException {
        Options ops = new Options();
        ops.addOption(Option.builder("s").longOpt("sequential").desc("Run in sequential mode").build());
        ops.addOption(Option.builder("n").longOpt("min-characters").desc("Minimum characters for match to print").hasArg().build());
        ops.addOption(Option.builder("i").longOpt("input").desc("Input sequence to solve").hasArg().build());
        ops.addOption(Option.builder("r").longOpt("regex").desc("Regex to filter matches").hasArg().build());
        ops.addOption(Option.builder("h").longOpt("help").desc("Help").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(ops, args);
        if (cmd.hasOption("h")) {
            printHelp(ops);
            System.exit(0);
        }

        parallel = !cmd.hasOption("s");
        minSize = cmd.hasOption("n") ? Integer.parseInt(cmd.getOptionValue("n")) : minSize;
        pattern = cmd.hasOption("r") ? Pattern.compile(cmd.getOptionValue("r")) : pattern;
        if (cmd.hasOption("i")) {
            input = cmd.getOptionValue("i");
        } else {
            System.out.println("The --input option is required" + "\n" + "Try --help");
            System.exit(0);
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        parseOptions(args);

        StringBuilder s = new StringBuilder(input);
        readDictionary();

        System.out.println(String.format("Input: %s (%d length, %d blanks)\n" +
                                         "Running in %s mode\n" +
                                         "Outputting matches of %d length or greater\n" +
                                         "Outputting matches of pattern: %s\n" +
                                         "%s",
                                         input,
                                         input.length(),
                                         StringUtils.countMatches(input, "*"),
                                         parallel ? "parallel" : "sequential",
                                         minSize,
                                         pattern,
                                         StringUtils.repeat('*', PAD)));
        try (StatusReporter reporter = new StatusReporter()) {
            if (parallel) {
                // Generate a list of starting points that can be safely computed in parallel and produce all matches when collectively solved.
                // Starting points are:
                //     - Choose each character in the input and make it the first character. This character will not be touched during solving.
                //     - Choose each character in the input and delete it. Solve the remaining sequence.
                // Therefore, the max parallelism factor is twice the length of the input.
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
                    NUM_PROCESSED_TL.set(0L);
                    solve(startingPoint, startingPoint.length() == s.length() ? 1 : 0);
                    NUM_PROCESSED.addAndGet(NUM_PROCESSED_TL.get());
                });
            } else {
                NUM_PROCESSED_TL.set(0L);
                solve(s, 0);
                NUM_PROCESSED.addAndGet(NUM_PROCESSED_TL.get());
            }
        }

        System.out.println(String.format("%s\nFound %,d solutions for %s\nProcessed %,d",
                                         StringUtils.repeat('*', PAD),
                                         SOLUTIONS.size(),
                                         input,
                                         NUM_PROCESSED.get()));
    }
}
