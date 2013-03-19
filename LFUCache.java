import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class LFUCache<K,V> {

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
		if(cacheMap.containsKey(key)){
			return;
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
	
	public V get(K key){
		if(cacheMap.containsKey(key)){
			synchronized(this){
				if(cacheMap.containsKey(key)){
					Item item=cacheMap.get(key);
					item.lastUseTime=time++;
					item.useCount++;
					queue.remove(item);
					queue.remove(item);
					queue.add(item);
					return item.value;
				}
				return null;
			}
		}else{
			return null;
		}
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
