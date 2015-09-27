/**
 * Corpus.java
 * <p/>
 * Written by: Adam Goforth
 * Started on: Dec 4, 2005
 */
package jchampollion;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;

import java.io.*;
import java.util.*;

/**
 * This class represents one or more text files that make up a corpus.
 *
 * @author Adam Goforth
 *
 */
public class Corpus {

    String sourceFilename;
    String targetFilename;

    /**
     * Constructor for the Corpus.
     * Currently only supports a file.
     * TODO: Support a directory with multiple files
     *
     * @param source_ The text file that contains the source language corpus.
     * @param target_ The text file that contains the target language corpus.
     */
    public Corpus(String source_, String target_) {
        sourceFilename = source_;
        targetFilename = target_;
    } // End constructor

    /**
     * Takes a file or directory, and adds its contents to a Lucene index.
     *
     * @param writer
     *            The IndexWriter already pointing to the index.
     * @param file
     *            The file or directory to be added.
     * @throws IOException
     *             Exception if there is a problem reading from or writing to
     *             the index.
     */
    public static void indexDocs(IndexWriter writer, File file)
            throws IOException {
        // do not try to index files that cannot be read
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                // an IO error could occur
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        indexDocs(writer, new File(file, files[i]));
                    }
                }
            } else {
                System.out.println("adding " + file);
                try {
                    FileInputStream is = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                    if (reader != null) {

                        String record = null;
                        int recCount = 0;

                        try {
                            while ((record = reader.readLine()) != null) {
                                recCount++;
                                writer.addDocument(createDocumentFromSentence(recCount, record));
                            }
                        } catch (IOException e) {
                            // Catch IO errors from FileInputStream
                            System.out.println("Error reading file: " + e.getMessage());
                        }
                        System.out.println("Source Lines Indexed: " + recCount);
                    }


                }
                // at least on windows, some temporary files raise this
                // exception with an "access denied" message
                // checking if the file can be read doesn't help
                catch (FileNotFoundException fnfe) {
                    ;
                }
            }
        }
    }

    /**
     * Takes a sentence and creates a Lucene document from it.
     *
     * @param sNum        The number of the sentence in the corpus.
     * @param sentence    The sentence itself.
     * @return A Lucene Document containing the sentence and number.
     */
    public static Document createDocumentFromSentence(int sNum, String sentence) {
        // make a new, empty document
        Document doc = new Document();

        //doc.add(Field.Text("snum", "" + sNum));
        doc.add(new Field("snum", "" + sNum, true, true, false));
        doc.add(Field.Text("contents", sentence));

        return doc;
    }

    /**
     * Rebuild the indices
     *
     */
    public void buildIndices() {
        buildIndex("source");
        buildIndex("target");
    }

    /**
     * Adds the a corpus to the index
     *
     * @param    source    The source {source,target} that should be added.
     */
    public void buildIndex(String source) {
        Date start = new Date();
        try {
            IndexWriter writer = new IndexWriter(source + "Index", new SimpleAnalyzer(), true);
            if (source.equals("source")) {
                indexDocs(writer, new File(sourceFilename));
            } else if (source.equals("target")) {
                indexDocs(writer, new File(targetFilename));
            }

            writer.optimize();
            writer.close();

            Date end = new Date();

            System.out.print(end.getTime() - start.getTime());
            System.out.println(" total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }
    }

    /**
     * getSentencesContaining returns a Vector of Integers containing the
     * numbers of the sentences that contain the given words in the source
     * language corpus.
     *
     * @param    words_    The words to be found
     * @param    source    The source {source, target} to be searched
     * @return A Vector of the sentence numbers
     */
    public Vector getSentencesContaining(String words_, String source) {
        Vector sentenceNums = new Vector();

        words_ = requireAll(words_);

        try {
            Searcher searcher = new IndexSearcher("sourceIndex");
            Analyzer analyzer = new SimpleAnalyzer();

            Query query = QueryParser.parse(words_, "contents", analyzer);
            Hits hits = searcher.search(query);

            // Add the numbers of all the hits to the Vector
            for (int i = 0; i < hits.length(); i++) {
                Document sentence = hits.doc(i);
                sentenceNums.add(sentence.get("snum"));
                //DEBUG System.out.println(sentence.get("snum") + ": " + sentence.get("contents"));
            }
        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }

        return sentenceNums;
    }

    /**
     * numSentencesContaining returns the number of sentences containing the
     * given words.
     *
     * @param words_    The words to be found
     * @param source    The source {source, target} to be searched.
     * @return The number of sentences containing the words
     */
    public int numSentencesContaining(String words_, String source) {
        int num = 0;

        words_ = requireAll(words_);
        //DEBUG System.out.println("Finding hits for " + words_);

        try {
            Searcher searcher = new IndexSearcher(source + "Index");
            Analyzer analyzer = new SimpleAnalyzer();

            Query query = QueryParser.parse(words_, "contents", analyzer);
            Hits hits = searcher.search(query);

            num = hits.length();
        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }
        return num;
    }


    /**
     * Return the contents of a sentence, referenced by its number.
     *
     * @param num        The number of the sentence to be returned.
     * @param source    The index {source,target} to be searched.
     * @return A String with the sentence in it.
     */
    public String getSentence(String num, String source) {
        String s = "";

        try {
            Searcher searcher = new IndexSearcher(source + "Index");

            Query query = new TermQuery(new Term("snum", num));
            Hits hits = searcher.search(query);

            //System.out.println("Searching: for \'" + num + "\' " + query.toString("snum"));
            //System.out.println("Hits for " + num + ": " + hits.length());

            for (int i = 0; i < hits.length(); i++) {
                Document sentence = hits.doc(i);
                //System.out.println(sentence);
                s = sentence.get("contents");
            }
            //Document sentence = hits.doc(0);
            //s = sentence.get("content");

        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }

        return s;
    }

    /**
     * dice returns the Dice value, a double between 0 and 1, describing the
     * strength of the relationship between X and Y. Both X and Y are one or
     * more words. Separate multiple words by spaces. Occurrances of X will be
     * counted in the source corpus, and occurrances of Y in the target corpus.
     *
     * @param X
     *            One or more words in the corpus.
     * @param Y
     *            One or more words in the corpus.
     * @return A double between 0 and 1 describing the strength of the
     *         relationship between X and Y. The closer to 1 the value is, the
     *         more strongly related X and Y are.
     */
    public double dice(String X, String Y) {
        if (X.equals("") || Y.equals("")) {
            return 0;
        }

        double diceVal = 0;

        double numXAndY = countIntersections(X, Y);

        double numX = numSentencesContaining(X, "source");
        double numY = numSentencesContaining(Y, "target");

        diceVal = (2 * numXAndY) / (numX + numY);

        return diceVal;
    }

    /**
     * Counts the intersection between the sentences containing S in the source
     * corpus and the sentences containing T in the target corpus.
     *
     * @param S
     *            The words in the source corpus, separated by spaces.
     * @param T
     *            The words in the target corpus, separated by spaces.
     * @return The number of sentences containing both all of the words in S and
     *         all of the words in T.
     */
    public int countIntersections(String S, String T) {
        int retNum = 0;

        // Require all terms
        S = requireAll(S);
        T = requireAll(T);

        try {
            // Get all sentences for the source terms
            Searcher ssearcher = new IndexSearcher("sourceIndex");
            Analyzer sanalyzer = new SimpleAnalyzer();

            Query squery = QueryParser.parse(S, "contents", sanalyzer);
            Hits sHits = ssearcher.search(squery, new Sort("snum"));

            // Get all sentences for the target terms
            Searcher tsearcher = new IndexSearcher("targetIndex");
            Analyzer tanalyzer = new SimpleAnalyzer();

            Query tquery = QueryParser.parse(T, "contents", tanalyzer);
            Hits tHits = tsearcher.search(tquery, new Sort("snum"));

            int sCount = 0;
            int tCount = 0;
            // Compare the sentences, and count how many match
            while (sCount < sHits.length() && tCount < tHits.length()) {
                Document sSentence = sHits.doc(sCount);
                int sSentNum = Integer.valueOf(sSentence.get("snum")).intValue();

                Document tSentence = tHits.doc(tCount);
                int tSentNum = Integer.valueOf(tSentence.get("snum")).intValue();

                //DEBUG System.out.println("s " + sSentNum + "\tt " + tSentNum);
                if (sSentNum == tSentNum) {
                    retNum++;
                    sCount++;
                    tCount++;
                } else if (sSentNum > tSentNum) {
                    tCount++;
                } else if (sSentNum < tSentNum) {
                    sCount++;
                }
            }

        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }

        return retNum;
    }

    /**
     * Get the words in the target corpus (in descending order by frequency) in
     * the sentences aligned with the sentences in the source corpus containing
     * all of the given words.
     * Used in Step 1 of the algorithm in the paper.
     *
     * @param words
     *            The words to search for in the source corpus.
     * @return A Vector of WordFrequency objects in descending order by
     *         frequency.
     */
    public Vector getRelatedWordsByFrequency(String words) {
        Vector wordsVector = new Vector();

        Vector sentences = getSentencesContaining(words, "source");

        Map wordsMap = new HashMap();
        final Integer ONE = new Integer(1);

        // Go through all the related sentences in the target corpus and add
        // their words to the Map
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = getSentence((String) sentences.get(i), "target");

            // Split words
            StringTokenizer tokenizer = new StringTokenizer(sentence, " ");

            // Go through the current sentence and add all words
            while (tokenizer.hasMoreTokens()) {
                // Get word
                String key = tokenizer.nextToken();

                if (!key.matches("(\\.|\\!|\\?|\\,|\\;|\\:|\\-|\\(|\\)|\\\"|\\%|\\#)")) {
                    Integer frequency = (Integer) wordsMap.get(key.toLowerCase());
                    if (frequency == null) {
                        frequency = ONE;
                    } else {
                        int value = frequency.intValue();
                        frequency = new Integer(value + 1);
                    }
                    wordsMap.put(key.toLowerCase(), frequency);
                }
            }
        } // End for

        // Sort the Map, then add the parts to the wordsVector in descending
        // order by frequency
        ArrayList entries = new ArrayList(wordsMap.entrySet());
        Collections.sort(entries, new Comparator() {
            public int compare(Object o1, Object o2) {
                Map.Entry e1 = (Map.Entry) o1;
                Map.Entry e2 = (Map.Entry) o2;
                Comparable v1 = (Comparable) e1.getValue();
                Comparable v2 = (Comparable) e2.getValue();
                return v2.compareTo(v1);
            }
        });
        for (Iterator i = entries.iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            //System.out.println("Word: " + e.getKey() + "\t\tFreq: " + e.getValue());
            wordsVector.add(new WordFrequency((String) e.getKey(), ((Integer) e.getValue()).intValue()));
        }

        return wordsVector;
    }

    /**
     * getRelatedCandidateWords gets a set of candidate translation words from the target corpus for a given set of words.  This is used in Step 1 of the algorithm, and is implemented using the optimization described in Section 5.1 of the paper, where a sorted list is analyzed from most to least frequent, stopping after a word fails to meet the candidate criteria.
     * @param words_    The words in the source corpus for which candidate translations should be found.
     * @param Tf        The variable Tf, the required minimum frequency (of sentences containing it) for a group of words to be a candidate (within the corpus subset related to the source sentences containing the given words).
     * @param Td        The variable Td, the minimum dice threshold for a group of word to be considered a candidate.
     * @return A Vector of Strings, one word each.
     */
    public Vector getRelatedCandidateWords(String words_, int Tf, double Td) {
        Vector wordsVector = new Vector();

        // Return if the words_ are not in the source corpus
        if (numSentencesContaining(words_, "source") == 0) {
            return wordsVector;
        }

        Vector relatedWords = getRelatedWordsByFrequency(words_);

        int i = 0;
        WordFrequency wf = (WordFrequency) relatedWords.get(i);
        double wordsFreq = numSentencesContaining(words_, "source");
        double diceUB = (2 * wf.getFreq()) / (wordsFreq + wf.getFreq());

		/* Go down the list until
		 * a) The word's local frequency is lower than the threshold Tf.
		 * b) The word's local frequency is so low that we know it would be
		 * impossible for the Dice coefficient between it and the source
		 * collocation to be higher than the threshold Td.
		 */
        while (wf.getFreq() > Tf && diceUB > Td && i < relatedWords.size()) {
            wf = (WordFrequency) relatedWords.get(i);

            // Exclude closed class German words (only prepositions and
            // articles). Smadja et al. remark in Section 7.1 that they mess up
            // the dice correlations if left in, so they are stripped.

            if (!wf.getWord().matches("(der|die|das|den|dem|des|ein|eine|einer|einen|einem|eines|bis|durch|entlang|f�r|gegen|ohne|um|an|am|auf|hinter|in|im|neben|�ber|unter|vor|zwischen|aus|ausser|au�er|bei|gegen�ber|mit|nach|seit|von|zu|zur|anstatt|statt|au�erhalb|innerhalb|trotz|w�hrend|wegen)")) {
                diceUB = (2 * wf.getFreq()) / (wordsFreq + wf.getFreq());

                if (dice(words_, wf.getWord()) > Td) {
                    // System.out.println("Adding Word: " + wf.getWord() +
                    // "\t\tFreq: " + wf.getFreq() + "\t\tDiceUB: " + diceUB +
                    // "\t\tDice: " + dice(words_, wf.getWord()));
                    wordsVector.add(wf.getWord());
                }
            }
            i++;
        }

        return wordsVector;
    }

    /**
     * getTranslation takes a collocation and returns a translation for it.
     *
     * @param collocation
     *            The collocation to be translated. This is one or more words in
     *            a String separated by spaces.
     * @param Tf
     *            The frequency threshold Tf.
     * @param Td
     *            The dice threshold Td.
     * @return A String containing the found translation.
     */
    public String getTranslation(String collocation, int Tf, double Td) {
        String translation = "";

        System.out.println("Finding translation for \"" + collocation + "\".  Found " + numSentencesContaining(collocation, "source") + " times in source corpus.");

        // Step 1
        Vector origCandidates = getRelatedCandidateWords(collocation, Tf, Td);

        // "Table" for local best translations
        Vector finalSet = new Vector();

        Vector tempCandidates = (Vector) origCandidates.clone();

        // Steps 2 Through 4 repeated while there are candidates left
        while (true) {
            // Step 2
            tempCandidates = removeLowDice(collocation, tempCandidates, Td);

            // Step 3
            if (tempCandidates.size() < 1) {
                break;
            }
            finalSet.add(getLocalBest(collocation, tempCandidates));

            // Step 4
            tempCandidates = cartesianProduct(tempCandidates, origCandidates);
        }

        // Step 5
        translation = getLocalBest(collocation, finalSet);

        return translation;
    }

    /**
     * removeLowDice takes a Vector of candidates (Strings of one or more words)
     * and computes the dice value for each of them, with relation to some
     * collocation. It returns a Vector with all of the candidates taken out
     * that scored below the threshold Td.
     *
     * @param words
     *            The collocation with which the dice score should be computed.
     * @param candidates
     *            A Vector of Strings, each containing one or more candidate
     *            words.
     * @param Td
     *            The dice threshold, under which no candidate shall pass.
     * @return The candidates Vector with those below the dice threshold
     *         removed.
     */
    private Vector removeLowDice(String words, Vector candidates, double Td) {
        Vector newCandidates = new Vector();

        // Go through the given candidates, and add the ones with a high enough
        // dice score to the new Vector
        for (int i = 0; i < candidates.size(); i++) {
            if (dice(words, (String) candidates.get(i)) > Td) {
                //DEBUG System.out.println("Keeping word: " + candidates.get(i) + "\t dice: " + dice(words, (String)candidates.get(i)) );
                newCandidates.add(candidates.get(i));
            }
        }

        return newCandidates;
    }

    /**
     * Looks at a group of candidates, and returns the one with the highest dice
     * score.
     *
     * @param collocation
     *            The collocation the candidates are being compared with.
     * @param candidates
     *            A Vector of Strings containing the candidates for best
     *            translation.
     * @return A String with the best candidate
     */
    private String getLocalBest(String collocation, Vector candidates) {
        String best = "";

        double bestScore = 0;

        // Find the maximum
        for (int i = 0; i < candidates.size(); i++) {
            double currentDice = dice(collocation, (String) candidates.get(i));
            if (currentDice > bestScore) {
                best = (String) candidates.get(i);
                bestScore = currentDice;
            }
        }

        return best;
    }

    /**
     * Returns a Vector that is the Cartesian Product of A and B, excluding duplicates.
     *
     * @param multiwordCandidates    A Vector of one or more words in Strings separated by spaces.
     * @param origCandidates        A Vector of single words in Strings.
     * @return
     */
    private Vector cartesianProduct(Vector multiwordCandidates, Vector origCandidates) {
        Vector retVector = new Vector();

        // A has the possible multiple-word strings in it. Combine these with the words
        // from the original candidates list.
        for (int i = 0; i < multiwordCandidates.size(); i++) {
            for (int j = 0; j < origCandidates.size(); j++) {
                String mwString = (String) multiwordCandidates.get(i);
                String origString = (String) origCandidates.get(j);
                // Make sure the original candidate isn't already in the string
                if (!mwString.startsWith(origString) && !mwString.endsWith(origString) && mwString.indexOf(" " + origString + " ") == -1) {
                    retVector.add(mwString + " " + origString);
                }
            }
        }

        return retVector;
    }

    /**
     * Takes a String with space separated words in it (presumably words for a
     * Lucene query) and appends +'s to each word, making them all required in
     * the Lucene query.
     *
     * @param words
     *            The String with space separated words.
     * @return A String with all words prepended with a +.
     */
    private String requireAll(String words) {
        // Add a + to the front of the first word
        words = "+" + words;
        // Require all words in X and Y in the search
        words = words.replaceAll(" ", " +");

        return words;
    }

    // Simple class to hold a word and its frequency
    private class WordFrequency {
        String word;
        int freq;

        public WordFrequency(String word_, int freq_) {
            word = word_;
            freq = freq_;
        }

        public String getWord() {
            return word;
        }

        public int getFreq() {
            return freq;
        }
    }
} // End class Corpus
