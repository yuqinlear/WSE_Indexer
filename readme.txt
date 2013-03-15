1.	How it works:
1)	Unzip index file  by function read_index(filename_index), it will return indices about each page¡¯s offset;
2)	Read pages by function read_page(filename_data,file_index) using the indices we just read;
3)	For each page, parse the content out into a string array which contains the association of a word with its context;
4)	Store the parsed out data into class WordMap which contains our 3 main index maps, we will talk about the details about these maps below;
5)	For every data file,  we put all data into maps, we output the inverted_index maps into a  file. The output data have been sorted since the tree map is an ordered structure;  
6)	Merge every small inverted_index output files into large files by linux sort and generate the inverted indices, the inverted indices file will be a couple of files based on the line number in it(every line in inverted indices is a posting ). Because we use nz_2 data, the number output inverted_index files is small, we just use linux sort once to sort all of these files.;
7)	At last, we output lexicon_index and url_index  into files;

2.	Data Structure:
We first tried to build a prefix tree with the numeric-alphabetic symbol as the node, and store the doc IDs of each word as a linkedlist in each tree node; and store all contexts of each Doc of each Word as a linkedlist in each Doc ID node in the tree node. It really fast to build the tree, but we find it is hard to traverse this prefix tree and the space waste for each node is too much;
Finally, we build a class called WordMap which contains three maps as required output data (postingMap, urlDocMap, and urlDocMap). All maps are using  TreeMap provided by java, which is a underlaid  red-black tree. Thus we can get a sorted data when we output these data after insertion;
1) urlDocMap:
	 1. Map a document ID to a UrlDocLen object;
	 2. A UrlDocLen object stores the url and document length of the Doc ID;
2) lexiconMap:
1.Map a word to a 2 dimension array (extensible);
2.int[0] store the document frequency, and int[1] store the address of the indices(postings);

3) postingMap:
	1.Map a word to all postings belonging to it; 
	2. Map TermInDoc objects to a specific doc in the postings of the specific word;
	3. There are term frequency and all contexts(LinkedList) stored for the specific document of the  specific word;
4 ) public void inSertIntoUrlDocMap(Integer docId,String url, Integer docLen):
insert the url and document length into the urlDocMap for the give doc ID;
5) public void inSertIntoLexMap(String word):
insert a word into lexiconMap,  if it is existed, increment its document frequency;
6)public void inSertIntoPostingMap(String word,Integer docId, Byte context):
insert a posting into postingMap for the give word; tempDocMap is a map from a docId of the give word to a TermInDoc object; TermInDoc object store all contexts and the term frequency of a given word in a give DocId; 
3. Programming in java; We used the java parser provided at the websites; 


4.Environment 
   Operating system: Ubuntu 11.10 (Virtual Machine), win7 64X(native) ;
   JDK 1.7, Eclipse June

5. output files introduction
We set the capacity of 2^16 postings per file for inverted list after merge.
1)Inverted list:
Format: one line for each word.
Each line: Document ID + Term Frequency + context ;
2)url_index.txt: 
Format: document ID + url + the length of page;
3)lexicon_index.txt :
Format:  word  + document frequency +filename+ offset;

6.  result:
We tested over nz2.tar which cost 3-4 minutes, ( 2G RAM in Virtual Machine)
nz2.tar: 126M
built inverted list: 5M

7. planned improvement:
1) compact the repeat contexts;
2) compress posting by using vbyte encoding.
   
