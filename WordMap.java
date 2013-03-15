import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class WordMap {

	/* postingMap:
	 *  1.Map a word to all postings belonging to it; 
	 *  2. Map TermInDoc objects to a specific doc in the postings of the specific word;
	 *  3. There are term frequency and all contexts(LinkedList) stored for the specific document of the  specific word;
	 */
	public TreeMap<String,HashMap<Integer,TermInDoc>> postingMap;
	
	/*urlDocMap:
	 * 1. Map a document ID to a UrlDocLen object;
	 * 2. A UrlDocLen object stores the url and document length of the Doc ID;
	 */
	public Map<Integer, UrlDocLen> urlDocMap;
	
	/*lexiconMap:
	 * 1.Map a word to a 2 dimension array (extensible);
	 * 2.int[0] store the document frequency, and int[1] store the address of the indices(postings);
	 */
	public Map<String,int[]> lexiconMap;
	
	WordMap(){
		//postingMap=new TreeMap<String,HashMap<Integer,TermInDoc>> ();
		urlDocMap=new HashMap<Integer, UrlDocLen> ();
		lexiconMap = new HashMap<String,int[]>();
		
	}
	
	
	/* insert a word into lexiconMap,  if it is existed, increment its document frequency;
	 * Note: increment the document frequency ONLY ONCE for each file;
	 */
	public void inSertIntoLexMap(String word,int file_num,int file_offset){
		int[] lexInfo=lexiconMap.get(word); //get the array of (time frequency + posting index) info of the give word;
		if(lexInfo==null){  //if the lexicon is not existed
			lexInfo=new int[3];
			lexInfo[0]=1;  //document frequency;
			lexInfo[1]=file_num; // index file number;
			lexInfo[2]=file_offset; //offset in the index file;
			lexiconMap.put(word,lexInfo);
		}else{ //if existed, increment document frequency
			//lexInfo[0]++;
//			int temp = lexInfo.elementAt(0)+1;
//			lexInfo.setElementAt(temp, 0);
			lexInfo[0]++;
			lexiconMap.put(word, lexInfo);
		}
	}
	
	/* insert the url and document length into the urlDocMap for the give doc ID;
	 */
	public void inSertIntoUrlDocMap(Integer docId,String url, Integer docLen){
		urlDocMap.put(docId,new UrlDocLen(url,docLen));
	}
	
	/* insert a posting into postingMap for the give word; 
	 * tempDocMap is a map from a docId of the give word to a TermInDoc object;
	 * TermInDoc object store all contexts and the term frequency of a given word in a give DocId
	 */
	public void inSertIntoPostingMap(String word,Integer docId, Byte context){
		HashMap<Integer,TermInDoc> termDocMap=postingMap.get(word); // find the termDocMap for the give map;	
		if(termDocMap==null){    // the word is first time occurs;
			termDocMap=new HashMap<Integer,TermInDoc>();
			termDocMap.put(docId,new TermInDoc(context)); // put the doc ID and context into termDocMap;
			postingMap.put(word, termDocMap);
		}else{   // there is a mapping for this word;
			TermInDoc tempTermInDoc=termDocMap.get(docId);// the temp TermInDoc object mapped from the given docId;
			if (tempTermInDoc==null){  // the Docment ID with the given word was not inserted before
				termDocMap.put(docId,new TermInDoc(context)); // put the doc ID and context into termDocMap;
//				inSertIntoLexMap(word);// update the lexicon map by this word;
			}else{// document ID was existed in the map of the given word;
				tempTermInDoc.contexts.add(context);  // add the context of the given word in the given docId;
				tempTermInDoc.termFreq++; // increment the term frequency of the given word in the docId;
				postingMap.put(word, termDocMap);
			}
		}
	}
	
	public Byte convertStrToByte(String str){
		return (byte)str.charAt(0);		
	}
} 

/*a class to store all contexts and the term frequency of a given word in a give DocId*/
class TermInDoc{
	int termFreq; //term frequency in a document;
	List<Byte> contexts;  // all contexts of the given word in a document;
	public TermInDoc(Byte context){
		contexts=new LinkedList<Byte>();
		contexts.add(context);
		termFreq=1;
	}
}


/*a class to store the url and document length of a given docId*/
class UrlDocLen{  
	String url;
	int docLen; //document length;
	public UrlDocLen(String url,int docLen){
		this.url=url;
		this.docLen=docLen;
	}
}