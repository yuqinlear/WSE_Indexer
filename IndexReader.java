import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;


public class IndexReader {
	public static WordMap wordmap;
	public LFUCache cache;
	
	IndexReader(){
		//postingMap=new TreeMap<String,HashMap<Integer,TermInDoc>> ();
		wordmap=new WordMap();
//		cache=new LFUCache(2000);// construct by using the number of inverted list
	}
	
	public void setupLexicon(){
		
	}
	
	public void setupUrl(){
		
	}

	
	public byte[] readIndex(String keyword) throws FileNotFoundException,IOException {
		/*first check the cache*/
//		cache.
		int[] lexInfo=wordmap.lexiconMap.get(keyword);//lexInfo=int[5];
		//lexInfo[0]=docFreq;lexInfo[1]=filename;lexInfo[2]=startOffset;lexInfo[3]=length by bytes;lexInfo[4]=chunkNum; 
		InputStream readin=new FileInputStream(new File(Integer.toString(lexInfo[1])));
//		byte[] metadata=new byte[lexInfo[4]]; //separate metadata and docID with Freq;
//		byte[] invertedlist=new byte[lexInfo[3]-lexInfo[4]]; //in compressed form;
		byte[] invertedlist=new byte[lexInfo[3]];
		readin.read(invertedlist);
		return invertedlist;
		
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		String keyword=" ";
		wordmap=new WordMap();
		wordmap.setupLexicon("result/lexicon_index.txt");
		wordmap.setupUrl("result/url_index.txt");
		IndexReader reader=new IndexReader();
		int[] lexinfo=wordmap.lexiconMap.get(keyword);
		byte[] invertedlist=reader.readIndex(keyword);
		int chunkNum=(int)lexinfo[4]&0xff;
		int DocIDLen=0;
		for(int i=0; i<chunkNum;i++){
			DocIDLen+=(int)invertedlist[i]&0xff;
		}
		List<Integer> DocIDList=VB.VBDECODE(invertedlist,chunkNum,DocIDLen);
		int temp=0;
		for (Integer DocID:DocIDList){
		System.out.print(DocID+" ");
		System.out.print(((int)invertedlist[chunkNum+DocIDLen+temp]&0xff)+" ");
		temp++;
		}
	}

}
