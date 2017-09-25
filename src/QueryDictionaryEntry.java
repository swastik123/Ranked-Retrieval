import java.util.List;


public class QueryDictionaryEntry {

	
	String term;
	Integer docFrequency;
	Integer termFrequency;
	List<PostingFileEntry> postingList;


	public QueryDictionaryEntry(String term, Integer docFreq, Integer termFreq, List<PostingFileEntry> list){
		this.term=term;
		this.docFrequency=docFreq;
		this.termFrequency=termFreq;
		this.postingList=list;
	}
	
}
