package testingStuff;

import java.util.HashMap;
import java.util.Map;

public class MethodsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testMapHashCodeChanging();
	}
	
	private static void testMapHashCodeChanging()
	{
		Map<String, String> map1 = new HashMap<String, String>();
		Map<String, String> map2 = new HashMap<String, String>();
		
		map1.put("1", "2");
		map1.put("3", "4");
		
		Map<Map<String, String>, Boolean> metamap = new HashMap<Map<String,String>, Boolean>();
		metamap.put(map1, true);
		
		map2.put("1", "2");
		map2.put("3", "4");
		
		System.out.println(metamap.get(map2));
	}

}
