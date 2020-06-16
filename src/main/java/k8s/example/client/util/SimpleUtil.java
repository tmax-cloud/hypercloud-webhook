package k8s.example.client.util;

import java.util.List;
import java.util.Map;

public class SimpleUtil {

    public static char[] toHexString(int value, int length, boolean upper) {
        char[] string = new char[length];

        for (int i = 0; i < length; i++) {
            int n = value & 0xF;
            char c = 0;
            switch (n) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                c = (char) (n + '0');
                break;
            case 10:
                c = upper ? 'A' : 'a';
                break;
            case 11:
                c = upper ? 'B' : 'b';
                break;
            case 12:
                c = upper ? 'C' : 'c';
                break;
            case 13:
                c = upper ? 'D' : 'd';
                break;
            case 14:
                c = upper ? 'E' : 'e';
                break;
            case 15:
                c = upper ? 'F' : 'f';
                break;
            }
            string[length - i - 1] = c;
            value >>= 4;
        }
        return string;
    }

    public static void toHexString(int value, int length, char[] string, int startOffset, boolean upper) {
        for (int i = 0; i < length; i++) {
            int n = value & 0xF;
            char c = 0;
            switch (n) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                c = (char) (n + '0');
                break;
            case 10:
                c = upper ? 'A' : 'a';
                break;
            case 11:
                c = upper ? 'B' : 'b';
                break;
            case 12:
                c = upper ? 'C' : 'c';
                break;
            case 13:
                c = upper ? 'D' : 'd';
                break;
            case 14:
                c = upper ? 'E' : 'e';
                break;
            case 15:
                c = upper ? 'F' : 'f';
                break;
            }
            string[startOffset + length - i - 1] = c;
            value >>= 4;
        }
    }
    
    public static String getQueryParameter( Map< String, List<String> > queryMap, String key ){
		if( key == null )
			return null;
		
		if( queryMap.containsKey( key ) )
			return queryMap.get( key ).get(0);
		
		return null;
	}
    
    public static List<String> getQueryParameterArray( Map< String, List<String> > queryMap, String key ){
 		if( key == null )
 			return null;
 		
 		if( queryMap.containsKey( key ) )
 			return queryMap.get( key );
 		
 		return null;
 	}
    
    public static String getString( String str ){
 		return str != null ? str : "";
 	}
}
