import java.util.*;

public class VB {
        /*
    VB_Compress(n)
    1 bytes = <>
    2 while true
    3 do PREPEND(bytes, n mod 128)
    4   if n < 128
    5   then BREAK
    6   n = n div 128
    7 bytes[LENGTH(bytes)] += 128
    8 return bytes
*/    
    
    public static List<Byte> VB_Compress(int n) {
        List<Byte> bytes = new ArrayList<Byte>();
        while (true) {
                bytes.add((byte)(n%128));
                if (n < 128) 
                        break;
                n = n / 128;
        }
        byte last  = bytes.get( bytes.size()-1 );
        if(bytes.size() >1)
        	bytes.set( bytes.size()-1, (byte)(last | 0x80) ); //Achieves the effect of += 128. 
		                                                      //This is done like that because 
		                                                      //Java doesn't have unsigned byte.
        List<Byte> tmp = new ArrayList<Byte>();//the highest bit is at the last, convert the order
        for (int i=0; i<bytes.size(); i++) tmp.add(bytes.get(bytes.size()-i-1));
        return tmp;
    }
    
 /*
        VBENCODE(numbers)
        1 bytestream = <>
        2 for each n in numbers
        3 do bytes = VB_Compress(n)
        4       bytestream = EXTEND(bytestream, bytes)
        5 return bytestream   
  */
    
    public static byte[] VBENCODE(List<Integer> numbers) {
        List<Byte> bytestream_l = new ArrayList<Byte>();
        for (Integer n : numbers) {
                List<Byte> bytes = VB_Compress(n);
                bytestream_l.addAll(bytes);
        }
        
        //Convert result to byte[], then return. 
        byte[] bytestream = new byte[bytestream_l.size()];
        for (int i=0; i<bytestream_l.size(); i++) bytestream[i] = bytestream_l.get(i); 
        return bytestream;
    }
    
/*    
    VBDECODE(bytestream)
        1 numbers = <>
        2 n = 0
        3 for i = 1 to LENGTH(bytestream)
        4 do if bytestream[i] < 128
        5       then n = 128*n + bytestream[i]
        6       else n = 128*n + (bytestream[i] - 128)
        7               APPEND(numbers, n)
        8               n = 0
        9 return numbers  
*/    
    
    public static List<Integer> VBDECODE(byte[] bytestream) {
        List<Integer> numbers = new ArrayList<Integer>();
        int n = 0;
        for (int i=0; i<bytestream.length; i++) {
                if ( (bytestream[i] & (byte)(0x80)) == 0 )  
                        n = 128*n + bytestream[i];
                else {
                        byte b = (byte)(bytestream[i] & 0x7F); //Achieves the effect of -= 128. 
                                                                                                                   //This is done like that because 
                                                                                                                   //Java doesn't have unsigned byte.
                        n = 128*n + b;
                        numbers.add(n);
                        n = 0;
                }
        }
        return numbers;
    }
    
//    public static void main(String[] args) throws Exception {
//        //Testing
//        List<Integer> numbers = new ArrayList<Integer>();
//        numbers.add(2); 
////        numbers.add(120); 
//        numbers.add(130);
////        numbers = VB.VBDECODE( VB.VBENCODE(numbers) );
//        byte[] result = VB.VBENCODE(numbers);
//        Binary test1 = new Binary();
//        test1.write(result, "VB_result");
//        System.out.println("Finish");   
//    }
}