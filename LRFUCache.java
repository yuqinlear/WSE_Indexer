import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class LRFUCache<K,V>implements Cache<K,V> {

	
	public static void main(String[] args) {
		LRFUCache<Integer, String> cache=new LRFUCache<Integer,String>(5,1000);
		cache.put(1, "1");
		cache.put(2, "2");
		cache.put(3, "3");
		cache.put(4, "4");
		cache.get(4);
		cache.get(3);
		cache.get(3);
		cache.put(5, "5");
		cache.put(6, "6");
		cache.put(7, "7");
		cache.put(8, "8");
//		cache.put(9, "9");

		System.out.println(cache.queue.poll().value);
		System.out.println(cache.queue.poll().value);
		System.out.println(cache.queue.poll().value);
		System.out.println(cache.queue.poll().value);
		System.out.println(cache.queue.poll().value);
	}
	/**LRFU combine the advantage of LFU and RFU
	 * @param maxSize  the size of the cache
	 * @param factor a number greater or equal to 0  to adjust the time weight, 
	 *  when factor is unlimited large, it is FLU
	 *  when the factor decrease =>higher time weight=>lower weight to previous scores,thus it moves closer to RLU;
	 */
	public LRFUCache(int maxSize,int factor){
		this.maxSize=maxSize;
		this.factor=factor+1; //smooth 0 to 1;
		cacheMap=new HashMap<K,Item>(maxSize);
		queue=new PriorityQueue<Item>(maxSize,new Comparator<Item>(){
			public int compare(Item it1, Item it2){
				if(it1==it2){return 0;}
				if(it1.score>it2.score) {return 1;}
				else if(it1.score<it2.score){return -1;}
				else return 0;
			}
		});
	}
	
	public synchronized void put(K key,V value){
		//
        if(cacheMap.containsKey(key)) {
            Item item = cacheMap.remove(key);
            queue.remove(item);
		}
        
		if (queue.size()>=maxSize){
			Item item=queue.remove();
			cacheMap.remove(item.key);
		}
		Item item=new Item(key,value);
		cacheMap.put(key, item);
		queue.add(item);
	}
	
	public boolean contains(K key){
		return cacheMap.containsKey(key);
	}
	
    public V get(K key) {
    	int interval,timeWeight;
        if(cacheMap.containsKey(key)) {
                synchronized(this) {
                        if(cacheMap.containsKey(key)) {
                                Item item = cacheMap.get(key);
                                //LRFU  when factor=1, it is almost RLU, when factor=unlimited big, it is almost FLU;
                                interval=globalTime-item.lastUseTime;//interval betw. current time and lastUseTime 
                                item.lastUseTime = globalTime++;
                                timeWeight=(int) (interval/factor)+1;
                                item.score = item.score/timeWeight + 1; //timeWeight
                                queue.remove(item);
                                queue.add(item);
                                return item.value;
                        }
                        else return null;
                }
        }
        else return null;
    }

	
	
	private int maxSize=1000;
	private int globalTime=0; //a global time counter
	private int factor;
	private PriorityQueue<Item> queue;
	private Map<K,Item> cacheMap;
	
	private class Item{
		K key;
		V value;
		int score=0;
		int lastUseTime=0;
		public Item(K key,V value){
			this.key=key;
			this.value=value;
			lastUseTime=globalTime++;
		}
	}

}
