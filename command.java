import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class command {

	/**
	 * @param args
	 */
	
	static double avg_length = 0;
	
	public static void main(String[] args) {
		//get lexicon index from disk
		String lexicon_file = "";
		Map<String,int[]> lexicon_index = setupLexicon(lexicon_file);
		//get url index from disk
		String url_file = "";
		Map<Integer, UrlDocLen> url_index = setupUrl(url_file);
		
		
		//interface
		while(true)
		{
			System.out.print("Please input the query:");
			try{
				//get console input
			    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			    String input = bufferRead.readLine();
			    System.out.println(input);
                String[] words = input.split(" ");
                //query
                List<String> results = query(words);//result are urls, 10
                for(int i=0; i < results.size(); i++)
                {
                	System.out.println(results.get(i));
                }
                //finish
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

	}
	
	
	//put lexicon index in memory
	public static Map<String,int[]> setupLexicon(String lexicon_file)
	{
		Map<String, int[]> lexicon = new HashMap<String,int[]>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(lexicon_file));
			String line;
			while ((line = in.readLine()) != null) 
			{
				String words[] = line.split(" ");
				//insert into lexicon
			}
				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return lexicon;
	}
	
	//put url index in memory
		public static Map<Integer, UrlDocLen> setupUrl(String url_file)
		{
			Map<Integer, UrlDocLen> url = new HashMap<Integer, UrlDocLen>();
			int length = 0;
			try {
				BufferedReader in = new BufferedReader(new FileReader(url_file));
				String line;
				while ((line = in.readLine()) != null) 
				{
					String words[] = line.split(" ");
					length += Integer.parseInt(words[2]);
					//insert into url
					url.put(Integer.parseInt(words[0]), new UrlDocLen(words[1],Integer.parseInt(words[2])));
				}
				//calculate the average length of documents in the collection
				avg_length = (double)length/url.size();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return url;
		}
	
	
	//input: query words  output: top-10 urls
	public static List<String> query(String[] words)
	{
		List<String> url = new ArrayList<String>();
		//Map<String, head> head;
		//scan if in the cache
		for(int i=0; i<words.length; i++)
		{
			//offset = check_in_cache();
			//maintain cache
		}
		//scan if in the lexicon
		for(int i=0; i<words.length; i++)
		{
			//offset = check_in_lexicon();
			
			
			//maintain cache
		}
		
		//
		/*
		 * Map<DocID, score> TAAT;
		 * while(head.getValue(words[0]).hasNext())
		 * {
		 * 		//uncompress every DocID in head[0]
		 *      //check whether the other words in the DocID
		 *         use DAAT with chunkwise
		 *      if(true)
		 *      {
		 *      	double bm =  BM25;
		 *          TAAT.put(DocID, bm);
		 *      }
		 *      else
		 *        next;
		 * }
		 * TAAT.sort();
		 * if(TAAT.size>= 10)
		 * {
		 * 		for(int i=0; i<10; i++)
		 * 			url.add(TAAT.geturl);
		 * }
		*/
		
		return url;
		
	}

}
