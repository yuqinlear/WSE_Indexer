import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class LFUCache<K,V> implements Cache<K,V>{

	
	public static void main(String[] args) {
		LFUCache<Integer, String> cache=new LFUCache<Integer,String>(5);
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
	
	public LFUCache(int maxSize){
		this.maxSize=maxSize;
		cacheMap=new HashMap<K,Item>(maxSize);
		queue=new PriorityQueue<Item>(maxSize,new Comparator<Item>(){
			public int compare(Item it1, Item it2){
				if(it1==it2){return 0;}
				if(it1.useCount>it2.useCount) {return 1;}
				else if(it1.useCount<it2.useCount){return -1;}
				else if(it1.lastUseTime>it2.lastUseTime){return 1;}
				else if(it1.lastUseTime<it2.lastUseTime){return -1;}
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
        if(cacheMap.containsKey(key)) {
                synchronized(this) {
                        if(cacheMap.containsKey(key)) {
                                Item item = cacheMap.get(key);
                                item.lastUseTime = time++;
                                item.useCount = item.useCount + 1;
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
	private int time=0;
	private PriorityQueue<Item> queue;
	private Map<K,Item> cacheMap;
	
	private class Item{
		K key;
		V value;
		int useCount=0;
		int lastUseTime=0;
		public Item(K key,V value){
			this.key=key;
			this.value=value;
			lastUseTime=time++;
		}
	}

}
