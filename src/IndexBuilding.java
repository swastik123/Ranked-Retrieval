import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class IndexBuilding {

	public static Map<String, DictionaryEntryTerm> lemmaDictionary = new HashMap<String, DictionaryEntryTerm>();
	public static Map<String, QueryDictionaryEntry> queryDictionary = new HashMap<String, QueryDictionaryEntry>();
	public static Map<String, DocumentFrequency> doclenMaxFreqDetailsLemmas = new HashMap<String, DocumentFrequency>();
	public static Map<String, DocumentFrequency> doclenMaxFreqDetailsLemmasQueries = new HashMap<String, DocumentFrequency>();
	public static Set<String> stopWordsList;
	static Pattern pattern = Pattern.compile("<.?TITLE>", Pattern.CASE_INSENSITIVE);

	public static void main(String[] args) {
		try{
			String folderPath = args[0];
			String queryFilePath = args[1];
			String stopWordsFilePath = args[2];
			stopWordsList = getListOfStopWords(stopWordsFilePath);
			IndexBuilding indexBuilding = new IndexBuilding();

			List<String> queryList = QueryReader.parseQueries(queryFilePath);
			//List<String> lemmaQueryList = QueryReader.lemmaQueryList(queryList, stopWordsList);
			for(int i=0;i<queryList.size();i++){				
				Map<String,Integer> queryTernFreqMap = QueryReader.singleQueryTermFreq(queryList.get(i),stopWordsList);
				int j=i+1;
				String name="Q"+j;
				buildDocFreqQueries(queryTernFreqMap,name,name,name);
			}
						
			Map<String, DictionaryEntryTerm> uncompressedIndexv1 = indexBuilding.buildIndexVersion1(folderPath, stopWordsList);		

			int avgDocLength = getAvgDocLength(uncompressedIndexv1);
			int avgQueryLength = getAvgQueryLength(queryDictionary);

			for (int i = 0; i < queryList.size(); i++) {
				System.out.println("\nQuery" + (i + 1) + " : " + queryList.get(i));
				QueryReader.processQuery(queryList.get(i), uncompressedIndexv1,	doclenMaxFreqDetailsLemmas, stopWordsList, avgDocLength,avgQueryLength,folderPath);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}



	public  Map<String, DictionaryEntryTerm> buildIndexVersion1(String folderPath, Set<String> stopWordsDict){

		Lemmatizer lemmatizer = new Lemmatizer();
		File folder = new File(folderPath);

		for(File file: folder.listFiles()){

			if(file.isFile()){
				Map<String, Integer> termFreqMap = lemmatizer.buildLemmaDictionary(file, stopWordsDict);

				buildIndexesVersion1(file.getName(),termFreqMap,file.getName(),getTitle(file));
			}
		}

		return lemmaDictionary;
	}

	public  void buildIndexesVersion1(String docId, Map<String,Integer> lemmaTermFreqDict, String fileName, String title){

		long maxTermFreq = 0;
		long docLen = 0;


		for (String term : lemmaTermFreqDict.keySet()) {
			int termFreq = lemmaTermFreqDict.get(term);
			docLen += termFreq;
			if (!(stopWordsList.contains(term))) {
				if (termFreq > maxTermFreq) {
					maxTermFreq = termFreq;
				}
				updatePostingValuesVersion1(docId, term, lemmaTermFreqDict.get(term));
			}
		}


		DocumentFrequency entry = new DocumentFrequency(docLen, maxTermFreq, fileName, title);
		doclenMaxFreqDetailsLemmas.put(docId, entry);


	}





	private  void updatePostingValuesVersion1(String docId, String term,
			Integer termFrequency) {
		DictionaryEntryTerm entry = null;
		if (lemmaDictionary != null)
			entry = lemmaDictionary.get(term);

		if (entry == null) {
			entry = new DictionaryEntryTerm(term, 0, 0,
					new LinkedList<PostingFileEntry>());
			entry.postingList = new LinkedList<PostingFileEntry>();

		}
		entry.postingList.add(new PostingFileEntry(docId, termFrequency));
		entry.docFrequency += 1;
		entry.termFrequency += termFrequency;
		lemmaDictionary.put(term, entry);

	}



	public static HashSet<String> getListOfStopWords(String filePath){

		HashSet<String> stopWordsDict = new HashSet<String>();

		FileInputStream fileInputStream = null;
		DataInputStream dataInputStream = null;
		BufferedReader bufferedReader = null;

		try{
			File stopwordsFile = new File(filePath);
			fileInputStream = new FileInputStream(stopwordsFile);
			dataInputStream = new DataInputStream(fileInputStream);
			bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));

			String line = null;


			while((line=bufferedReader.readLine())!=null){

				stopWordsDict.add(line.toLowerCase());
			}
		}
		catch(Exception e){
			try {
				fileInputStream.close();
				dataInputStream.close();
				bufferedReader.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			e.printStackTrace();
		}
		return stopWordsDict;
	}



	public static int getAvgDocLength(Map<String, DictionaryEntryTerm> lemmadictionary) {
		BigInteger length = new BigInteger(Integer.toString(0));
		Set keys = lemmadictionary.keySet();
		Iterator i = keys.iterator();
		try{
			while (i.hasNext()) {
				String key = (String) i.next();
				length = length.add(new BigInteger(
						lemmadictionary.get(key).docFrequency.toString()));
			}

			length = length.divide(new BigInteger(Integer.toString(lemmadictionary
					.size())));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return length.intValue();
	}
	
	public static int getAvgQueryLength(Map<String, QueryDictionaryEntry> querydictionary) {
		BigInteger length = new BigInteger(Integer.toString(0));
		Set keys = querydictionary.keySet();
		Iterator i = keys.iterator();
		try{
			while (i.hasNext()) {
				String key = (String) i.next();
				length = length.add(new BigInteger(
						querydictionary.get(key).docFrequency.toString()));
			}

			length = length.divide(new BigInteger(Integer.toString(querydictionary.size())));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return length.intValue();
	}

	public static String getTitle(File file) {
		try {
			String data = new String(Files.readAllBytes(file.toPath()));
			String[] parts = pattern.split(data);
			if (parts.length > 1) {
				return parts[1].replace("\n", " ");
			} else
				System.out.println(".....!!" + file.getPath());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static void buildDocFreqQueries(Map<String,Integer> queryTermFreqDict, String fileName, String title, String docId){
			
			long maxTermFreq = 0;
			long docLen = 0;


			for (String term : queryTermFreqDict.keySet()) {
				int termFreq = queryTermFreqDict.get(term);
				docLen += termFreq;
				if (!(stopWordsList.contains(term))) {
					if (termFreq > maxTermFreq) {
						maxTermFreq = termFreq;
					}
					updatePostingValuesForQueries(docId, term, queryTermFreqDict.get(term));
				}
			}


			DocumentFrequency entry = new DocumentFrequency(docLen, maxTermFreq, fileName, title);
			doclenMaxFreqDetailsLemmasQueries.put(docId, entry);
		

	}
	
	
	
	
	private  static void updatePostingValuesForQueries(String docId, String term,Integer termFrequency) {
		QueryDictionaryEntry entry = null;
		if (queryDictionary != null)
			entry = queryDictionary.get(term);

		if (entry == null) {
			entry = new QueryDictionaryEntry(term, 0, 0,	new LinkedList<PostingFileEntry>());
			entry.postingList = new LinkedList<PostingFileEntry>();

		}
		entry.postingList.add(new PostingFileEntry(docId, termFrequency));
		entry.docFrequency += 1;
		entry.termFrequency += termFrequency;
		queryDictionary.put(term, entry);

	}
}
