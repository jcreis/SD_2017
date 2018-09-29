package proxy.server;



import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CacheSystem {
	
	private static final int EXPIRE_CACHE_TIMER = 10000;
	Map<Integer, List<String>> tweets;
	
	public CacheSystem(){
		tweets = new HashMap<Integer, List<String>>();
		t1.start();
	}
	
	public void store(int hash, List<String> list){
		tweets.put(hash, list);
	}

	public boolean hasCached(String keywords) {
		String[] splitedStr = keywords.split("[ //+]");
		int i=0;
		int hash=0;
		while( i < splitedStr.length ) {
			hash += splitedStr[i].hashCode();
		}
		if(tweets.containsKey(hash)){
			return true;
		}
		return false;
	}


	public List<String> getList(String keywords) {
		String[] splitedStr = keywords.split("[ //+]");
		int i=0;
		int hash=0;
		while( i < splitedStr.length ) {
			hash += splitedStr[i].hashCode();
		}
		return tweets.get(hash);
	}
	
	public void clearCache(){
		tweets = new HashMap<Integer, List<String>>();
	}
	
	Thread t1 = new Thread(new Runnable() {			
		public void run() {
			while(true){
				clearCache();
				try{
					Thread.sleep(EXPIRE_CACHE_TIMER);
				}
				
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});
	
	
}
