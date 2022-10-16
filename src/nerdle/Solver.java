package nerdle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Solver {

	public static final int GAME_SIZE = 8;
	public static final char[] POSSIBLE_MOVES = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'=', '+', '-', '*', '/'
	};

	public static final char[] POSSIBLE_INTEGERS = new char[] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};

	public static final char[] POSSIBLE_INTEGERS_EXLCUDING_ZERO = new char[] {
			'1', '2', '3', '4', '5', '6', '7', '8', '9'
	};

	public static final char[] POSSIBLE_OPERATORS = new char[] {
			'=', '+', '-', '*', '/'
	};

	private enum MODE {
		GENERATE, PLAY, PLAY_OLD, TEST, STATS, COMPUTER;
	}

	private static Scanner scanner;
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		MODE mode = MODE.PLAY;
		final String listFilename = "list-" + GAME_SIZE + ".ser";
		final String mapFilename = "map-" + GAME_SIZE + ".ser";

		scanner = new Scanner(System.in);
		System.out.println("Please enter what mode you wish to enter: " + 
				Arrays.stream(MODE.values()).map(Enum::toString).collect(Collectors.joining(",")));

		mode = MODE.valueOf(scanner.next());

		if(mode == MODE.GENERATE) {
			List<String> solutions = generateSolutions();
			int[][] map = generateMap(solutions);
			System.out.println(solutions.size());

			FileOutputStream fos = new FileOutputStream(mapFilename);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(map);
			oos.close();

			fos = new FileOutputStream(listFilename);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(solutions);
			oos.close();
		} else if(mode == MODE.PLAY || mode == MODE.PLAY_OLD){
			File f = new File(mapFilename);
			if(!f.exists()) {
				System.out.println("First run the generate mode.");
				return;
			}

			FileInputStream fis = new FileInputStream(mapFilename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			int[][] map = (int[][]) ois.readObject();
			ois.close();

			fis = new FileInputStream(listFilename);
			ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			List<String> solutions = (List<String>) ois.readObject();
			ois.close();

			play(map, solutions, mode);
		} else if(mode == MODE.TEST) {
			// who needs tests
			System.out.println(generateDiff("13+59=72", "100-1=99")); // PPGGBGBB
		} else if(mode == MODE.STATS) {
			FileInputStream fis = new FileInputStream(listFilename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			List<String> solutions = (List<String>) ois.readObject();
			ois.close();

			List<Map<Character, Integer>> maps = new ArrayList<>(GAME_SIZE);
			IntStream.range(0, GAME_SIZE).forEach(i -> maps.add(new HashMap<>()));

			for(String s: solutions) {
				char[] c = s.toCharArray();
				for(int i = 0; i < c.length; i++) {
					maps.get(i).compute(c[i], (k, v) -> v == null ? 1 : v + 1);
				}
			}

			for(Map<Character, Integer> map: maps) {
				System.out.println(map);
			}
		} else if(mode == MODE.COMPUTER) {
			File f = new File(mapFilename);
			if(!f.exists()) {
				System.out.println("First run the generate mode.");
				return;
			}

			FileInputStream fis = new FileInputStream(mapFilename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			int[][] map = (int[][]) ois.readObject();
			ois.close();

			fis = new FileInputStream(listFilename);
			ois = new ObjectInputStream(fis);
			@SuppressWarnings("unchecked")
			List<String> solutions = (List<String>) ois.readObject();
			ois.close();

			computer(map, solutions);
		}

		scanner.close();
	}

	public static void computer(int[][] diffs, List<String> solutions) {
		Set<Integer> solutionSet = new HashSet<>();
		for(int i = 0; i < solutions.size(); i++) solutionSet.add(i);
		String answer = solutions.get(ThreadLocalRandom.current().nextInt(0, solutions.size()));

		while(true) {
			System.out.println("Possible answers: " + solutionSet.size());
			String optimal = guess(diffs, solutions, solutionSet);
			System.out.println("Optimal: " + optimal);

			if(solutionSet.size() == 0) break;

			System.out.println("Please input your guess:");
			String guess = scanner.next();
			if(guess.equals(answer)) {
				System.out.println("You guess the correct answer!");
				break;
			}
			
			int[] d = genDiff(guess, answer);
			System.out.println(generateStringDiff(d) + " Hint");
			
			int hintTransformed = calcDiff(d);
			if(hintTransformed == Math.pow(3, GAME_SIZE) - 1) break;

			prune(
					diffs, 
					solutions, 
					solutionSet, 
					guess,
					hintTransformed
					);
		}
	}

	public static void play(int[][] diffs, List<String> solutions, MODE mode) {
		Set<Integer> solutionSet = new HashSet<>();
		List<Solution> sols = new ArrayList<>();
		for(int i = 0; i < solutions.size(); i++) solutionSet.add(i);
		if(mode == MODE.PLAY_OLD) {
			for(String s: solutions) {
				sols.add(new Solution(s.toCharArray()));
			}
		}

		while(true) {
			System.out.println("Possible answers: " + solutionSet.size());
			String optimal = mode == MODE.PLAY ? guess(diffs, solutions, solutionSet) 
					: guess(sols, solutionSet);
			System.out.println("Optimal: " + optimal);

			if(solutionSet.size() == 0) break;

			System.out.println("Please input your guess:");
			String guess = scanner.next();
			System.out.println("Please input your hint (G | P | B):");
			String hint = scanner.next();

			int hintTransformed = transform(hint);
			if(hintTransformed == Math.pow(3, GAME_SIZE) - 1) break;

			prune(
					diffs, 
					solutions, 
					solutionSet, 
					guess.isBlank() ? optimal : guess,
							hintTransformed
					);
		}
	}

	public static int transform(String hint) {
		if(hint.length() != GAME_SIZE) {
			throw new RuntimeException("Guess should be equal to " + GAME_SIZE);
		}

		int ret = 0;
		int pos = 0;
		for(char c: hint.toCharArray()) {
			if(c == 'G') {
				ret += Math.pow(3, hint.length() - pos - 1) * GREEN;
			} else if(c == 'P') {
				ret += Math.pow(3, hint.length() - pos - 1) * PURPLE;
			} else if(c == 'B') {
				ret += Math.pow(3, hint.length() - pos - 1) * BLACK;
			} else {
				throw new RuntimeException("Hint should only contain G (green), P (purple) or B (black)");
			}
			pos++;
		}

		return ret;
	}

	public static void prune(int[][] diffs, List<String> solutions, Set<Integer> solutionSet,
			String guess, int hint) {
		int guessIndex = -1;
		// for(Integer i: solutionSet) {
		for(int i = 0; i < solutions.size(); i++) {
			if(solutions.get(i).equals(guess)) {
				guessIndex = i;
				break;
			}
		}

		Set<Integer> toRemove = new HashSet<>();
		for(Integer i: solutionSet) {
			if(diffs[guessIndex][i] != hint) {
				toRemove.add(i);
			}
		}

		solutionSet.removeAll(toRemove);
	}

	public static int[][] generateMap(List<String> solutions){
		int n = solutions.size();
		int[][] ret = new int[n][n];
		for(int i = 0; i < n; i++) {
			for(int j = 0; j < n; j++) {
				ret[i][j] = generateDiff(solutions.get(i), solutions.get(j));
			}
		}
		return ret;
	}

	private static int GREEN = 2;
	private static int PURPLE = 1;
	private static int BLACK = 0;
	public static int generateDiff(String x, String other) {
		int[] diffs = genDiff(x, other);
		return calcDiff(diffs);
	}

	public static int calcDiff(int[] diffs) {
		int ret = 0;
		for(int i = 0; i < diffs.length; i++) {
			ret += Math.pow(3, diffs.length - i - 1) * diffs[i];
		}
		return ret;
	}

	public static String generateStringDiff(int[] diffs) {
		StringBuilder sb = new StringBuilder(diffs.length);
		for(int i: diffs) {
			if(i == GREEN) sb.append('G');
			else if(i == PURPLE) sb.append('P');
			else sb.append('B');
		}
		return sb.toString();
	}

	public static int[] genDiff(String x, String other) {
		int[] diffs = new int[GAME_SIZE];
		Arrays.fill(diffs, BLACK);

		// green
		char[] xC = x.toCharArray();
		char[] otherC = other.toCharArray();
		for(int i = 0; i < xC.length; i++) {
			if(xC[i] == otherC[i]) {
				diffs[i] = GREEN;
				otherC[i] = 0;
			}
		}

		// purple
		for(int i = 0; i < xC.length; i++) {
			if(diffs[i] == GREEN) continue;
			for(int j = 0; j < otherC.length; j++) {
				if(xC[i] == otherC[j]) {
					diffs[i] = PURPLE;
					otherC[j] = 0;
				}
			}
		}

		return diffs;
	}

	public static String guess(int[][] diffs, List<String> solutions, Set<Integer> solutionSet) {
		if(solutionSet == null || solutionSet.size() == 0) return null;
		if(solutionSet.size() == 1) return solutions.get(solutionSet.iterator().next());

		int n = solutionSet.size();
		int optimal = -1;
		double best = -1;

		// for(int i: solutionSet) {
		for(int i = 0; i < solutions.size(); i++) {
			int[] buckets = new int[(int)Math.pow(3, GAME_SIZE)];
			for(int j: solutionSet) {
				buckets[diffs[i][j]]++;
			}

			double entropy = 0;

			for(int b: buckets) {
				if(b == 0) continue;
				entropy += - (Math.log((double) b / n) / Math.log(2) )
						* ((double) b / n);
			}

			if(entropy > best || (entropy == best && solutionSet.contains(i))) {
				best = entropy;
				optimal = i;
			}
		}

		return solutions.get(optimal);
	}

	public static String guess(List<Solution> solutions, Set<Integer> solutionSet) {
		if(solutionSet == null || solutionSet.size() == 0) return null;
		if(solutionSet.size() == 1) return solutions.get(solutionSet.iterator().next()).getChars();

		Solution optimal = solutions.get(solutionSet.iterator().next());

		long[][] distribution = new long[GAME_SIZE][POSSIBLE_MOVES.length];

		for(int i: solutionSet) solutions.get(i).incrementDistribution(distribution);

		//		for(Solution solution: solutions) solution.incrementDistribution(distribution);
		//
		double maxEntropy = optimal.entropy(distribution, solutions.size());
		//		for(Solution solution: solutions) {
		//			double entropy = solution.entropy(distribution, solutions.size());
		//			if(entropy > maxEntropy) {
		//				maxEntropy = entropy;
		//				optimal = solution;
		//			}
		//		}

		for(int i: solutionSet) {
			double entropy = solutions.get(i).entropy(distribution, solutions.size());
			if(entropy > maxEntropy) {
				maxEntropy = entropy;
				optimal = solutions.get(i);
			}
		}

		return optimal.getChars();
	}

	public static List<String> generateSolutions(){
		List<String> solutions = new ArrayList<>();
		generateSolutionsHelper(solutions, new char[GAME_SIZE], 0, true, false);
		return solutions;
	}

	public static void generateSolutionsHelper(
			List<String> solutions,
			char[] curr,
			int index,
			boolean prevWasOperator,
			boolean afterEquals) {
		if(index == curr.length) {
			if(!prevWasOperator && Solution.valid(curr)) solutions.add(new String(curr));
			return;
		}

		if(afterEquals && prevWasOperator) {
			for(char c: POSSIBLE_INTEGERS_EXLCUDING_ZERO) {
				curr[index] = c;
				generateSolutionsHelper(solutions, curr, index + 1, false, afterEquals);
			}
		} else if(afterEquals && !prevWasOperator) {
			for(char c: POSSIBLE_INTEGERS) {
				curr[index] = c;
				generateSolutionsHelper(solutions, curr, index + 1, false, afterEquals);
			}
		} else if(prevWasOperator) {
			for(char c: POSSIBLE_INTEGERS_EXLCUDING_ZERO) {
				curr[index] = c;
				generateSolutionsHelper(solutions, curr, index + 1, false, afterEquals);
			}
		} else {
			for(char c: POSSIBLE_INTEGERS) {
				curr[index] = c;
				generateSolutionsHelper(solutions, curr, index + 1, false, afterEquals);
			}

			for(char c: POSSIBLE_OPERATORS) {
				curr[index] = c;
				generateSolutionsHelper(solutions, curr, index + 1, true, c == '=');
			}
		}
	}

}

class Solution {

	public char[] chars;

	public Solution(char[] curr) {
		this.chars = Arrays.copyOf(curr, curr.length);
	}

	public String getChars() {
		return new String(chars);
	}

	public double entropy(long[][] distribution, double solutionSize) {
		double entropy = 0;
		for(int i = 0; i < chars.length; i++) {
			for(int j = 0; j < Solver.POSSIBLE_MOVES.length; j++) {
				if(chars[i] == Solver.POSSIBLE_MOVES[j]) {
					entropy += - (Math.log(distribution[i][j] / solutionSize) / Math.log(2) )
							* (distribution[i][j] / solutionSize)
							- (Math.log((solutionSize - distribution[i][j]) / solutionSize) / Math.log(2) )
							* (solutionSize - distribution[i][j]) / solutionSize;

					break;
				}
			}
		}

		return entropy;
	}

	public void incrementDistribution(long[][] distribution) {
		for(int i = 0; i < chars.length; i++) {
			for(int j = 0; j < Solver.POSSIBLE_MOVES.length; j++) {
				if(chars[i] == Solver.POSSIBLE_MOVES[j]) {
					distribution[i][j]++;
					break;
				}
			}
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(chars);
	}

	public static boolean valid(char[] curr) {
		Stack<Double> s = new Stack<>();
		double sum = 0;
		double c = 0;
		char operator = '+';

		for(int i = 0; i < curr.length; i++) {
			if(Character.isDigit(curr[i])) c = c * 10 + curr[i] - '0';
			else {
				if(operator == '+') {
					s.push(c);
				} else if(operator == '-') {
					s.push(-c);
				} else if(operator == '*') {
					s.push(s.pop() * c);
				} else if(operator == '/') {
					if(c == 0) return false;
					s.push(s.pop() / c);
				}
				c = 0;
				operator = curr[i];
			}
		}

		while(!s.isEmpty()) sum += s.pop();
		return operator == '=' && sum == c;
	}
}
