import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;  
import java.io.FileNotFoundException;
import java.io.FileOutputStream;  
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;  
  
class Index
{
	String url;
	int doc1;//not sure the meaning
	int doc2;//not sure the meaning
	int doc_length;
	String Ip;
	String status;
	public Index(String url, int doc1, int doc2, int doc_length, String ip,
			String status) {
		super();
		this.url = url;
		this.doc1 = doc1;
		this.doc2 = doc2;
		this.doc_length = doc_length;
		Ip = ip;
		this.status = status;
	}
	public Index(String url, int doc_length, String ip, String status) {
		super();
		this.url = url;
		this.doc_length = doc_length;
		Ip = ip;
		this.status = status;
	}
}

public class Test_file_bit {  
	
    public static void main(String[] args) throws IOException{  
    	WordMap index = new WordMap();
    	//add a loop read all the files(data --- index)
    	int document_ID = 0;
    	for(int file_num=0; file_num< 83; file_num++)
    	{
    		String filename_data = "nz2_merged/"+file_num+"_data";
            String filename_index = "nz2_merged/"+file_num+"_index";
            Vector<Index> file_index = read_index(filename_index);  
            Vector<String> pages = read_page(filename_data, file_index);             
            index.postingMap = new TreeMap<String,HashMap<Integer,Integer>> ();
            
            ///parse a page and insert into Postings;
            for(int i = 0; i<pages.size(); i++)
            {
            	StringBuilder strbuilder=new StringBuilder();
//            	System.out.println();
            	Parser.parseDoc(file_index.elementAt(i).url,pages.elementAt(i),strbuilder);
            	//write parse result into files
            	if(strbuilder.length() == 0)
        		{
        			document_ID++;
        			continue;
        		}
            	System.out.println("page  "+document_ID+": \n"+strbuilder);
            	
            	String[] lines=strbuilder.toString().split("\n");
        		for (String line:lines){
        			String[] word=line.split(" ");
        			if(word.length == 2)
        			{
            			index.inSertIntoPostingMap(word[0], document_ID,WordMap.contextWeight(word[1]));//insert into 
        			}
        			//add to lexicon file_index and Inverted file_index
        		}
        		document_ID++;
        		//add url and document length to url index, every document once
        		index.urlDocMap.put(document_ID, new UrlDocLen(file_index.elementAt(i).url, file_index.elementAt(i).doc_length));
            }//end for pages
            
//            System.out.println("start write inverted index into file");
            //start print Inverted index to file
            
	            Iterator  ilter1= index.postingMap.entrySet().iterator();
	            BufferedWriter fout;
	      		try {
	      			fout = new BufferedWriter(new FileWriter("result/inverted_index_"+file_num+".txt"));
	      			while (ilter1.hasNext())
	      	        {
	      	             Map.Entry entry1 = (Map.Entry) ilter1.next();
	      	             String word = (String) entry1.getKey();
	      	             HashMap posting = (HashMap) entry1.getValue();
	      	             
	      	             Iterator  ilter2= posting.entrySet().iterator();
	      	             String post_string = word+" ";
	      	             while (ilter2.hasNext())  // concatenate all docID and freq for this word;
	      	             {
	      	            	 Map.Entry entry2 = (Map.Entry) ilter2.next();
	      	                 int docID = (int)entry2.getKey();
	      	                 int freq = (int)entry2.getValue();
	      	                 post_string += docID+" "+freq+" ";
	      	             }
	      	             fout.write(post_string+"\n");
	      	         }
	      			fout.flush();
	      			fout.close();
	      		} catch (IOException e) {
	      			// TODO Auto-generated catch block
	      			e.printStackTrace();
	      		}//end print Inverted index into file inverted_index_i.txt(i from 0 to 82)
	      	System.out.println("start write inverted index into file");
    	}//end for documents
    	
    	//merge inverted index
    	//linux sort all the files
    	//generate linux sort command
    	String command = new String("sort -k1,1d -k2,2n");
    	for(int i=0; i<83; i++)
    	{
    		command += " result/inverted_index_"+i+".txt";
    	}
    	System.out.println(command);
		Process cmdProc = Runtime.getRuntime().exec(command);

//		OutputStreamWriter fout = new OutputStreamWriter(new FileOutputStream("0"));
		OutputStream fout = new BufferedOutputStream(new FileOutputStream("0"),128*1024);
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
		String line, previousWord;		
		int offset=0,startOffset=0,filename=0,chunkNum=0,wordTotalNum=0,docFreq;
		int[] temp_lexinfo,chunk ;
		String word[]={" "};//initialize the first word;
		byte[] metadata, compressedChunk = null;
		List<Integer> docIDsInList=new ArrayList<Integer>();
		List<Byte> freqsInList=new ArrayList<Byte>();	
		//read lines after merge
		while ((line = stdoutReader.readLine()) != null) {
			previousWord=word[0];
			word=line.split(" "); //word[0]=word,word[1]=docId,word[2]=freq,word[3]=docId, word[4]=freq,....
			temp_lexinfo=index.lexiconMap.get(word[0]);    		
			if(temp_lexinfo==null){// if we read a new word, we make up the inverted list of the last word;
				//make chunks for the previous word;
				docFreq=docIDsInList.size(); // the docID number of the given word;
				chunkNum=docFreq/64 +1;
				startOffset=offset;
				metadata = new byte[docFreq/64 +1];
				offset += chunkNum;				
				for(int i=0; i<chunkNum; i++)
				{
					if((i+1)*64 > docFreq)
					{
						chunk=new int[docFreq-i*64];
						chunk[0]=docIDsInList.get(i*64);
						for (int j=i*64+1;j<docFreq;j++){
							chunk[j-i*64]=docIDsInList.get(j)-docIDsInList.get(j-1);
						}
						compressedChunk = VB.VBENCODE(chunk);
						offset+=compressedChunk.length;
						metadata[i]=(byte)compressedChunk.length;  //metadata store the length(bytes) of the chunk
//						lengthByBytes+=metadata[i];
					}
					else
					{
						chunk=new int[64];
						chunk[0]=docIDsInList.get(i*64);
						for (int j=i*64+1;j<(i+1)*64;j++){
							chunk[j-i*64]=docIDsInList.get(j)-docIDsInList.get(j-1);
						}
						compressedChunk = VB.VBENCODE(chunk);
						offset+=compressedChunk.length;
						metadata[i]=(byte)compressedChunk.length;  //metadata store the length(bytes) of the chunk
//						lengthByBytes+=metadata[i];
					}
				}
				offset+=freqsInList.size();// add the size of freq chunks;
				/***
				 * write chunks into file
				 */
				fout.write(metadata);// 1. write meta data;
				fout.write(compressedChunk);// 2.write compressed docId chunks
				byte[] termfreqs = new byte[freqsInList.size()];
				for(int i=0; i<freqsInList.size(); i++){ 
					termfreqs[i] = freqsInList.get(i).byteValue();
				}
				fout.write(termfreqs); //3.write doc frequency right after docId chunks
				/*insert the lexinfo to lexicon map*/
				index.inSertIntoLexMap(previousWord,docFreq,filename,startOffset,offset-startOffset,chunkNum);
				/*check out inverted index file*/
				wordTotalNum++;
				if ((wordTotalNum&0xFFF)==0xFFF){// store 4096 words in one index file;
					offset=0;
					fout.flush();//force to write out the buffer;
			        fout.close();
//					file_write.output.close();			        
			        filename=wordTotalNum;// using words number as the name of index file gives more convenience for observation;
					fout = new BufferedOutputStream(new FileOutputStream(Integer.toString(filename)),128*1024);
//			        file_write.initial_output(filename);
				}
				//make new docIDsInList and freqsInList
				docIDsInList=new ArrayList<Integer>();
				freqsInList=new ArrayList<Byte>();			
				for (int i=0;i<((word.length-1)>>>2);i++){
					docIDsInList.add(Integer.parseInt(word[i*2+1]));
					freqsInList.add((byte)Integer.parseInt(word[i*2+2]));
				}
			}else{// when the posting belonging to the same word, add them into doc id list and freq list;
				for (int i=0;i<((word.length-1)>>>2);i++){
					docIDsInList.add(Integer.parseInt(word[i*2+1]));
					freqsInList.add((byte)Integer.parseInt(word[i*2+2]));
				}
			}
		}
		//TODO write the last word!!
        fout.close();
		
		//print error of linux sort
		BufferedReader stderrReader = new BufferedReader(
		         new InputStreamReader(cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
			System.out.println(line);
		}

		int retValue = cmdProc.exitValue();
		System.out.println(retValue);
    	
        
        
		//start print url index to file
		System.out.println("start write url index into file");
		Iterator ilter1= index.urlDocMap.entrySet().iterator();
		try {
			BufferedWriter fout2 = new BufferedWriter(new FileWriter("result/url_index.txt"));
			while (ilter1.hasNext())
	        {
				String url_string = new String();//store every line of url_index
				Map.Entry entry1 = (Map.Entry) ilter1.next();
	            int id = (int) entry1.getKey();
	            UrlDocLen url = (UrlDocLen) entry1.getValue();
				url_string += id+" "+url.url+" "+url.docLen; 
				System.out.println(url_string);
	            fout2.write(url_string+"\n");
	        }
			fout2.flush();
			fout2.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}//end print url_index to disk
		
		//start print lexicon index to file
		System.out.println("start write lexicon index into file");
		ilter1= index.lexiconMap.entrySet().iterator();
		String theWord;
		try {
//			OutputStreamWriter fout2 = new OutputStreamWriter(new FileOutputStream("result/lexicon_index.txt"));
			BufferedWriter fout2 = new BufferedWriter(new FileWriter("result/lexicon_index.txt"));
			while (ilter1.hasNext())
	        {
				String lexicon_string = new String();//store every line of url_index
				Map.Entry entry1 = (Map.Entry) ilter1.next();
				theWord = (String) entry1.getKey();
	            int[] lexinfo = (int[]) entry1.getValue();//term frequence and offset of inverted file
//	            lexicon_string += word+" "+tf_offset.elementAt(0)+" "+tf_offset.elementAt(1);
	            lexicon_string = theWord+" "+lexinfo[0]+" "+lexinfo[1]+" "+lexinfo[2]+" "+lexinfo[3]+" "+lexinfo[4];
	            fout2.write(lexicon_string+"\n");
	        }
			fout2.flush();
			fout2.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}//end print url_index to disk
        System.out.println("done");
        //end loop
    }  
    
    public static Byte convertStrToByte(String str){
		return (byte)str.charAt(0);		
	}
    
    public static Vector<Index> read_index(String filename) {
    	Vector<Index> file_index = new Vector<Index>();
    	try {  
              
            BufferedReader gzipReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
            String buf;
            while((buf=gzipReader.readLine()) != null)
            {
            	System.out.println(buf);
            	String[] ss = buf.split(" ");
            	Index temp = new Index(ss[0], Integer.parseInt(ss[3]), ss[4], ss[6]);
            	file_index.add(temp);
            }
            gzipReader.close();
        } catch (Exception ex){  
            System.err.println(ex.toString());  
        }  
        return file_index;
    }
    
    
    //should edit
    public static Vector<String> read_page(String filename, Vector<Index> file_index) {  
        
    	int number=0;
    	Vector<String> pages = new Vector<String>();
		        try {  
		             
			            BufferedReader gzipReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
			              
			            char[] buffer;
			            String a = new String();
			            buffer = new char[file_index.elementAt(number).doc_length];
			            while ((gzipReader.read(buffer, 0, file_index.elementAt(number).doc_length)) != -1)
			            {
//			            	System.out.println("page"+number+":");
			            	a = String.valueOf(buffer);
			            	pages.add(a);
			            	number ++;
			            	if(number==file_index.size())
			            	{
//			            		System.out.println("finish");
			            		break;
			            	}			            	
			            	buffer = new char[file_index.elementAt(number).doc_length];			            	
			            }
			                
		                gzipReader.close();
		                
			        } catch (Exception ex){  
			            System.err.println(ex.toString());  
			           
			        }  
    	return pages;
    } 
}
