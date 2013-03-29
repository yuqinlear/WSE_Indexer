import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;



public class IndexReader {
	public WordMap wordmap;
	public Cache<String, byte[]> listCache;
	public Cache<String[], String[]> resultCache;
	public String[] result;
	
	IndexReader(){
		wordmap=new WordMap();
		listCache=new LRFUCache<String,byte[]>(200,200);// construct by using the number of inverted list
		resultCache=new LRFUCache<String[],String[]>(200,200);
	}

	/**
	 * given all keywords, prune them to the limited words and return the urls with highest scores calculated by BM25
	 */		
	public String[] query(String[] originalWords,int pruneSize, int resultSize) throws FileNotFoundException, IOException{
		//trim to pruneSize
		List<String> keywordsList=new ArrayList<String>(pruneSize);
		String[] result=new String[resultSize];
		String[] keywords;
		int[] lexinfo;
		boolean setResultCache=true;
		int[] chunkNums=new int[pruneSize];
		
		/*convert originalWords to lowercase*/
		for(int i=0;i<originalWords.length;i++){
			originalWords[i]=originalWords[i].toLowerCase();
		}
		
		/*prune the input original keywords to a limited number, add remove these words which were not existed in the lexicon*/
		Arrays.sort(originalWords);
		int j=0;
		for (int i=0;i<originalWords.length&&(keywordsList.size()<pruneSize);i++){
			if((lexinfo=wordmap.lexiconMap.get(originalWords[i]))!=null){
				keywordsList.add(originalWords[i]);
				chunkNums[j]=(int)lexinfo[4]&0xff;
				j++;
			}
		}
		
		if (keywordsList.isEmpty()){return result;}
		//keywords we really concerns
		keywords=new String[keywordsList.size()];
		keywordsList.toArray(keywords);
		int keywordNum=keywords.length;

		Queue<Page> rankHeap=new PriorityQueue<Page>(100,new Comparator<Page>(){
			public int compare(Page p1, Page p2){
				if(p1==p2){return 0;}
				if(p1.score<p2.score) {return 1;}
				else {return -1;}
			}
		});
			
		/*result cache*/
		if (resultCache.contains(keywords)){
			return resultCache.get(keywords);
		}
		int docId=0,tempDocId,i;
		byte[][] invLists = new byte[keywordNum][];
		int[][] docIdFreq=new int[keywordNum][2];//docId+freq, [0]=docId, [1]=termFreq;
		/*list cache*/
		for(i=0;i<keywordNum;i++){
			if(listCache.contains(keywords[i])){
				invLists[i]=(byte[]) listCache.get(keywords[i]);
			}
			invLists[i]=openList(keywords[i]);
//			/*limit the cache number*/
//			if (chunkNums[i]<10000){//64*4*10000 bytes
//				listCache.put(keywords[i],invLists[i]);
//			 	setResultCache=false;
//			}
			listCache.put(keywords[i],invLists[i]);
		}

		/* DAAT */
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
		
		//put the heap value to the result array
		for (i=0;i<resultSize;i++){
			if (!rankHeap.isEmpty()){
			Page page=rankHeap.poll();
			UrlDocLen urlInfo=wordmap.urlDocMap.get((page.docId));
			System.out.println(urlInfo.url);
			System.out.println(page.score);
			result[i]=urlInfo.url;
			}
		}
		if(setResultCache==true){
			resultCache.put(keywords, result);
		}
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
		RandomAccessFile readin=new RandomAccessFile("nz2/"+Integer.toString(lexInfo[1]),"r");
//		byte[] metadata=new byte[lexInfo[4]]; //separate metadata and docID with Freq;
//		byte[] invertedlist=new byte[lexInfo[3]-lexInfo[4]]; //in compressed form;
		byte[] invertedlist=new byte[lexInfo[3]];
		readin.seek(lexInfo[2]);
		readin.read(invertedlist);
		readin.close();
		return invertedlist;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		IndexReader reader=new IndexReader();
//		String[] keywords={"Study","English","US"};
		reader.wordmap.setupLexicon("nz2/result/lexicon_index.txt");
////		System.out.println(wordmap.lexiconMap.size());
		reader.wordmap.setupUrl("nz2/result/url_index.txt");
//		reader.result=reader.query(keywords);
		
		//interface
		while(true)
		{
			System.out.print("Please input the query:");
			try{
				//get console input
			    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    String input = bufferRead.readLine();
			    System.out.println(input);
			    String[] keywords = input.split(" ");
				reader.query(keywords,10,10);//(originalWords, pruneSize, resultSize)
				System.out.println();
                //finish
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

	}
	

}
