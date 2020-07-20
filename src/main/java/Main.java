import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {
    private static void printHelp(Options ops) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar ScrabbleSolver.java",
                            "Solve scrabble sequences\n\n",
                            ops,
                            "",
                            true);
    }

    private static String parseOptions(String[] args, Solver.Builder solverBuilder) throws ParseException {
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

        if (cmd.hasOption("s")) {
            solverBuilder.withParallel(false);
        }

        if (cmd.hasOption("n")) {
            solverBuilder.withMinCharaters(Integer.parseInt(cmd.getOptionValue("n")));
        }

        if (cmd.hasOption("r")) {
            solverBuilder.withRegex(cmd.getOptionValue("r"));
        }

        if (cmd.hasOption("i")) {
            return cmd.getOptionValue("i");
        } else {
            throw new RuntimeException("The --input option is required" + "\n" + "Try --help");
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        Solver.Builder solverBuilder = new Solver.Builder();
        String input = parseOptions(args, solverBuilder);

        solverBuilder.build().solve(input);
    }
}
