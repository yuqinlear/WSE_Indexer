import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;



public class IndexReader {
	public WordMap wordmap;
	public LFUCache<String, byte[]> listCache;
	public LFUCache<String[], String[]> resultCache;
	public String[] result;
	
	IndexReader(){
		wordmap=new WordMap();
		//postingMap=new TreeMap<String,HashMap<Integer,TermInDoc>> ();		
		listCache=new LFUCache<String,byte[]>(200);// construct by using the number of inverted list
		resultCache=new LFUCache<String[],String[]>(200);
	}

	public String[] query(String[] keywords) throws FileNotFoundException, IOException{
		//open list
		String[] result=new String[10];
		Queue<Page> rankHeap=new PriorityQueue<Page>(100,new Comparator<Page>(){
			public int compare(Page p1, Page p2){
				if(p1==p2){return 0;}
				if(p1.score<p2.score) {return 1;}
				else {return -1;}
			}
		});
		
		/*convert keywords to lowercase*/
		for(int i=0;i<keywords.length;i++){
			keywords[i]=keywords[i].toLowerCase();
		}
		
		/*result cache*/
		Arrays.sort(keywords);
		if (resultCache.contains(keywords)){
			return resultCache.get(keywords);
		}
		int keywordNum=keywords.length, docId=0,tempDocId,i;
		byte[][] invLists = new byte[keywordNum][];
		int[][] docIdFreq=new int[keywordNum][2];//docId+freq, [0]=docId, [1]=termFreq;
		int[] chunkNums=new int[keywordNum];
		/*list cache*/
		for(i=0;i<keywordNum;i++){
			if(listCache.contains(keywords[i])){
				invLists[i]=(byte[]) listCache.get(keywords[i]);
			}
			invLists[i]=openList(keywords[i]);
			listCache.put(keywords[i],invLists[i]);
		}
		for(i=0;i<keywordNum;i++){
		int[] lexinfo=wordmap.lexiconMap.get(keywords[i]);
		chunkNums[i]=(int)lexinfo[4]&0xff;
		}
		//TODO sort the inverted list by the number of docId;
		while(docId<0x7FFFFFFF){
			docIdFreq[0]=nextGEQ(invLists[0],docId,chunkNums[0]);//get the next GEQ docID+TF in the inverted list of keywords[0];
			if (docIdFreq[0][0]==0x7FFFFFFF){break;}
			tempDocId=docId=docIdFreq[0][0];
			for(i=1;i<keywordNum;i++){
				docIdFreq[i]=nextGEQ(invLists[i],docId,chunkNums[i]);
				tempDocId=docIdFreq[i][0];
				if(docId!=docIdFreq[i][0]) {break;}
			}
			//no intersection;
			if(tempDocId>docId){docId=tempDocId;}
			//intersection;
			else{
				/*BM25*/				
				rankHeap.add(new Page(docId,BM25(keywords,docIdFreq)));				
				docId++;
			}
		}		
		
		for (i=0;i<10;i++){
			if (!rankHeap.isEmpty()){
			Page page=rankHeap.poll();
			UrlDocLen urlInfo=wordmap.urlDocMap.get((page.docId));
			System.out.println(urlInfo.url);
			System.out.println(page.score);
			result[i]=urlInfo.url;
			}
//			else{
//			result[i]="";
//			}
		}
		resultCache.put(keywords, result);
		return result;
	}
	
	/*
	 * docIdFreq[i][j], i=number of keywords, j=0 docId, j=1 frequency;
	 */
	public double BM25(String[] keywords,int[][] docIdFreq){
		double k1=1.2,b=0.75,K,score=0;
		UrlDocLen ull;
		int[] lexinfo;
		//lexInfo[0]=docFreq;lexInfo[1]=filename;lexInfo[2]=startOffset;lexInfo[3]=length by bytes;lexInfo[4]=chunkNum; 
		for (int i=0;i<keywords.length; i++){
			lexinfo=wordmap.lexiconMap.get(keywords[i]);	
			ull=wordmap.urlDocMap.get(docIdFreq[i][0]);
			K=k1*((1-b)+b*ull.docLen/wordmap.averageLen);
			score+=Math.log10(((wordmap.totalPageNum-lexinfo[0]+0.5)/(lexinfo[0]+0.5)))*((k1+1)*docIdFreq[i][1]/(K+docIdFreq[i][1]));
		}
		return score;
	}
	
