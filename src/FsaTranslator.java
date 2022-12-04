import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


/**
 * Translates the given FSA to the regular expression.
 *
 * @author Vladimir Ryabenko
 * @version 1.0, 06.04.2022
 */
public class FsaTranslator {
    public static void main(String[] args) {
        try {
            new FsaTranslator().run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads, handles exceptions and transforms FSA to the regular expression if there are no exceptions.
     *
     * @throws IOException if errors occur during input or output
     */
    private void run() throws IOException {
        File file = new File("input.txt");
        Scanner scanner = new Scanner(file);
        FileWriter writer = new FileWriter("output.txt");

        try {
            Fsa fsa = fsaInput(scanner, writer);
            String regExp = KleeneAlgorithm(fsa);
            writer.write(regExp);
        } catch (RuntimeException ex) {
            writer.write(ex.getMessage());
        } catch (Exception ex) {
            writer.write("Error:\nE0: Input file is malformed");
        } finally {
            scanner.close();
            writer.close();
        }
    }

    /**
     * Builds the regular expression that corresponds to the given FSA.
     *
     * @param fsa the given FSA
     * @return a regular expression that corresponds to the given FSA
     */
    private String KleeneAlgorithm(Fsa fsa) {
        String[][] prevStage = new String[fsa.size][fsa.size];
        for (int i = 0; i < fsa.size; i++) {
            for (int j = 0; j < fsa.size; j++) {
                if (fsa.trans[i][j].isEmpty() && i != j)
                    prevStage[i][j] = "{}";
                else if (fsa.trans[i][j].isEmpty())
                    prevStage[i][j] = "eps";
                else {
                    prevStage[i][j] = "";
                    for (String s : fsa.trans[i][j]) {
                        prevStage[i][j] += s + "|";
                    }
                    if (i == j)
                        prevStage[i][j] += "eps";
                    else {
                        prevStage[i][j] = prevStage[i][j].substring(0, prevStage[i][j].length() - 1);
                    }
                }
            }
        }

        for (int k = 0; k < fsa.size; k++) {
            String[][] curStage = new String[fsa.size][fsa.size];
            for (int i = 0; i < fsa.size; i++) {
                for (int j = 0; j < fsa.size; j++) {
                    curStage[i][j] = "(" + prevStage[i][k] + ")" + "(" + prevStage[k][k] + ")*" +
                            "(" + prevStage[k][j] + ")|(" + prevStage[i][j] + ")";
                }
            }
            prevStage = curStage;
        }

        String regExp = "";
        int counter = 0;
        for (int i = 0; i < fsa.size; i++) {
            if (fsa.finiteStates.contains(i)) {
                if (counter != 0)
                    regExp += "|";
                regExp += prevStage[fsa.initState][i];
                counter++;
            }
        }
        if (counter == 0)
            regExp = "{}";
        return regExp;
    }

    /**
     * Reads and analyses the given FSA.
     *
     * @param scanner scanner from the "input.txt"
     * @param writer writer to the "output.txt"
     * @throws RuntimeException if errors are detected during validation
     * @throws IOException if errors occur during input or output
     */
    private Fsa fsaInput(Scanner scanner, FileWriter writer) throws RuntimeException, IOException {
        Fsa fsa = new Fsa();

        //Gets data from the file
        String line1 = scanner.nextLine();
        String line2 = scanner.nextLine();
        String line3 = scanner.nextLine();
        String line4 = scanner.nextLine();
        String line5 = scanner.nextLine();

        //Checks on E0
        if ((line1.split("=").length != 2 || line2.split("=").length != 2 ||
                line3.split("=").length != 2 || line4.split("=").length != 2
                || line5.split("=").length != 2) || (!line1.split("=")[0].equals("states")
                || !line2.split("=")[0].equals("alpha") || !line3.split("=")[0].equals("initial")
                || !line4.split("=")[0].equals("accepting") || !line5.split("=")[0].equals("trans"))) {
            throw new RuntimeException("Error:\nE0: Input file is malformed");
        }

        //Converts the first line read to a convenient format, checks on E0
        String str = line1.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE0: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        int i = 0;
        for (String s : str.split(",")) {
            if (!s.equals(""))
                fsa.states.put(s, i++);
            else
                throw new RuntimeException("Error:\nE0: Input file is malformed");
        }
        fsa.size = fsa.states.size();

        //Converts the second line read to a convenient format, checks on E0
        str = line2.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE0: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        for (String s : str.split(",")) {
            if (!s.equals(""))
                fsa.alphabet.add(s);
            else
                throw new RuntimeException("Error:\nE0: Input file is malformed");
        }

        //Converts the third line read to a convenient format, checks on E0, E1, E4
        str = line3.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE0: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        if (!str.equals("") && !fsa.states.containsKey(str))
            throw new RuntimeException("Error:\nE1: A state '" + str + "' is not in the set of states");
        if (str.equals(""))
            throw new RuntimeException("Error:\nE4: Initial state is not defined");
        fsa.initState = fsa.states.get(str);

        //Converts the fourth line read to a convenient format, checks on E0, E1
        str = line4.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE0: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        for (String s : str.split(",")) {
            if (!s.equals("") && !fsa.states.containsKey(s))
                throw new RuntimeException("Error:\nE1: A state '" + s + "' is not in the set of states");
            if (!s.equals(""))
                fsa.finiteStates.add(fsa.states.get(s));
        }

        fsa.outTrans = new HashSet[fsa.size];
        fsa.trans = new HashSet[fsa.size][fsa.size];
        for (i = 0; i < fsa.size; i++) {
            fsa.outTrans[i] = new HashSet<>();
            for (int j = 0; j < fsa.size; j++)
                fsa.trans[i][j] = new HashSet<>();
        }
        fsa.links = new boolean[fsa.size][fsa.size];

        //Converts the fifth line read to a convenient format, checks on E0, E1, E3, E5
        str = line5.split("=")[1];
        if (str.charAt(0) != '[' || str.charAt(str.length() - 1) != ']')
            throw new RuntimeException("Error:\nE0: Input file is malformed");
        str = str.substring(1, str.length() - 1);
        for (String s : str.split(",")) {
            String state1 = s.split(">")[0];
            String state2 = s.split(">")[2];
            String tran = s.split(">")[1];
            if (!fsa.states.containsKey(state1))
                throw new RuntimeException("Error:\nE1: A state '" + state1 + "' is not in the set of states");
            else if (!fsa.states.containsKey(state2))
                throw new RuntimeException("Error:\nE1: A state '" + state2 + "' is not in the set of states");
            else if (!fsa.alphabet.contains(tran))
                throw new RuntimeException("Error:\nE3: A transition '" + tran + "' is not represented in the alphabet");
            else if (fsa.outTrans[fsa.states.get(state1)].contains(tran))
                throw new RuntimeException("Error:\nE5: FSA is nondeterministic");
            fsa.outTrans[fsa.states.get(state1)].add(tran);
            fsa.trans[fsa.states.get(state1)][fsa.states.get(state2)].add(tran);
            fsa.links[fsa.states.get(state1)][fsa.states.get(state2)] = true;
            fsa.links[fsa.states.get(state2)][fsa.states.get(state1)] = true;
        }

        //Checks on E2
        if (isDisjoint(fsa))
            throw new RuntimeException("Error:\nE2: Some states are disjoint");

        return fsa;
    }

    /**
     * Checks if some states in the given FSA are disjoint.
     *
     * @param fsa the given FSA
     * @return true if some states in the given FSA are disjoint and false otherwise
     */
    private boolean isDisjoint(Fsa fsa) {
        ArrayDeque<Integer> next = new ArrayDeque<>();
        boolean[] visited = new boolean[fsa.states.size()];
        visited[0] = true;
        next.addLast(0);
        while (!next.isEmpty()) {
            int curState = next.removeFirst();
            for (int i = 0; i < fsa.states.size(); i++) {
                if (fsa.links[curState][i] && !visited[i]) {
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
     * Represents the FSA.
     */
    private class Fsa {
        HashMap<String, Integer> states;
        HashSet<String> alphabet;
        HashSet<Integer> finiteStates;
        Integer initState;
        HashSet<String>[][] trans;
        boolean[][] links;
        HashSet<String>[] outTrans;
        Integer size;

        public Fsa() {
            states = new HashMap<>();
            alphabet = new HashSet<>();
            finiteStates = new HashSet<>();
            size = 0;
        }
    }
}
