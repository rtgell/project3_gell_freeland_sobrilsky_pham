/*******************************************************************************
 * @file BpTree.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/*******************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTree <K extends Comparable <K>, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /***************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER - 1);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER);
            } // if
        } // constructor
    } // Node inner class

    /****************************************************
     *class Entry implement Map.Entry 
     *@author Minh Pham
     */
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

    /** The root of the B+Tree
     */
    private Node root;      // Minh Pham: change private final to private only, spliting root cause the tree grows up

    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    /***************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTree (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true);
    } // BpTree

    /***************************************************************************
     * Return null to use the natural order based on the key type.  This requires
     * the key type to implement Comparable.
     */
    public Comparator <? super K> comparator () 
    {
        return null;
    } // comparator

    /***************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     * @author Minh Pham
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <Map.Entry <K, V>> ();
        
        Node n=root;
        if(n==null) return enSet;
        Queue<Node> myQueue = new LinkedList<Node>();
        myQueue.add(n);
        while(!myQueue.isEmpty()){
               Node temp = myQueue.poll();
               if(temp.isLeaf){
        		for(int i=0; i<temp.nKeys; i++){
        			enSet.add(new Entry(temp.key[i], (V) temp.ref[i]));
        		}
        	}
        	else{
        		for(int i=0; i< temp.nKeys+1; i++){
        			myQueue.add((Node)temp.ref[i]);
        		}
        	}
        }
            
        return enSet;
    } // entrySet


    /***************************************************************************
     * Return a TreeMap containing all the entries as pairs of keys and values.
     * @return  the the TreeMap
     * @author Minh Pham
     */
    private TreeMap<K,V> TreeMap ()
    {
        TreeMap<K, V> map = new TreeMap<K, V>();
        Node n=root;
        Queue<Node> myQueue = new LinkedList<Node>();
        myQueue.add(n);
        while(!myQueue.isEmpty()){
               Node temp = myQueue.poll();
               if(temp.isLeaf){
        		for(int i=0; i<temp.nKeys; i++){
        			map.put(temp.key[i], (V) temp.ref[i]);
        		}
        	}
        	else{
        		for(int i=0; i< temp.nKeys+1; i++){
        			myQueue.add((Node)temp.ref[i]);
        		}
        	}
        }            
        return map;
    } // TreeMap

    /***************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /***************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        insert (key, value, root, null);
        return null;
    } // put

    /***************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     * @author Minh Pham
     */
    public K firstKey () 
    {
         Node n = root;
             while(n!=null && !n.isLeaf){
    		n = (Node) n.ref[0];
    	  }
    	  return n.key[0];
    } // firstKey

    /***************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     * @author Minh Pham
     */
    public K lastKey () 
    {
         Node n = root;
             while(n!=null && !n.isLeaf){
    		n = (Node) n.ref[n.nKeys];
    	  }
    	  return n.key[n.nKeys-1];
    } // lastKey

    /***************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     * @author Zachary Freeland
     */
    public SortedMap <K,V> headMap (K toKey)
    {

	SubMap sub = new SubMap(this, null, false, toKey, false);
        return sub;

    } // headMap

    /***************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     * @author Zachary Freeland
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {

	SubMap sub = new SubMap(this, fromKey, true, null, false);
        return sub;

    } // tailMap

    /***************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     * @author Zachary Freeland
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {

	SubMap sub = new SubMap(this, fromKey, true, toKey, false);
        return sub;

    } // subMap

    /***************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     * @author Minh Pham
     */
    public int size ()
    {
         // level order tree traversal-> store nodes in a queue
        // add node.nKeyso to sum if it is a leaf
        int sum = 0;
        Node n =root;
        if(root==null) return 0;
        Queue<Node> myQueue = new LinkedList<Node>();   
        myQueue.add(n);
        while(!myQueue.isEmpty()){
               Node temp = myQueue.poll();
               if(temp.isLeaf){
        		sum+=temp.nKeys;
        		//System.out.println("count");
        	}
        	else{
        		for(int i=0; i< temp.nKeys+1; i++){
        			myQueue.add((Node)temp.ref[i]);
        		}
        	}
        }

        return  sum;
    } // size

    /***************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        out.println ("BpTree");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

        out.println ("-------------------------------------------");
    } // print

    /***************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param ney  the current node
     * @author Minh Pham: modify find()
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo (k_i) <= 0) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i] : null;
                } else {
                    return (key.equals (k_i)) ? find (key, (Node) n.ref [i+1]): find (key, (Node) n.ref [i]);          // the line has been modified to compatible to my code
                } // if
            } // if
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
    } // find

    /***************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     * @author Minh Pham: do not use recursive
     */
    private void insert (K key, V ref, Node n, Node p)
    {
       int pos=0;
           Stack<Node> myStack = new Stack();					// use stack to store the path from root to the leaf node being inserted key and ref
    	while(!n.isLeaf){
    		pos=0;
    		while(pos<n.nKeys && n.key[pos].compareTo(key)<=0) pos++;
    		p = n;
    		myStack.add(p);
    		n = (Node) n.ref[pos];    		
    	}// after while loop: n points to a leaf
    	
    	pos=0;
	while(pos<n.nKeys && n.key[pos].compareTo(key)<0) pos++;
    	// get position to insert

	if(pos<n.nKeys && n.key[pos].compareTo(key)==0){
              System.out.println ("BpTree:insert: attempt to insert duplicate key = " + key);
              n.ref[pos]=ref;
       return;
	}
		
	// check number of entries
    	if(n.nKeys==ORDER-1){									// n has ORDER-1 entries -> insertion causes node n splitting
    		Node newNode = split(key, ref, n);
			
    		if(p==null);										// case: n points to root -> create new root
    		else if(p.nKeys<ORDER-1){							// no need to split parent node, just insert the entry to parent node
    			pos=0;
    			while(pos<p.nKeys && p.key[pos].compareTo(key)<=0) pos++;	
				wedge(newNode.key[0], (V)newNode, p, pos);
			}
    		else{												// parent node need splitting
    			do{												// while loop to split nodes recursively from leaf back to root if needed
    				p=myStack.pop();
    				Node aNode = split(newNode.key[0],(V)newNode, p);
    				newNode=aNode;
    			}
    			while(!myStack.isEmpty() && myStack.peek().nKeys==ORDER-1);

    			if(!myStack.isEmpty()){							// node p is not full, just insert key and ref to this node							
    				p = myStack.pop();
    	   			pos=0;										
        			while(pos<p.nKeys && p.key[pos].compareTo(key)<=0) pos++;
    				wedge(newNode.key[0], (V)newNode,p,pos);
    			}
    		}
    	}   	
    	// n has fewer entries than ORDER-1
    	else wedge(key, ref, n, pos);
    } // insert

    /***************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     * @author Minh Pham: modify wedge() to compatible with my codes
     */
    private void wedge (K key, V ref, Node n, int i)
    {
        if(n.isLeaf){
            for (int j = n.nKeys; j > i; j--) {
                n.key [j] = n.key [j - 1];
                n.ref [j] = n.ref [j - 1];
            } // for
            n.ref [i] = ref;
        }
        else{
            for (int j = n.nKeys; j > i; j--) {
                n.key [j] = n.key [j - 1];
                n.ref [j+1] = n.ref [j];
            } // for
            n.ref [i+1] = ref;
        }
        n.key [i] = key;
        n.nKeys++;
        
        if(ref.getClass().toString().equals("class BpTree$Node") && !((Node)ref).isLeaf){
               for(int j=0; j<((Node)ref).nKeys-1; j++){
               	((Node)ref).key[j]=((Node)ref).key[j+1];
        		((Node)ref).ref[j]=((Node)ref).ref[j+1];
        	}
        	((Node)ref).ref[((Node)ref).nKeys-1]=((Node)ref).ref[((Node)ref).nKeys];
        	((Node)ref).nKeys--;
        }
    } // wedge

    /***************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @author Minh Pham
     */
    private Node split (K key, V ref, Node n)
    {
        Node newNode = new Node(n.isLeaf);
        // find the position to insert
        int pos=0;
        while(pos<ORDER-1 && key.compareTo(n.key[pos])>0){
               pos++;
        }
        int mid = ORDER/2;       	// mid of node n

        // key is on the left within node n
        if(pos<mid){
        	if(ORDER%2==0) mid--;
        	
        	// copy to newnode
            for(int i=0; i< ORDER/2; i++){
            	newNode.key[i] = n.key[mid+i];
            	newNode.ref[i] = n.ref[mid+i];
            }
            newNode.ref[ORDER/2]=n.ref[ORDER-1];
        	n.nKeys=mid;
            newNode.nKeys=ORDER/2;
            
            wedge(key, ref, n, pos);				// insert key and ref to node n
        }
        else{
        	if(ORDER%2==1) mid++;
        	n.nKeys=mid;
        	pos-=mid;

        	// copy to new node
            for(int i=0; i< (ORDER-2)/2; i++){
            	newNode.key[i] = n.key[mid+i];
            	newNode.ref[i] = n.ref[mid+i];
            }
            newNode.ref[(ORDER-2)/2]=n.ref[ORDER-1];
        	newNode.nKeys=(ORDER-2)/2;
        	
        	// insert key and ref to the new node
        	wedge(key, ref, newNode, pos);				
         }

        // splitting the root causes creating newRoot
        if(n==root){
        	Node newRoot = new Node(false);
        	newRoot.ref[0]=n;
        	wedge(newNode.key[0], (V) newNode, newRoot, 0);
        	root =newRoot;
        }
        
        return newNode;
    } // split

    /***************************************************************************
     * Return the number of keys in an interval
     * @param lo    lower limit of interval, can be null
     * @param loIn  whether lo is "in" interval
     * @param hi    upper limit, can be null
     * @param hiIn  whether hi is "in" interval
     * @return  number of keys within the interval defined by params
     * @author Zachary Freeland
     */

    public int nKeysInInterval(K lo, Boolean loIn, K hi, Boolean hiIn) {	    

	int sum = 0;
	Node n =root;
	if(root==null) return 0;
	Queue<Node> q = new LinkedList<Node>();   
	q.add(n);
	while(!q.isEmpty()){
	    Node temp = q.poll();
	    int i = 0;
	    int j = 0;
	    int k = temp.nKeys;
	    if(temp.isLeaf){
		if(lo != null)
		    while( i < temp.nKeys && !inInterval(temp.key[i], lo, loIn, hi, hiIn) ) { i++; }
		if(hi != null)
		    while( k > i && !inInterval(temp.key[k-1], lo, loIn, hi, hiIn) ) { k--; }
		sum+= k - i;
	    }
	    else{
		if(lo != null)
		    while( i < temp.nKeys && !inInterval(temp.key[i], lo, loIn, hi, hiIn) ) { i++; }
		if(hi != null)
		    while( k > i && !inInterval(temp.key[k-1], lo, loIn, hi, hiIn) ) { k--; }
		    if(i != 0)
			i--;
		    for(; i < k; i++)
			q.add((Node)temp.ref[i]);
	    }//else
	}//while
	return  sum;
    } // nKeysInRange

    /***************************************************************************
     * Return whether key is in an interval
     * @param key   key to test
     * @param lo    lower limit of interval, can be null
     * @param loIn  whether lo is "in" interval
     * @param hi    upper limit, can be null
     * @param hiIn  whether hi is "in" interval
     * @return  number of keys within the interval defined by params
     * @author Zachary Freeland
     */
    private boolean inInterval(K key, K lo, Boolean loIn, K hi, Boolean hiIn) {

	if (lo != null) {
	    int c = key.compareTo(lo);
	    if (c < 0 || (c == 0 && !loIn))
		return false; //key is too low
	}

	if (hi != null) {
	    int c = key.compareTo(hi);
	    if (c > 0 || (c == 0 && !hiIn))
		return false; //key is too high
	}

	return true;
    }


    /***************************************************************************
     * Return the first key within an interval
     * @param lo    lower limit of interval, can be null
     * @param loIn  whether lo is "in" interval
     * @param hi    upper limit, can be null
     * @param hiIn  whether hi is "in" interval
     * @return  first key within the interval defined by params
     * @author Zachary Freeland
     */
    public K firstKeyInInterval(K lo, Boolean loIn, K hi, Boolean hiIn) {
	K first = null;
	if ( lo == null ){
	    first = firstKey();
	} else {
	    Queue<Node> q = new LinkedList<Node>();
	    q.add(root);
	    while( !q.isEmpty() ){
		Node temp = q.poll();
		if( temp.isLeaf ){
		    for( int i = 0; i < temp.nKeys; i++){
			if( inInterval(temp.key[i], lo, loIn, hi, hiIn) ){
			    first = temp.key[i];
			    while(!q.isEmpty() ) { q.poll(); }
			}//if
		    }//for

		}else{
		    for( int i = 0; i < temp.nKeys; i++){
			if( inInterval(temp.key[i], lo, loIn, hi, hiIn) ){
			    if(i != 0)
				q.add((Node)temp.ref[i-1]);
			    q.add((Node)temp.ref[i]);
			}//if
		    }//for

		}//else

	    }//while

	}//else
	return first;
    }


    /***************************************************************************
     * Return the last key within an interval
     * @param lo    lower limit of interval, can be null
     * @param loIn  whether lo is "in" interval
     * @param hi    upper limit, can be null
     * @param hiIn  whether hi is "in" interval
     * @return  last key within the interval defined by params
     * @author Zachary Freeland
     */
    public K lastKeyInInterval(K lo, Boolean loIn, K hi, Boolean hiIn) {
	K last = null;
	if ( hi == null ){
	    last = lastKey();
	} else {
	    Node n = root;
	    int i = n.nKeys;
	    int j = 0;
	    while(!n.isLeaf){
		i = n.nKeys;
		while( !inInterval(n.key[i], lo, loIn, hi, hiIn) && i >= 0 )
		    i--;
		// key[i] < (|| =) hi after loop
		n = (Node)n.ref[i];
	    }
	    i = n.nKeys;
	    while( !inInterval(n.key[i], lo, loIn, hi, hiIn) && i >= 0 )
		i--;
	    //key[i] < (|| =) hi after loop
	    last = n.key[i];
	}
	return last;
    }


    /***************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTree <Integer, Integer> bpt = new BpTree <Integer, Integer> (Integer.class, Integer.class);
        int totKeys = 45;
        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < totKeys; i += 2) bpt.put (i, i * i);
        bpt.print (bpt.root, 0);
        for (int i = 0; i < totKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
    } // main

    /***************************************************************************
     * SubMap class below is derived from the SubMap implementation in api class
     * java.util.concurrent.ConcurrentSkipListMap, Authors are as noted in the
     * copied file header except where @author tags are present to indicate 
     * newly written methods to provide functionality for the BpTree class, as
     * well as some minor changes throuchout that make the existing constructors
     * and other code compatible with the implementation of BpTree.
     * @author Zachary Freeland
     *
     * The first following comment is a copy of the GNU GPL 2 header the original
     * code is distributed with
     */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */


    /**
     * Submaps returned by {@link BpTree} submap operations
     * represent a subrange of mappings of their underlying
     * maps. Instances of this class support all methods of their
     * underlying maps, differing in that mappings outside their range are
     * ignored, and attempts to add mappings outside their ranges result
     * in {@link IllegalArgumentException}.  Instances of this class are
     * constructed only using the <tt>subMap</tt>, <tt>headMap</tt>, and
     * <tt>tailMap</tt> methods of their underlying maps.
     *
     * @serial include
     */
    static final class SubMap<K extends Comparable <K>,V> extends AbstractMap<K,V>
        implements SortedMap<K,V>, Cloneable,
                   java.io.Serializable {

        /** Underlying map */
        private final BpTree<K,V> t;
        /** lower bound key, or null if from start */
        private final K lo;
        /** upper bound key, or null if to end */
        private final K hi;
        /** inclusion flag for lo */
        private final boolean loInclusive;
        /** inclusion flag for hi */
        private final boolean hiInclusive;

        // Lazily initialized view holders
        private transient Set<K> keySetView;
        private transient Set<Map.Entry<K,V>> entrySetView;
        private transient Collection<V> valuesView;

        /**
         * Creates a new submap, initializing all fields
         */
        SubMap(BpTree<K,V> tree,
               K fromKey, boolean fromInclusive,
               K toKey, boolean toInclusive) {
            if (fromKey != null && toKey != null &&
                fromKey.compareTo(toKey) > 0)
                throw new IllegalArgumentException("inconsistent range");
            this.t = tree;
            this.lo = fromKey;
            this.hi = toKey;
            this.loInclusive = fromInclusive;
            this.hiInclusive = toInclusive;
        }

        /* ----------------  Utilities -------------- */

        private boolean tooLow(K key) {
            if (lo != null) {
                int c = key.compareTo(lo);
                if (c < 0 || (c == 0 && !loInclusive))
                    return true;
            }
            return false;
        }

        private boolean tooHigh(K key) {
            if (hi != null) {
                int c = key.compareTo(hi);
                if (c > 0 || (c == 0 && !hiInclusive))
                    return true;
            }
            return false;
        }

        private boolean inBounds(K key) {
            return !tooLow(key) && !tooHigh(key);
        }

        private void checkKeyBounds(K key) throws IllegalArgumentException {
            if (key == null)
                throw new NullPointerException();
            if (!inBounds(key))
                throw new IllegalArgumentException("key out of range");
        }

        /* ----------------  Map API methods -------------- */

        public boolean containsKey(Object key) {
            if (key == null) throw new NullPointerException();
            K k = (K)key;
            return inBounds(k) && t.containsKey(k);
        }

        public V get(Object key) {
            if (key == null) throw new NullPointerException();
            K k = (K)key;
            return ((!inBounds(k)) ? null : t.get(k));
        }

        public V put(K key, V value) {
            return t.put(key, value);
        }

	/***************************************************************************
	 * Return the size of the SubMap
	 * @return  number of keys within the range of the submap
	 * @author Zachary Freeland
	 */


        public int size() {	    
	    return t.nKeysInInterval(lo, loInclusive, hi, hiInclusive);
	} // size

        /* ----------------  SortedMap API methods -------------- */

        public Comparator<? super K> comparator() {
            return t.comparator();
        }

        /**
         * Utility to create submaps, where given bounds override
         * unbounded(null) ones and/or are checked against bounded ones.
         */
        private SubMap<K,V> newSubMap(K fromKey,
                                      boolean fromInclusive,
                                      K toKey,
                                      boolean toInclusive) {
            if (lo != null) {
                if (fromKey == null) {
                    fromKey = lo;
                    fromInclusive = loInclusive;
                }
                else {
                    int c = fromKey.compareTo(lo);
                    if (c < 0 || (c == 0 && !loInclusive && fromInclusive))
                        throw new IllegalArgumentException("key out of range");
                }
            }
            if (hi != null) {
                if (toKey == null) {
                    toKey = hi;
                    toInclusive = hiInclusive;
                }
                else {
                    int c = toKey.compareTo(hi);
                    if (c > 0 || (c == 0 && !hiInclusive && toInclusive))
                        throw new IllegalArgumentException("key out of range");
                }
            }
            return new SubMap<K,V>(t, fromKey, fromInclusive,
                                   toKey, toInclusive);
        }

        public SubMap<K,V> subMap(K fromKey,
                                  boolean fromInclusive,
                                  K toKey,
                                  boolean toInclusive) {
            if (fromKey == null || toKey == null)
                throw new NullPointerException();
            return newSubMap(fromKey, fromInclusive, toKey, toInclusive);
        }

        public SubMap<K,V> headMap(K toKey, boolean inclusive) {
            if (toKey == null)
                throw new NullPointerException();
            return newSubMap(null, false, toKey, inclusive);
        }

        public SubMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (fromKey == null)
                throw new NullPointerException();
            return newSubMap(fromKey, inclusive, null, false);
        }

        public SubMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SubMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public SubMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

	/***************************************************************************
	 * Return the first key within this SubMap
	 * @return  first key within the range of the submap
	 * @author Zachary Freeland
	 */

        public K firstKey() {
	    if ( size() == 0 )
		return null;
	    else if (lo == null)
		return t.firstKey();
	    else
		return t.firstKeyInInterval(lo, loInclusive, hi, hiInclusive);
        }


	/***************************************************************************
	 * Return the last key within this SubMap
	 * @return  last key within the range of the submap
	 * @author Zachary Freeland
	 */
        public K lastKey() {
	    if ( size() == 0 )
		return null;
	    else if (hi == null)
		return t.lastKey();
	    else
		return t.lastKeyInInterval(lo, loInclusive, hi, hiInclusive);
	}

	/***************************************************************************
	 * Returns copy of entrySet from underlying tree after removing extraneous
	 * entries
	 * @return  Set of entries view of SubMap
	 * @author Zachary Freeland
	 */

        public Set<Map.Entry<K,V>> entrySet() {
            Set<Map.Entry<K,V>> es = entrySetView;
            if (es != null){
		entrySetView = t.entrySet();
		Object[] entries = entrySetView.toArray();
		K e = null;
		for(int i=0; i < entries.length; i++)
		    if( !inBounds( (e = ((Map.Entry<K,V>)entries[i]).getKey()) ) )
			entrySetView.remove(e);
		es = entrySetView;
	    }
	    return es;
        }
    }// SubMap class


} // BpTree class

