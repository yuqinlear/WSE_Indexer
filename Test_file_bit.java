import java.io.BufferedReader;
import java.io.FileInputStream;  
import java.io.FileNotFoundException;
import java.io.FileOutputStream;  
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;  
import java.util.zip.GZIPOutputStream;  
  
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
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public int getDoc1() {
		return doc1;
	}
	public void setDoc1(int doc1) {
		this.doc1 = doc1;
	}
	public int getDoc2() {
		return doc2;
	}
	public void setDoc2(int doc2) {
		this.doc2 = doc2;
	}
	public int getDoc_length() {
		return doc_length;
	}
	public void setDoc_length(int doc_length) {
		this.doc_length = doc_length;
	}
	public String getIp() {
		return Ip;
	}
	public void setIp(String ip) {
		Ip = ip;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
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
            for(int i = 0; i<pages.size(); i++)
            {
            	StringBuilder strbuilder=new StringBuilder();
//            	System.out.println();
            	Parser.parseDoc("http://www.testparser.com",pages.elementAt(i),strbuilder);
            	//write parse result into files
            	if(strbuilder.length() == 0)
        		{
        			document_ID++;
        			continue;
        		}
            	System.out.println("page  "+document_ID+": \n"+strbuilder);
            	
            	String[] lines=strbuilder.toString().split("\n");
            	int contextWeight;
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
        		index.urlDocMap.put(document_ID, new UrlDocLen(file_index.elementAt(i).getUrl(), file_index.elementAt(i).getDoc_length()));
            }//end for pages
            
            System.out.println("start write inverted index into file");
            //start print Inverted index to file
            
	            Iterator  ilter1= index.postingMap.entrySet().iterator();
	      		OutputStreamWriter fout;
	      		try {
	      			fout = new OutputStreamWriter(new FileOutputStream("result/inverted_index_"+file_num+".txt"));
	      			while (ilter1.hasNext())
	      	        {
	      	        	 String post_string = new String();
	      	             Map.Entry entry1 = (Map.Entry) ilter1.next();
	      	             String word = (String) entry1.getKey();
	      	             HashMap posting = (HashMap) entry1.getValue();
	      	             
	      	             Iterator  ilter2= posting.entrySet().iterator();
	      	             post_string += word+" ";
	      	             while (ilter2.hasNext())
	      	             {
	      	            	 Map.Entry entry2 = (Map.Entry) ilter2.next();
	      	                 int docID = (int)entry2.getKey();
	      	                 int freq = (int)entry2.getValue();
//	      	                 Iterator  ilter3= term.contexts.iterator();
	      	                 post_string += docID+" "+freq+" ";
//	      	                 while (ilter3.hasNext())
//	      	                 {
//	      	                	 post_string += new String(new byte [] {(byte)ilter3.next()});
//	      	                 }
//	      	               post_string += " ";
	      	             }
	      	             System.out.println(post_string);
	      	             fout.write(post_string+"\n");
	      	         }
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
		BufferedReader stdoutReader = new BufferedReader(
		         new InputStreamReader(cmdProc.getInputStream()));
		String line,line_without_word;
		int index_position=0,filename=0;
		int line_number=1;
		int[] temp_lexinfo ;
		Binary file_write = new Binary();
		file_write.initial_output(filename);
		while ((line = stdoutReader.readLine()) != null) {
			String word[];
			if ((line_number&0xFFFF)==0xFFFF){
				index_position=0;
//		        fout.close();
				file_write.output.close();
		        filename=line_number;// using line number as the name of index file gives more convenience for observation;
//				fout=new OutputStreamWriter(new FileOutputStream(Integer.toString(filename)));
		        file_write.initial_output(filename);
			}
			word=line.split(" ");
			line_without_word=line.substring(word[0].length());
			temp_lexinfo=index.lexiconMap.get(word[0]);
			//need edit
			index.inSertIntoLexMap(word[0],filename,index_position);
			//add compress and write into binary file
			if(temp_lexinfo==null){// this word inserted first time after merge;
			//compress line_without_word and write into file
//				fout.write("\n"+line_without_word); // or directly append this line;
				index_position+=line_without_word.length()+1;			
			}else{
				//compress line_without_word and write into file
//				fout.write(line_without_word); //change to new line when meet a new word;
				index_position+=line_without_word.length();			
			}
			line_number++;
		}
//        fout.close();
		file_write.output.close();
		
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
			OutputStreamWriter fout2 = new OutputStreamWriter(new FileOutputStream("result/url_index.txt"));
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
			fout2.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}//end print url_index to disk
		
		//start print lexicon index to file
		System.out.println("start write lexicon index into file");
		ilter1= index.lexiconMap.entrySet().iterator();
		try {
			OutputStreamWriter fout2 = new OutputStreamWriter(new FileOutputStream("result/lexicon_index.txt"));
			while (ilter1.hasNext())
	        {
				String lexicon_string = new String();//store every line of url_index
				Map.Entry entry1 = (Map.Entry) ilter1.next();
	            String word = (String) entry1.getKey();
	            int[] tf_offset = (int[]) entry1.getValue();//term frequence and offset of inverted file
//	            lexicon_string += word+" "+tf_offset.elementAt(0)+" "+tf_offset.elementAt(1);
	            lexicon_string += word+" "+tf_offset[0]+" "+tf_offset[1]+" "+tf_offset[2];
	            fout2.write(lexicon_string+"\n");
	        }
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
			            
			            int n;
			            String a = new String();
			            int i=0;
			            buffer = new char[file_index.elementAt(number).getDoc_length()];
			            while ((n = gzipReader.read(buffer, 0, file_index.elementAt(number).getDoc_length())) != -1)
			            {
			            	System.out.println("page"+number+":");
			            	a = String.valueOf(buffer);
			            	pages.add(a);
			            	number ++;
			            	if(number==file_index.size())
			            	{
			            		System.out.println("finish");
			            		break;
			            	}
			            	
			            	buffer = new char[file_index.elementAt(number).getDoc_length()];
			            	
			            }
			                
		                gzipReader.close();
		                
			        } catch (Exception ex){  
			            System.err.println(ex.toString());  
			           
			        }  
    	return pages;
    } 
}