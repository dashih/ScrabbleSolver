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
import java.util.regex.Matcher;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
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
    private static Pattern pattern = Pattern.compile(".*");

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

        // Check if match and output if match is unique.
        if (FOUND.putIfAbsent(str, true) == null && DICTIONARY.contains(str) && pattern.matcher(str).matches()) {
            System.out.println(str);
        }

        // Select each character in turn and swap it to the start of this iteration level.
        // Solve the remainder of the sequence.
        for (int i = idx; i < s.length(); i++) {
            swap(s, idx, i);
            solve(s, idx + 1);
            swap(s, idx, i);
        }

        // Delete each character in turn and solve the remainder.
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

        System.out.println(String.format("Running in %s mode. Outputting %d characters or greater.\n********************************",
                                         parallel ? "parallel" : "sequential",
                                         minSize));
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
                solve(startingPoint, startingPoint.length() == s.length() ? 1 : 0);
            });
        } else {
            solve(s, 0);
        }

        System.out.println(String.format("********************************\nFound %s words for %s", FOUND.size(), input));
    }
}
