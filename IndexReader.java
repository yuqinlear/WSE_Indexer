import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;


public class IndexReader {
	public  static WordMap wordmap;
	public LFUCache cache;
	
	IndexReader(){
		//postingMap=new TreeMap<String,HashMap<Integer,TermInDoc>> ();
		
//		cache=new LFUCache(2000);// construct by using the number of inverted list
	}
	
//	public List<String> query(String[] keywords){
//		for (int i=0;i<keywords.length;i++){
//			openList(keywords[i]);
//		}
//		int docId=0;
//		while(docId){
//			
//		}
//	}
	
//	public int getFreq(byte[] invList, int docId){
//		
//	}
	/**return the equal or larger docID of the inverted list by a given docId;
	 */
	public int[] nextGEQ(byte[] invList, int docId,int chunkNum){
		int [] docIdFreq=new int[2];
		int upperBound=0,i=1,j,lengthOfChunks=0,chunksLen=0,temp,MAXINT=0x7FFFFFFF,postingPos=0;
		for(j=0;j<chunkNum;j++){//the lengthOfChunks
			lengthOfChunks+=invList[j];
		}
		
		if (chunkNum>1){
			for (i=1;(i<chunkNum)&&(upperBound<=docId);i++){ //i equals the next chunk of scanning chunk 
				upperBound=VB.firstDocIdOfChunk(invList,chunkNum,i);
			}
			if (upperBound==docId){
				docIdFreq[0]=upperBound;
				docIdFreq[1]=invList[chunkNum+lengthOfChunks+i*64];
				return docIdFreq;}
			for (j=0;j<i-1;j++){ //calculate the offset from the end of metadata to the beginning of scanning chunk;
				chunksLen+=(int)invList[j]&0xff;
			}
		}
		
		List<Integer> uncompressedChunk=VB.VBDECODE(invList,chunkNum+chunksLen,(int)invList[i-1]&0xff);
		temp=uncompressedChunk.get(0);
		for(j=1;j<uncompressedChunk.size();j++){
			if (docId>temp){
				temp+=uncompressedChunk.get(j);
			}else{
				docIdFreq[0]=temp;
				docIdFreq[1]=invList[chunkNum+lengthOfChunks+(i-1)*64+j-1];
				return docIdFreq;
			}
		}
		
		// compare with the last element in the chunk;
		if (docId>temp&&(i==chunkNum)){//docId > all the did in the list;
			docIdFreq[0]=MAXINT;
			docIdFreq[1]=0;
			return docIdFreq;	
		} 
		else if(docId>temp){
			docIdFreq[0]=upperBound;
			docIdFreq[1]=invList[chunkNum+lengthOfChunks+i*64];
			return docIdFreq;
			}
		else{
		docIdFreq[0]=temp;
		docIdFreq[1]=invList[chunkNum+lengthOfChunks+(i-1)*64+j-1];
		return docIdFreq;
		}
		
	}

	
	public byte[] openList(String keyword) throws FileNotFoundException,IOException {
		/*first check the cache*/
//		cache.
		int[] lexInfo=wordmap.lexiconMap.get(keyword);//lexInfo=int[5];
		//lexInfo[0]=docFreq;lexInfo[1]=filename;lexInfo[2]=startOffset;lexInfo[3]=length by bytes;lexInfo[4]=chunkNum; 
		RandomAccessFile readin=new RandomAccessFile(Integer.toString(lexInfo[1]),"r");
//		byte[] metadata=new byte[lexInfo[4]]; //separate metadata and docID with Freq;
//		byte[] invertedlist=new byte[lexInfo[3]-lexInfo[4]]; //in compressed form;
		byte[] invertedlist=new byte[lexInfo[3]];
		readin.seek(lexInfo[2]);
		readin.read(invertedlist);
		return invertedlist;
		
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		wordmap=new WordMap();
		String keyword="mall";
		wordmap.setupLexicon("result/lexicon_index.txt");
//		System.out.println(wordmap.lexiconMap.size());
		wordmap.setupUrl("result/url_index.txt");
		IndexReader reader=new IndexReader();
		int[] lexinfo=wordmap.lexiconMap.get(keyword);
		byte[] invertedlist=reader.openList(keyword);
		int chunkNum=(int)lexinfo[4]&0xff;
		int DocIDLen=0;// length of DocIDs by bytes
		for(int i=0; i<chunkNum;i++){
			DocIDLen+=(int)invertedlist[i]&0xff;
		}
		//uncompress
		List<Integer> DocIDList=VB.VBDECODE(invertedlist,chunkNum,DocIDLen);
		int docFreq = DocIDList.size();
		for(int i=0; i<chunkNum; i++){
            if((docFreq-i*64)>64){
				for (int j=i*64+1;j<64;j++){
					DocIDList.set(j,DocIDList.get(j)+DocIDList.get(j-1));
				}
            }
            else{
            	for (int j=i*64+1;j<docFreq;j++){
					DocIDList.set(j,DocIDList.get(j)+DocIDList.get(j-1));
				}
            }
		}	
		
		int temp=0;
		for (Integer DocID:DocIDList){
		System.out.print(DocID+" ");
	//	System.out.print(((int)invertedlist[chunkNum+DocIDLen+temp]&0xff)+" ");
		temp++;
		}
	}

}
