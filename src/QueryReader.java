import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;


public class QueryReader {

	public static List<String> parseQueries (String queryFilePath){
		List<String> queryList = new ArrayList<String>();

		try {
			File queryFile = new File(queryFilePath);
			if (queryFile.isFile()) {
				String querydata = new String(Files.readAllBytes(new File(
						queryFilePath).toPath()));
				String[] querySplits = Pattern.compile("[Q0-9:]+").split(
						querydata);
				for (String split : querySplits) {
					String query = split.trim().replaceAll("\\r\\n", " ");
					query=query.replaceAll(".I", "");
					query=query.replaceAll(".W\n", "");
					//query=query.replaceAll("\n", "");
					if (query.length() > 0) {
						queryList.add(query);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return queryList;
	}

	public static void processQuery(String query, Map<String,DictionaryEntryTerm> dictIndex, Map<String,DocumentFrequency> maxDocLemmas, Set<String> stopWordsDict, int avgDocumentLen,int avgQueryLength,String folderPath ){


		Map<String, Integer> lemmaDict = buildLemmaDictionaryForQuery(query, stopWordsDict);
		eliminateStopWords(lemmaDict, stopWordsDict);

		Map<String, Double> W1_tab = new HashMap<String, Double>();
		Map<String, Double> W2_tab = new HashMap<String, Double>();

		Map<String, Double> queryW1Map = new HashMap<String, Double>();
		Map<String, Double> queryW2Map = new HashMap<String, Double>();
		
		int queryCollLen=0;
		int queryMaxTermFreq=0;
		for(Map.Entry<String, Integer> entry : lemmaDict.entrySet()){
			queryCollLen+=entry.getValue();	
			if(entry.getValue()>queryMaxTermFreq)
				queryMaxTermFreq=entry.getValue();
		}
		
		for (String queryTerm : lemmaDict.keySet()) {
			DictionaryEntryTerm dictEntryTerm = dictIndex.get(queryTerm);
			if (dictEntryTerm == null) {
				continue;
			}

			
			
			/*QueryDictionaryEntry queryEntryTerm = IndexBuilding.queryDictionary.get(queryTerm);
			int queryDocFreq = queryEntryTerm.docFrequency;*/
			
			/*for (PostingFileEntry postingFileEntry : queryEntryTerm.postingList) {
				int termFreq = postingFileEntry.frequency;
				long maxTermFrequency = IndexBuilding.doclenMaxFreqDetailsLemmasQueries.get(postingFileEntry.docId).maxFrequency;
				long docLength = IndexBuilding.doclenMaxFreqDetailsLemmasQueries.get(postingFileEntry.docId).docLength;
				int collectionSize = IndexBuilding.doclenMaxFreqDetailsLemmasQueries.size();
				queryW1+= calculateW1(termFreq, maxTermFrequency, queryDocFreq, collectionSize);
				queryW2+= calculateW2(termFreq, docLength, avgQueryLength, queryDocFreq,collectionSize);				
			}*/
			
			double queryW1=0;
			double queryW2=0;
			
			queryW1= calculateW1(lemmaDict.get(queryTerm), queryMaxTermFreq, 1, 20);
			queryW2= calculateW2(lemmaDict.get(queryTerm), queryCollLen, avgQueryLength, 1,20);
			
		
			
			
			DecimalFormat df = new DecimalFormat("#.####");
			df.setRoundingMode(RoundingMode.CEILING);
			if(queryW1Map.containsKey(queryTerm)){
				double val = queryW1Map.get(queryTerm)+queryW1;
				String stringVal = df.format(val);
				
				queryW1Map.put(queryTerm, Double.parseDouble(stringVal));
			}
			else{
				double val = queryW1;
				String stringVal = df.format(val);
				queryW1Map.put(queryTerm, Double.parseDouble(stringVal));
			}
			
			if(queryW2Map.containsKey(queryTerm)){
				double val = queryW2Map.get(queryTerm)+queryW2;
				String stringVal = df.format(val);
				queryW2Map.put(queryTerm, Double.parseDouble(stringVal));
			}
			else{
				double val = queryW2;
				String stringVal = df.format(val);
				queryW2Map.put(queryTerm, Double.parseDouble(stringVal));
			}
			
			int docFrequency = dictEntryTerm.docFrequency;
			for (PostingFileEntry postingFileEntry : dictEntryTerm.postingList) {
				int termFreq = postingFileEntry.frequency;
				long maxTermFrequency = maxDocLemmas.get(postingFileEntry.docId).maxFrequency;
				long docLength = maxDocLemmas.get(postingFileEntry.docId).docLength;
				int collectionSize = maxDocLemmas.size();
				double w1 = calculateW1(termFreq, maxTermFrequency, docFrequency, collectionSize);
				double w2 = calculateW2(termFreq, docLength, avgDocumentLen, docFrequency,collectionSize);
				addWeights(W1_tab, postingFileEntry.docId, w1);
				addWeights(W2_tab, postingFileEntry.docId, w2);
			}
		}


		System.out.print("Lemma Query: ");
		for (String queryTerm : lemmaDict.keySet()) {
			System.out.print(queryTerm + " ");
		}

		System.out.println();
		System.out.println("Vector representation of query(W1): ");
		System.out.println(queryW1Map);
		
		System.out.println("Vector representation of query(W2): ");
		System.out.println(queryW2Map);
			
		/*System.out.println("Vector Representation: "+lemmaDict);
		System.out.println();*/

		System.out.println("\nTop 5 documents by W1");
		showTop5Entries(W1_tab,folderPath, stopWordsDict);
		docVectorRepresentation(W1_tab,folderPath, stopWordsDict);
		
		System.out.println("\nTop 5 documents by W2");
		showTop5Entries(W2_tab, folderPath, stopWordsDict);
		docVectorRepresentation(W2_tab, folderPath, stopWordsDict);
	}


	public static void addWeights(Map<String, Double> weight_tab, String docId,double w) {
		if (weight_tab.get(docId) == null) {
			weight_tab.put(docId, w);
			return;
		}
		weight_tab.put(docId, w + weight_tab.get(docId));
	}



	public static double calculateW1(int termFreq, long maxTermFreq, int docFreq, int collectionSize) {
		double w1 = 0;
		try {
			w1 = (0.4 + 0.6 * Math.log(termFreq + 0.5)/ Math.log(maxTermFreq + 1.0))* (Math.log(collectionSize / docFreq) / Math.log(collectionSize));
		} 
		catch (Exception e) {
			w1 = 0;
		}
		return w1;
	}

	public static double calculateW2(int termFreq, long doclength, double avgDoclength,	int docFreq, int collectionSize) {
		double w2 = 0;
		try {
			w2 = (0.4 + 0.6* (termFreq / (termFreq + 0.5 + 1.5 * (doclength / avgDoclength)))* Math.log(collectionSize / docFreq)/ Math.log(collectionSize));
		} 
		catch (Exception e) {
			w2 = 0;
		}
		return w2;
	}


	public static void eliminateStopWords(Map<String, Integer> lemmaDic, Set<String> stopWords){

		Iterator<String> iterator = lemmaDic.keySet().iterator();
		while (iterator.hasNext()) {
			if (stopWords.contains(iterator.next())) {
				iterator.remove();
			}
		}
	}

	public static Map<String, Integer> buildLemmaDictionaryForQuery(String query, Set<String> stopWordsDict) {
		// TODO Auto-generated method stub
		Map<String,Integer> queryDictionary=new HashMap<String,Integer>();

		StringTokenizer st = new StringTokenizer(query, " ");
		String strCurrent="";
		while (st.hasMoreTokens()) {
			strCurrent = st.nextToken();

			strCurrent = strCurrent.replaceAll("['.`]+", "");
			Lemmatizer.tokenizeAndaddToLemmaDictionary(strCurrent, queryDictionary, stopWordsDict);

		}
		return queryDictionary;
	}

	public static void showTop5Entries(Map<String, Double> weight_table, String folderPath, Set<String> stopWordsDict) {

		Map<String, Double> sortedWeightMap = new HashMap<String, Double>();
		sortedWeightMap = sortMap(weight_table);
		
		int i=1;
		
		for (Map.Entry<String, Double> entry : sortedWeightMap.entrySet()) {
			if(i>5)
				break;

			Map<String, String> dataMap = dataDictionary(folderPath, stopWordsDict, entry.getKey());
			Map<String, Integer> docVecRep=documentVectorRepresentation(dataMap);
			System.out.println("Document Name: "+entry.getKey());
			i++;
		}
		
		i=1;
		System.out.println("Rank" + " Weight   "  + " Document Name" + " \t Title");
		for (Map.Entry<String, Double> entry : sortedWeightMap.entrySet()) {
			if(i>5)
				break;

			Map<String, String> dataMap = dataDictionary(folderPath, stopWordsDict, entry.getKey());
			Map<String, Integer> docVecRep=documentVectorRepresentation(dataMap);
			DecimalFormat df = new DecimalFormat("#.####");
			df.setRoundingMode(RoundingMode.CEILING);

			Double d = entry.getValue().doubleValue();			

			System.out.println((i)
					+ " : "
					+ df.format(d)
					+ " : "
					+ IndexBuilding.doclenMaxFreqDetailsLemmas.get(entry
							.getKey()).documentName
							+ " : "
							+ IndexBuilding.doclenMaxFreqDetailsLemmas.get(entry
									.getKey()).documentTitle);		

			//System.out.println("Vector Representation: "+docVecRep);
			i++;
		}

	}

	public static void docVectorRepresentation(Map<String, Double> weight_table, String folderPath, Set<String> stopWordsDict) {

		Map<String, Double> sortedWeightMap = new HashMap<String, Double>();
		sortedWeightMap = sortMap(weight_table);
		
		int i=1;
		
		for (Map.Entry<String, Double> entry : sortedWeightMap.entrySet()) {
			if(i>5)
				break;

			Map<String, String> dataMap = dataDictionary(folderPath, stopWordsDict, entry.getKey());
			Map<String, Integer> docVecRep=documentVectorRepresentation(dataMap);
			System.out.println("Vector Representation: "+docVecRep);
			i++;
		}
	

	}


	public static <K, V extends Comparable<? super V>> Map<K, V> sortMap(final Map<K, V> mapToSort) {
		List<Map.Entry<K, V>> entries = new ArrayList<Map.Entry<K, V>>(mapToSort.size());

		entries.addAll(mapToSort.entrySet());

		Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
			public int compare(final Map.Entry<K, V> entry1, final Map.Entry<K, V> entry2) {
				return entry2.getValue().compareTo(entry1.getValue());
			}
		});

		Map<K, V> sortedMap = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}



	public static Map<String, Integer> documentVectorRepresentation(Map<String, String> dataMap){

		Map<String, Integer> map = new HashMap<String, Integer>();

		String value="";
		for (Map.Entry<String, String> entry : dataMap.entrySet()) {
			value = entry.getValue();

		}
		StringTokenizer tokenizer = new StringTokenizer(value," ");
		while(tokenizer.hasMoreTokens()){
			String currString = tokenizer.nextToken();
			if(map.containsKey(currString)){
				map.put(currString, map.get(currString)+1);
			}
			else{
				map.put(currString, 1);
			}
		}
		return map;

	}



	public static Map<String, Integer> queryTermFreq(List<String> queryList, Set<String> stopwordsDictionary){
		Map<String, Integer> queryTermFreqMap = new HashMap<String, Integer>();

		try{
			String dataString="";		

			for(int k=0;k<queryList.size();k++){

				String query = queryList.get(k);

				StringTokenizer tokenizer = new StringTokenizer(query," ");

				while(tokenizer.hasMoreTokens()){

					String currString = tokenizer.nextToken();
					// removing xml tags and changing to lower case
					currString =currString.replaceAll("<[^>]+>", "").toLowerCase();
					// removing white spaces and numeric values
					currString = currString.replaceAll("[0-9]","");
					//removing certain special characters
					currString = currString.replaceAll("[^\\w\\s-'.!:;]+", "");
					if(!(stopwordsDictionary.contains(currString)))
					{
						currString = currString.replaceAll("['.`]+", "");								

						if (currString.trim().length() > 0) {

							// handle special cases
							if (currString.endsWith("'s")) {
								currString = currString.replace("'s", "").trim();
								dataString=Lemmatizer.getInstance().getLemma(currString);
								if(queryTermFreqMap.containsKey(dataString)){
									queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
								}
								else{
									queryTermFreqMap.put(dataString.trim(), 1);
								}

							} else if (currString.contains("-")) {
								String[] newTokens = currString.split("-");
								for (String newToken : newTokens) {

									if(newToken.trim().length()>0){
										dataString=Lemmatizer.getInstance().getLemma(newToken);
										if(queryTermFreqMap.containsKey(dataString)){
											queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
										}
										else{
											queryTermFreqMap.put(dataString.trim(), 1);
										}
									}
								}
							} else if (currString.contains("_")) {
								String[] newTokens = currString.split("_");
								for (String newToken : newTokens) {
									if(newToken.trim().length()>0){
										dataString=Lemmatizer.getInstance().getLemma(newToken);
										if(queryTermFreqMap.containsKey(dataString)){
											queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
										}
										else{
											queryTermFreqMap.put(dataString.trim(), 1);
										}
									}
								}
							} 
							else{
								dataString=Lemmatizer.getInstance().getLemma(currString);
								if(queryTermFreqMap.containsKey(dataString)){
									queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
								}
								else{
									queryTermFreqMap.put(dataString.trim(), 1);
								}
							}

						}
					}

				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return queryTermFreqMap;	
	}




	public static Map<String, String> dataDictionary(String folderPath, Set<String> stopwordsDictionary, String fileName){
		Map<String, String> map = new HashMap<String, String>();
		FileInputStream fileInputStream = null;
		DataInputStream dataInputStream = null;
		BufferedReader bufferedReader = null;
		try{
			File folder = new File(folderPath+File.separator+fileName);
			String dataString="";		
			if (folder.isFile()) {				
				fileInputStream = new FileInputStream(folder);
				dataInputStream = new DataInputStream(fileInputStream);
				bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
				String strline = null;			


				while((strline=bufferedReader.readLine())!=null){

					StringTokenizer tokenizer = new StringTokenizer(strline," ");

					while(tokenizer.hasMoreTokens()){

						String currString = tokenizer.nextToken();
						// removing xml tags and changing to lower case
						currString =currString.replaceAll("<[^>]+>", "").toLowerCase();
						// removing white spaces and numeric values
						currString = currString.replaceAll("[0-9]","");
						//removing certain special characters
						currString = currString.replaceAll("[^\\w\\s-'.!:;]+", "");
						if(!(stopwordsDictionary.contains(currString)))
						{
							currString = currString.replaceAll("['.`]+", "");								

							if (currString.trim().length() > 0) {

								// handle special cases
								if (currString.endsWith("'s")) {
									currString = currString.replace("'s", "").trim();
									dataString=dataString+Lemmatizer.getInstance().getLemma(currString);

								} else if (currString.contains("-")) {
									String[] newTokens = currString.split("-");
									for (String newToken : newTokens) {

										dataString=dataString+Lemmatizer.getInstance().getLemma(newToken);
									}
								} else if (currString.contains("_")) {
									String[] newTokens = currString.split("_");
									for (String newToken : newTokens) {
										dataString=dataString+Lemmatizer.getInstance().getLemma(newToken);
									}
								} 
								else{
									dataString=dataString+Lemmatizer.getInstance().getLemma(currString);
								}

							}
						}

					}
				}
			}

			map.put(folder.getName(), dataString);

		}
		catch(Exception e){
			e.printStackTrace();
		}
		return map;	
	}




	public static List<String> lemmaQueryList(List<String> queryList, Set<String> stopwordsDictionary){
		List<String> list = new ArrayList<String>();
		try{
			String dataString="";		
			for(int i=0;i<queryList.size();i++){
				String query = queryList.get(i);
				StringTokenizer tokenizer = new StringTokenizer(query," ");
				while(tokenizer.hasMoreTokens()){
					String currString = tokenizer.nextToken();
					// removing xml tags and changing to lower case
					currString =currString.replaceAll("<[^>]+>", "").toLowerCase();
					// removing white spaces and numeric values
					currString = currString.replaceAll("[0-9]","");
					//removing certain special characters
					currString = currString.replaceAll("[^\\w\\s-'.!:;]+", "");
					if(!(stopwordsDictionary.contains(currString)))
					{
						currString = currString.replaceAll("['.`]+", "");
						if (currString.trim().length() > 0) {
							// handle special cases
							if (currString.endsWith("'s")) {
								currString = currString.replace("'s", "").trim();
								dataString=dataString+Lemmatizer.getInstance().getLemma(currString)+" ";

							} else if (currString.contains("-")) {
								String[] newTokens = currString.split("-");
								for (String newToken : newTokens) {
									if(newToken.trim().length()>0){
										dataString=dataString+Lemmatizer.getInstance().getLemma(newToken)+" ";
									}
								}
							} else if (currString.contains("_")) {
								String[] newTokens = currString.split("_");
								for (String newToken : newTokens) {
									if(newToken.trim().length()>0){
										dataString=dataString+Lemmatizer.getInstance().getLemma(newToken)+" ";
									}
								}
							} 
							else{
								dataString=dataString+Lemmatizer.getInstance().getLemma(currString)+" ";
							}

						}
					}

				}
				list.add(dataString);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return list;	
	}












	public static Map<String, Integer> singleQueryTermFreq(String query, Set<String> stopwordsDictionary){
		Map<String, Integer> queryTermFreqMap = new HashMap<String, Integer>();

		try{
			String dataString="";		
			StringTokenizer tokenizer = new StringTokenizer(query," ");
			while(tokenizer.hasMoreTokens()){

				String currString = tokenizer.nextToken();
				// removing xml tags and changing to lower case
				currString =currString.replaceAll("<[^>]+>", "").toLowerCase();
				// removing white spaces and numeric values
				currString = currString.replaceAll("[0-9]","");
				//removing certain special characters
				currString = currString.replaceAll("[^\\w\\s-'.!:;]+", "");
				if(!(stopwordsDictionary.contains(currString)))
				{
					currString = currString.replaceAll("['.`]+", "");								

					if (currString.trim().length() > 0) {

						// handle special cases
						if (currString.endsWith("'s")) {
							currString = currString.replace("'s", "").trim();
							dataString=Lemmatizer.getInstance().getLemma(currString);
							if(queryTermFreqMap.containsKey(dataString)){
								queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
							}
							else{
								queryTermFreqMap.put(dataString.trim(), 1);
							}

						} else if (currString.contains("-")) {
							String[] newTokens = currString.split("-");
							for (String newToken : newTokens) {

								if(newToken.trim().length()>0){
									dataString=Lemmatizer.getInstance().getLemma(newToken);
									if(queryTermFreqMap.containsKey(dataString)){
										queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
									}
									else{
										queryTermFreqMap.put(dataString.trim(), 1);
									}
								}
							}
						} else if (currString.contains("_")) {
							String[] newTokens = currString.split("_");
							for (String newToken : newTokens) {
								if(newToken.trim().length()>0){
									dataString=Lemmatizer.getInstance().getLemma(newToken);
									if(queryTermFreqMap.containsKey(dataString)){
										queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
									}
									else{
										queryTermFreqMap.put(dataString.trim(), 1);
									}
								}
							}
						} 
						else{
							dataString=Lemmatizer.getInstance().getLemma(currString);
							if(queryTermFreqMap.containsKey(dataString)){
								queryTermFreqMap.put(dataString.trim(), queryTermFreqMap.get(dataString)+1);
							}
							else{
								queryTermFreqMap.put(dataString.trim(), 1);
							}
						}

					}
				}

			}

		}
		catch(Exception e){
			e.printStackTrace();
		}
		return queryTermFreqMap;	
	}
}
