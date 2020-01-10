package search;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 *  TextSearcher implements a search interface to an underlying text file 
 *  consisting of the single method {@link #search(String, int)}. 
 */
class TextSearcher {

	private String fileContents;
	private double[] p_pows = p_pow_array();
	//wordMap = <hashKey_of_word, list of word occurrence order>
	private HashMap<Double, ArrayList<Integer>> wordMap = new HashMap<>();
	// List is used as a tuple [startPos, endPos]
	private ArrayList<List<Integer>> wordStartAndEndPosition = new ArrayList<>();
	// next word count
	private int nextWordCount = 0;

	/**
	 * Initializes the text searcher with the contents of a text file.
	 * The current implementation just reads the contents into a string 
	 * and passes them to #init().  You may modify this implementation if you need to.
	 * 
	 * @param f Input file.
	 * @throws IOException if can't read file
	 */
	TextSearcher(File f) throws IOException {
		try (FileReader r = new FileReader(f)) {
			StringWriter w = new StringWriter();
			char[] buf = new char[4096];
			int readCount;
			while ((readCount = r.read(buf)) > 0) {
				w.write(buf, 0, readCount);
			}
			init(w.toString());
		}
	}
	
	/**
	 *  Initializes any internal data structures that are needed for
	 *  this class to implement search efficiently.
	 */
	private void init(String fileContents) {
		this.fileContents = fileContents;
		// word start position in file
		int startPos = 0;

		String wordRegex = "[a-zA-Z0-9']+[a-zA-Z0-9]*";
		TextTokenizer textTokenizer = new TextTokenizer(fileContents, wordRegex);
		while (textTokenizer.hasNext()) {
			String word = textTokenizer.next();
			if (textTokenizer.isWord(word)) {
				double hashKey = compute_hash(word);
				if (wordMap.containsKey(hashKey)) {
					ArrayList<Integer> list = wordMap.get(hashKey);
					list.add(nextWordCount++);
				}
				else{
					ArrayList<Integer> list = new ArrayList<>();
					list.add(nextWordCount++);
					wordMap.put(hashKey, list);
				}
				// keep start and end position of word in the file
				wordStartAndEndPosition.add(Arrays.asList(startPos, startPos += word.length()));
			}
			else {
				//add the length of the punctuation
				startPos += word.length();
			}
		}
	}

	
	/**
	 * 
	 * @param queryWord The word to search for in the file contents.
	 * @param contextWords The number of words of context to provide on
	 *                     each side of the query word.
	 * @return One context string for each time the query word appears in the file.
	 */
	String[] search(String queryWord, int contextWords) {
		ArrayList<String> returnStrings = new ArrayList<>();
		// look up word using hashKey
		ArrayList<Integer> wordOrders = wordMap.get(compute_hash(queryWord));
		// no match
		if (wordOrders == null) return new String[0];

		// proceed
		for (int order:wordOrders){
			// it is not likely that words share the same hashkey but it can happen,
			// so check before returning
			if (queryWord.toLowerCase().equalsIgnoreCase(getStringByPositionInFile(order, order))){
				int startOrder = Math.max(order - contextWords, 0);
				int endOrder = (order + contextWords > nextWordCount) ? nextWordCount -1  : order + contextWords;
				returnStrings.add(getStringByPositionInFile(startOrder, endOrder));
			}
		}
		return returnStrings.toArray(new String[0]);
	}

	private String getStringByPositionInFile(int startOrder, int endOrder){
		int startPosition = wordStartAndEndPosition.get(startOrder).get(0);
		int endPosition = wordStartAndEndPosition.get(endOrder).get(1);
		if (endOrder == nextWordCount - 1){
			// if endOrder is the last word, go all the way to the end.
			return fileContents.substring(startPosition);
		}
		return fileContents.substring(startPosition, endPosition);
	}

	/*
	 * pre-calculate p_pow to speed up calculation of hashkey
	 */
	private double[] p_pow_array(){
		int p = 53;
		double m = 1e9 + 9;
		double p_pow = 1;

		double[] p_pows = new double[20];
		p_pows[0] = p_pow;
		for(int i = 1; i <= 19; i++){
			p_pows[i] = (p_pows[i-1] * p) % m;
		}
		return p_pows;
	}

	// hash function based on https://cp-algorithms.com/string/string-hashing.html
	private double compute_hash(String s) {
		double m = 1e9 + 9;
		double hash_value = 0;
		int index = 0;
		for (char c : s.toLowerCase().toCharArray()) {
			hash_value = (hash_value + (c - 'a' + 1) * p_pows[index]) % m;
		}
		return hash_value;
	}
}


