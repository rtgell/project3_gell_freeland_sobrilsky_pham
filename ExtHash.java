

/*******************************************************************************
 * @file ExtHash.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/*******************************************************************************
 * This class provides hash maps that use the Extendable Hashing algorithm.  Buckets
 * are allocated and stored in a hash table and are referenced using directory dir.
 */
public class ExtHash <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /***************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int  nKeys;
        K [] key;
        V [] value;
        int depth;
        @SuppressWarnings("unchecked")
        Bucket (int d)
        {
            nKeys = 0;
            depth = d;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
        } // constructor
        int getDepth(){
        	return depth;
        }
    } // Bucket inner class
    private class Entry implements Map.Entry <K, V>
    {
    	    K key;
    	    V value;
    	    Entry(K k, V v){
    	    	    key = k;
    	    	    value = v;
    	    }
    	    public V setValue(V value){
    	    	    this.value = value;
    	    	    return value;
    	    }
    	    public V getValue(){
    	    	    return value;
    	    }
    	    public K setKey(K key){
    	    	    this.key = key;
    	    	    return key;
    	    }
    	    public K getKey(){
    	    	    return key;
    	    }
    	    public String toString(){
    	    	    return "[" + key + "," + value + "]";
    	    }
    }
    /** The hash table storing the buckets (buckets in physical order)
     */
    private final List <Bucket> hTable;

    /** The directory providing access paths to the buckets (buckets in logical oder)
     */
    private final List <Bucket> dir;

    /** The modulus for hashing (= 2^D) where D is the global depth
     */
    private int mod;

    /** The number of buckets
     */
    private int nBuckets;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /***************************************************************************
     * Construct a hash table that uses Extendable Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of buckets (a power of 2, e.g., 4)
     */
    public ExtHash (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <Bucket> ();   // for bucket storage
        dir    = new ArrayList <Bucket> ();   // for bucket access
        mod    = nBuckets = initSize;
        for(int i = 0; i < initSize; i++){
        	Bucket b = new Bucket(mod);
        	hTable.add(b);
        	dir.add(b);
        }
    } // ExtHash

    /***************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <Map.Entry <K, V>> ();
        for(int i = 0; i < hTable.size(); i++){
        	Bucket buck = hTable.get(i);
        	for(int j = 0; j < buck.nKeys; j++){
        		enSet.add(new Entry(buck.key[j], buck.value[j]));
        	}
        }    
    
        return enSet;
    } // entrySet

    /***************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int    i = h (key);
        Bucket b = dir.get (i);
        V ret = null;
        for(int j = 0; j < b.nKeys; j++){
        	if(b.key[j].equals(key)){
        		ret = b.value[j];
        	}
        }

        return ret;
    } // get

    /***************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        int    i = h (key);
        Bucket b = dir.get (i);

        for(int j = 0; j < b.nKeys; j++){
        	if(b.key[j].equals(key)){
        		b.value[j] = value;
        		return null;
        	}
        }
        if(b.nKeys < SLOTS){
        	b.key[b.nKeys] = key;
        	b.value[b.nKeys] = value;
        	b.nKeys++;
        	return null;
        }
        else{
        	expand(b, key, value);
        	return null;
        }
    } // put
    private void expand(Bucket b, K key, V value){
    	    int depth;
    	    if(b.getDepth() == mod){
    	    	    int size = dir.size();
    	    	    for(int i = 0; i < size; i++){
    	    	    	    dir.add(dir.get(i));
    	    	    }
    	    	    int i = h(key);
    	    	    int j = i+mod;
    	    	    mod = mod*2;
    	    	    depth = mod;
    	    	    Bucket buck1 = new Bucket(mod);
    	    	    Bucket buck2 = new Bucket(mod);
    	    	    nBuckets++;
    	    	    for(int k = 0; k < hTable.size(); k++){
    	    	    	    if(hTable.get(k).equals(b)){hTable.remove(k); k = hTable.size();}
    	    	    }
    	    	    dir.remove(i);
    	    	    dir.add(i, buck1);
    	    	    dir.remove(j);
    	    	    dir.add(j, buck2);
    	    	    hTable.add(buck1);
    	    	    hTable.add(buck2);
    	    }
    	    else{
    	    	    int d = b.getDepth();
    	    	    int i = 0;
    	    	    for(int l = 0; l < dir.size(); l++){
    	    	    	    if(b.equals(dir.get(l))){
    	    	    	       i = l;
    	    	    	       l = dir.size();
    	    	    	    }
    	    	    }
    	    	    Bucket buck1 = new Bucket(d*2);
    	    	    Bucket buck2 = new Bucket(d*2);
    	    	    int k = 0;
    	    	    for(int l = 0; l < hTable.size(); l++){
    	    	    	    if(hTable.get(l).equals(b)){hTable.remove(l); l = hTable.size();}
    	    	    }
    	    	    hTable.add(buck1);
    	    	    hTable.add(buck2);
    	    	    while(i < mod){
    	    	    	    if(k % 2 == 0){
    	    	    	    	    dir.remove(i);
    	    	    	    	    dir.add(i, buck1);
    	    	    	    }
    	    	    	    else{
    	    	    	    	    dir.remove(i);
    	    	    	    	    dir.add(i, buck2);
    	    	    	    }
    	    	    	    k++;
    	    	    	    i = i + d;  	    
    	    	    }
    	    }
    	    put(key, value);
    	    for(int k = 0; k < b.nKeys; k++){
    	    	    put(b.key[k], b.value[k]);
    	    }
    }
    /***************************************************************************
     * Return the size (SLOTS * number of buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * nBuckets;
    } // size

    /***************************************************************************
     * Print the hash table.
     */
    public void print(){
        out.println ("Hash Table (Extendable Hashing)");
        out.println ("-------------------------------------------");
        for(int i = 0; i < dir.size(); i++){
        	Bucket b = dir.get(i);
        	System.out.println("Bucket " + i + ":" + " Depth:" + b.getDepth());
        	for(int j = 0; j < b.nKeys; j++){
        		System.out.println("Key:" + b.key[j] + " value:"+b.value[j] );
        	}
        }
        out.println ("-------------------------------------------");
    } // print

    /***************************************************************************
     * Hash the key using the hash function.
     * @param key  the key to hash
     * @return  the location of the directory entry referencing the bucket
     */
    private int h (Object key)
    {
        return key.hashCode () % mod;
    } // h

    /***************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {                                       
        ExtHash <Integer, Integer> ht = new ExtHash <Integer, Integer> (Integer.class, Integer.class, 2);
        int nKeys = 30;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 1) { ht.put (i, i * i);}
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // ExtHash class