	class Page{
		int docId;
		double score;
		Page(int docId,double score){
			this.docId=docId;
			this.score=score;
		}
	}
	

	/**return the equal or larger docId and the term freq in the docId of the inverted list by a given docId;
	 * return MAXINT if no docId matched
	 */
	public static int[] nextGEQ(byte[] invList, int docId,int chunkNum){
		int [] docIdFreq=new int[2];
		int upperBound=0,i=1,j,lengthOfChunks=0,chunksLen=0,temp,MAXINT=0x7FFFFFFF;
		for(j=0;j<chunkNum;j++){//the length Of Chunks(docIDs);
			lengthOfChunks+=invList[j];
		}
		
		if (chunkNum>1){
			for (i=1;i<chunkNum;i++){ //i equals the next chunk of scanning chunk 
				upperBound=VB.firstDocIdOfChunk(invList,chunkNum,i);
				if(upperBound>docId){break;};
			}
			if (upperBound==docId){
				docIdFreq[0]=upperBound;
				docIdFreq[1]=invList[chunkNum+lengthOfChunks+(i-1)*64];//get the term frequency corresponding to the docId
				return docIdFreq;}
			for (j=0;j<i-1;j++){ //calculate the offset from the end of metadata to the beginning of scanning chunk;
				chunksLen+=(int)invList[j]&0xff;
			}
		}
		
		List<Integer> uncompressedChunk=VB.VBDECODE(invList,chunkNum+chunksLen,(int)invList[i-1]&0xff);
		temp=uncompressedChunk.get(0);
		for(j=1;j<uncompressedChunk.size();j++){
			if (docId>temp){
				temp+=uncompressedChunk.get(j);// sum the gaps;
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
		IndexReader reader=new IndexReader();
		String[] keywords={"Study","English","US"};
		reader.wordmap.setupLexicon("result/lexicon_index.txt");
////		System.out.println(wordmap.lexiconMap.size());
		reader.wordmap.setupUrl("result/url_index.txt");
		reader.result=reader.query(keywords);

//		for (int i=0;i<reader.result.length;i++){
//			System.out.println(reader.result[i]);
//		}
//		int[] lexinfo=wordmap.lexiconMap.get(keyword);
//		byte[] invertedlist=reader.openList(keyword);
//		int chunkNum=(int)lexinfo[4]&0xff;
//		int DocIDLen=0;// length of DocIDs by bytes
//		for(int i=0; i<chunkNum;i++){
//			DocIDLen+=(int)invertedlist[i]&0xff;
//		}
//		int [] docIdFreq = nextGEQ(invertedlist, 0 ,chunkNum);
//		System.out.println("docID: "+docIdFreq[0]+"  Freq: "+docIdFreq[1]);
		//uncompress
//		List<Integer> DocIDList=VB.VBDECODE(invertedlist,chunkNum,DocIDLen);
//		int docFreq = DocIDList.size();
//		for(int i=0; i<chunkNum; i++){
//            if((docFreq-i*64)>64){
//				for (int j=i*64+1;j<64;j++){
//					DocIDList.set(j,DocIDList.get(j)+DocIDList.get(j-1));
//				}
//            }
//            else{
//            	for (int j=i*64+1;j<docFreq;j++){
//					DocIDList.set(j,DocIDList.get(j)+DocIDList.get(j-1));
//				}
//            }
//		}	
//		
//		int temp=0;
//		for (Integer DocID:DocIDList){
//		System.out.print(DocID+" ");
//	//	System.out.print(((int)invertedlist[chunkNum+DocIDLen+temp]&0xff)+" ");
//		temp++;
//		}
	}
	

}
