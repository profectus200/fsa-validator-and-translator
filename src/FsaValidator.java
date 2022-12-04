import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Validates the given FSA.
 *
 * @author Vladimir Ryabenko
 * @version 1.0, 19.02.2022
 */
public class FsaValidator {
    public static void main(String[] args) {
        try {
            new FsaValidator().run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens and closes the scanner and writer. Handle exceptions.
     *
     * @throws IOException if errors occur during input or output
     */
    private void run() throws IOException {
        File file = new File("fsa.txt");
        Scanner scanner = new Scanner(file);
        FileWriter writer = new FileWriter("result.txt");

        try {
            execute(scanner, writer);
        } catch (RuntimeException ex) {
            writer.write(ex.getMessage());
        } catch (Exception ex) {
            writer.write("Error:\nE5: Input file is malformed");
        } finally {
            scanner.close();
            writer.close();
        }
    }

    /**
     * Reads and analyses the given FSA.
     *
     * @param scanner scanner from the file
     * @param writer  writer to the file
     * @throws RuntimeException if errors are detected during validation
     * @throws IOException      if errors occur during input or output
     */
    private void execute(Scanner scanner, FileWriter writer) throws RuntimeException, IOException {
        HashMap<String, Integer> states = new HashMap<>();
        HashSet<String> alpha, finiteStates = new HashSet<>();
        String initState;
        String[][] trans;
        String[] warnings = new String[3];
        boolean[][] links;
        HashSet<String>[] outTrans;

        //Gets data from the file
        String line1 = scanner.nextLine();
        String line2 = scanner.nextLine();
        String line3 = scanner.nextLine();
        String line4 = scanner.nextLine();
        String line5 = scanner.nextLine();

        //Checks on E5
        if ((line1.split("=").length != 2 || line2.split("=").length != 2 ||
                line3.split("=").length != 2 || line4.split("=").length != 2
                || line5.split("=").length != 2) || (!line1.split("=")[0].equals("states")
                || !line2.split("=")[0].equals("alpha") || !line3.split("=")[0].equals("init.st")
                || !line4.split("=")[0].equals("fin.st") || !line5.split("=")[0].equals("trans"))) {
            throw new RuntimeException("Error:\nE5: Input file is malformed");
        }

        //Converts the first line read to a convenient format, checks on E5
        String str = line1.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE5: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        int i = 0;
        for (String s : str.split(",")) {
            states.put(s, i++);
        }

        //Converts the second line read to a convenient format, checks on E5
        str = line2.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE5: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        alpha = new HashSet<>(Arrays.asList(str.split(",")));

        //Converts the third line read to a convenient format, checks on E1, E4, E5
        str = line3.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE5: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        initState = str;
        if (!initState.equals("") && !states.containsKey(initState))
            throw new RuntimeException("Error:\nE1: A state '" + initState + "' is not in the set of states");
        if (initState.equals(""))
            throw new RuntimeException("Error:\nE4: Initial state is not defined");

        //Converts the fourth line read to a convenient format, checks on E1, E5, W1
        str = line4.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE5: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        for (String s : str.split(",")) {
            if (!s.equals("") && !states.containsKey(s))
                throw new RuntimeException("Error:\nE1: A state '" + s + "' is not in the set of states");
            if (!s.equals(""))
                finiteStates.add(s);
        }
        if (finiteStates.size() == 0)
            warnings[0] = "W1: Accepting state is not defined\n";


        outTrans = new HashSet[states.size()];
        for (i = 0; i < states.size(); i++)
            outTrans[i] = new HashSet<>();
        links = new boolean[states.size()][states.size()];
        trans = new String[states.size()][states.size()];

        //Converts the fifth line read to a convenient format, checks on E1, E3, E5, W3
        str = line5.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE5: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        for (String s : str.split(",")) {
            String state1 = s.split(">")[0];
            String state2 = s.split(">")[2];
            String tran = s.split(">")[1];
            if (!states.containsKey(state1))
                throw new RuntimeException("Error:\nE1: A state '" + state1 + "' is not in the set of states");
            else if (!states.containsKey(state2))
                throw new RuntimeException("Error:\nE1: A state '" + state2 + "' is not in the set of states");
            else if (!alpha.contains(tran))
                throw new RuntimeException("Error:\nE3: A transition '" + tran + "' is not represented in the alphabet");
            else if (outTrans[states.get(state1)].contains(tran))
                warnings[2] = "W3: FSA is nondeterministic\n";
            outTrans[states.get(state1)].add(tran);
            trans[states.get(state1)][states.get(state2)] = tran;
            links[states.get(state1)][states.get(state2)] = true;
            links[states.get(state2)][states.get(state1)] = true;
        }

        //Checks on E2, W2 and makes report about FSA completeness
        if (isDisjoint(links, states.size()))
            throw new RuntimeException("Error:\nE2: Some states are disjoint");
        String report = isComplete(outTrans, states.size(), alpha.size());
        if (!isReachable(trans, states.size(), states.get(initState)))
            warnings[1] = "W2: Some states are not reachable from the initial state\n";

        //Writes the validation results if no errors are detected
        writer.write(report);
        report = "";
        for (String w : warnings) {
            if (w != null) report += w;
        }
        if (report.length() != 0) {
            writer.write("Warning:\n" + report);
        }
    }

    /**
     * Checks if the given FSA is complete or not.
     *
     * @param outTrans   set storing all transitions from each particular state
     * @param statesSize number of states in the given FSA
     * @param alphaSize  size of the alphabet
     * @return true if the given FSA is complete, false otherwise
     */
    private String isComplete(HashSet<String>[] outTrans, int statesSize, int alphaSize) {
        boolean comp = true;
        for (int i = 0; i < statesSize; i++) {
            if (outTrans[i].size() != alphaSize) {
                comp = false;
                break;
            }
        }
        return comp ? "FSA is complete\n" : "FSA is incomplete\n";
    }

    /**
     * Checks if there are any disjoint states.
     *
     * @param links      array storing whether any pairs of states are connected
     * @param statesSize number of states in the given FSA
     * @return true if some states are disjoint, false otherwise
     */
    private boolean isDisjoint(boolean[][] links, int statesSize) {
        ArrayDeque<Integer> next = new ArrayDeque<>();
        boolean[] visited = new boolean[statesSize];
        visited[0] = true;
        next.addLast(0);
        while (!next.isEmpty()) {
            int curState = next.removeFirst();
            for (int i = 0; i < statesSize; i++) {
                if (links[curState][i] && !visited[i]) {
                    visited[i] = true;
                    next.addLast(i);
                }
            }
        }
        for (boolean v : visited) {
            if (!v) return true;
        }
        return false;
    }

    /**
     * Checks if there are any unreachable states from the initial state.
     *
     * @param trans      array storing all transitions in the given FSA
     * @param statesSize number of states in the given FSA
     * @param initState  initial state
     * @return true if all states are reachable from the initial state, false otherwise
     */
    private boolean isReachable(String[][] trans, int statesSize, int initState) {
        ArrayDeque<Integer> next = new ArrayDeque<>();
        boolean[] visited = new boolean[statesSize];
        visited[initState] = true;
        next.addLast(initState);
        while (!next.isEmpty()) {
            int curState = next.removeFirst();
            for (int i = 0; i < statesSize; i++) {
                if (trans[curState][i] != null && !visited[i]) {
                    visited[i] = true;
                    next.addLast(i);
                }
            }
        }
        for (boolean v : visited) {
            if (!v) return false;
        }
        return true;
    }

}
