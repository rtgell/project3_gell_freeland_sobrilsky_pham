/*******************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */

import java.nio.ByteBuffer;
import java.io.Serializable;
import static java.lang.Boolean.*;
import static java.lang.System.out;
import java.util.*;

/*******************************************************************************
 * This class implements relational database tables (including attribute names,
 * domains and a list of tuples.  Five basic relational algebra operators are
 * provided: project, select, union, minus and join.  The insert data manipulation
 * operator is also provided.  Missing are update and delete data manipulation
 * operators.
 */
public class Table
       implements Serializable, Cloneable
{
    /** Debug flag, turn off once implemented
     */
    private static final boolean DEBUG = false;

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key. 
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple).
     */
    private final Map <KeyType, Comparable []> index;

    /***************************************************************************
     * Construct an empty table from the meta-data specifications.
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new FileList (this, tupleSize ());
        //index     = new TreeMap <KeyType, Comparable[]> ();                  // also try BPTreeMap, LinHash or ExtHash
        index     = new ExtHash<KeyType, Comparable[]> (KeyType.class, Comparable[].class, attribute.length);
    } // Table

    /***************************************************************************
     * Construct an empty table from the raw string specifications.
     * @param name        the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     */
    public Table (String name, String attributes, String domains, String _key)
    {
        this (name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // Table

    /***************************************************************************
     * Construct an empty table using the meta-data of an existing table.
     * @param tab     the table supplying the meta-data
     * @param suffix  the suffix appended to create new table name
     */
    public Table (Table tab, String suffix)
    {
        this (tab.name + suffix, tab.attribute, tab.domain, tab.key);
    } // Table

    /***************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     * #usage movie.project ("title year studioNo")
     * @param attributeList  the attributes to project onto
     * @return  the table consisting of projected tuples
     * @author Zachary Freeland
     */
    public Table project (String attributeList)
    {
        out.println ("RA> " + name + ".project (" + attributeList + ")");

        String [] pAttribute = attributeList.split (" ");
        int []    colPos     = match (pAttribute);
        Class []  colDomain  = extractDom (domain, colPos);
	//Test if all attributes exist
        for(int i = 0; i < pAttribute.length; i++){
	    if(!inAttribute(pAttribute[i])){System.out.println("Requested attribute does not exist"); System.exit(0);}
	}
        //prepare to test if keys are present                                                                                                                            
        int []    keyPos     = match (key); //find keys in columns                                                                                                       
        boolean   keysPres   = true; //no keys are missing yet                                                                                                           

        //outer loop iterates over key locations, ending if one is missed                                                                                             
        for(int i = 0; ( (i < keyPos.length) && keysPres ); i++){
            boolean currKeyFound = false; //set to true when found                                                                                                       

            //inner loop iterates over projected attribute columns in initial table, ending early if current key column is found                  
            for(int j = 0; ( (j < colPos.length) && !currKeyFound ); j++){
                if( keyPos [i] == colPos [j] ){
                    currKeyFound = true;
                }//if
            }//for
            if(!currKeyFound){
                keysPres = false;
            }//if
        }//for

        String [] newKey     = null;
        if (keysPres)
            newKey = key; //original key if all included                                                                                                                 
        else
            newKey = pAttribute; //all attributes if not                                                                                                                 


        Table     result     = new Table (name + count++, pAttribute, colDomain, newKey);

        for (Comparable [] tup : tuples) {
            result.insert(extractTup (tup, colPos));
        } // for

        return result;
    } // project

    /***************************************************************************
     * Select the tuples satisfying the given condition.
     * A condition is written as infix expression consists of 
     *   6 comparison operators: "==", "!=", "<", "<=", ">", ">="
     *   2 Boolean operators:    "&", "|"  (from high to low precedence)
     * #usage movie.select ("1979 < year & year < 1990")
     * @param condition  the check condition for tuples
     * @return the table consisting of tuples satisfying the condition
     * @author Ryan Gell
     */
    public Table select (String condition)
    {
        out.println ("RA> " + name + ".select (" + condition + ")");

       String [] postfix = infix2postfix (condition);
	System.out.println(Arrays.toString(postfix));
        Table     result  = new Table (name + count++, attribute, domain, key);

        for (Comparable [] tup : tuples) {
            if (evalTup (postfix, tup)) result.insert(tup);
        } // for

        return result;
    } // select

    /***************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     * #usage movie.union (show)
     * @param table2  the rhs table in the union operation
     * @return  the table representing the union (this U table2)
     * @author minh pham
     */
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");
        Table result = new Table (name + count++, attribute, domain, key);
        if (!this.compatible(table2)){
        	return result;
        }
        int length1 = this.tuples.size();
        for(int i=0; i< length1; i++){
        	result.insert(this.tuples.get(i));
        }
        int length2 = table2.tuples.size();
        for (int i=0; i< length2; i++){
        	if(!isEqual(table2.tuples.get(i), result.getTupFromKey(getKeyVal(table2.tuples.get(i)))) ){
        		result.insert(table2.tuples.get(i));
        	}
        }
        return result;
    } // union

    /***************************************************************************
     * Take the difference of this table and table2.  Check that the two tables
     * are compatible.
     * #usage movie.minus (show)
     * @param table2  the rhs table in the minus operation
     * @return  the table representing the difference (this - table2)
     * @author: Nicholas Sobrilsky
     */
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");

        Table result = new Table (name + count++, attribute, domain, key);

		if ( !this.compatible(table2) ){
		    System.err.println("Error: Tables not compatible. " + name + " returned.");
		} else{
		    for ( int i=0; i<this.tuples.size(); i++ ){
			    if (isEqual(this.tuples.get(i),table2.getTupFromKey(getKeyVal(this.tuples.get(i))) ) ){
			        break;
			    } else{
			        result.insert( this.tuples.get(i) );
			    }
		    }
		}

        return result;
    } // minus

    /***************************************************************************
     * Join this table and table2.  If an attribute name appears in both tables,
     * assume it is from the first table unless it is qualified with the first
     * letter of the second table's name (e.g., "s.").
     * In the result, disambiguate the attribute names in a similar way
     * (e.g., prefix the second occurrence with "s_").
     * Caveat: the key parameter assumes joining the table with the foreign key
     * (this) to the table containing the primary key (table2).
     * #usage movie.join ("studioNo == name", studio);
     * #usage movieStar.join ("name == s.name", starsIn);
     * @param condition  the join condition for tuples
     * @param table2     the rhs table in the join operation
     * @return  the table representing the join (this |><| table2)
     * @author Nicholas Sobrilsky
     */
    public Table join (String condition, Table table2)
    { 
        out.println ("RA> " + name + ".join (" + condition + ", " + table2.name + ")");
	
	String [] postfix = infix2postfix(condition);
	boolean keepAllAttributes = (postfix[1].substring(0, 2).equals("s."));
		 
	int attrDomSize = this.getAttributeLength() + table2.getAttributeLength();
	
	String rightCondName;
	
	if(!keepAllAttributes){
		attrDomSize--;
		rightCondName=postfix[1];
	}
	else{
		rightCondName=postfix[1].substring(2);
	}
	
	String [] resultAttribute = new String[attrDomSize];
	Class [] resultDomain = new Class[attrDomSize];
		
	//Sets the attribute and domain arrays of the result to the same of this table
	for (int initialize=0; initialize<this.getAttributeLength(); initialize++){
		resultAttribute[initialize] = this.getAttributeAt(initialize);
		resultDomain[initialize] = this.getDomainAt(initialize);
	}
	
	int skipIndex = -1;
	int skipCounter = 0;
	/*
	Adds the attributes of table2 to result attributes
	If the attribute is in the form of s.attrib, rename the attribute and add
	Else, skip the table2 attribute named in the condition
	*/
	for (int i=0; i<table2.getAttributeLength(); i++){
		if (keepAllAttributes && table2.getAttributeAt(i).equals(rightCondName)){
			resultAttribute[this.getAttributeLength()+i+skipCounter] = postfix[1];
		}
		else if (table2.getAttributeAt(i).equals(rightCondName)){
			skipIndex = i;
			skipCounter--;
		}
		else{
			resultAttribute[this.getAttributeLength()+i+skipCounter] = table2.getAttributeAt(i);
		}
	}
		
	/*
	Adds the domains of table2 to result domain
	If the domain belongs to a skipped attribute, skip it
	*/
	skipCounter=0;
	for (int j=0; j<table2.getDomainLength(); j++){
		if(j==skipIndex){
			skipCounter--;
		}
		else{
			resultDomain[this.getDomainLength()+skipCounter+j] = table2.getDomainAt(j);
		}
	}
	
	Table result = new Table (name + count++, resultAttribute, resultDomain, key);
		
	boolean noMatch=false;
		
	for(int m=0; m<this.tuples.size(); m++){
		Comparable [] resultTup = new Comparable[attrDomSize];
		int table1MatchIndex=0;
		int table2MatchIndex=0;
		for (int n=0; n<table2.tuples.size(); n++){
			if( this.getValueAt(this.columnPos(postfix[0]), this.tuples.get(m)).equals(table2.getValueAt(table2.columnPos(rightCondName), table2.tuples.get(n))) ){
				table2MatchIndex = n;
				break;
			}
			else if(n==table2.tuples.size()-1){
				noMatch=true;
			}
		} //Matches a tuple from each table that meets the condition
		
		if(noMatch){
			noMatch=false;
			continue;
		}
		
		for(int t1FillIndex=0; t1FillIndex<this.getAttributeLength(); t1FillIndex++){
			resultTup[t1FillIndex]=this.getValueAt(t1FillIndex, this.tuples.get(m));
		} //Adds all items from table1 tuple at index m to resultTup
		
		int t2FillLimit=table2.getAttributeLength();
		
		skipCounter = 0;
		for(int t2FillIndex=0; t2FillIndex<t2FillLimit; t2FillIndex++){
			out.println(t2FillIndex);
			if(table2.getAttributeAt(t2FillIndex).equals(rightCondName) && !keepAllAttributes){
				skipCounter--;
				continue;
			}
			resultTup[this.getAttributeLength()+t2FillIndex+skipCounter]=table2.getValueAt(t2FillIndex, table2.tuples.get(table2MatchIndex));
		} //Adds all unskipped items from the matched tuple in table2
		
		result.insert(resultTup);
	}
        return result;
    } // join
/***************************************************************************
     * Insert a tuple to the table.
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     * @param tup  the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup, domain)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            index.put (new KeyType (keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /***************************************************************************
     * Get the name of the table.
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName
    
    /***************************************************************************
     * Get the attribute at the indexed column of the table
     * @param index  the index number of the array item you want to access
     * @return  the attribute at the given index
     * @author  Nicholas Sobrilsky
     */
    private String getAttributeAt(int index)
    {
        return attribute[index];
    } //getAttributeAt
    
     /***************************************************************************
     * Get the domain (class) at the indexed column of the table
     * @param index  the index number of the array item you want to access
     * @return  the domain (class) at the given index
     * @author  Nicholas Sobrilsky
     */
    private Class getDomainAt(int index){
	 return domain[index];
    } //getDomainAt
    
     /***************************************************************************
     * Get the length of the attribute array of the table
     * @return  the length of the attribute array
     * @author  Nicholas Sobrilsky
     */
    private int getAttributeLength(){
	 return attribute.length;
    } //getAttributeArray
    
     /***************************************************************************
     * Get the length of the domain array of the table
     * @return  the length of the domain array
     * @author  Nicholas Sobrilsky
     */
    private int getDomainLength(){
	 return domain.length;
    } //getDomainLength

    /***************************************************************************
     * Print the table.
     */
    public void print ()
    {
        out.println ("\n Table " + name);

        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");

        if (DEBUG) {
            out.print ("|-");
            for (int i = 0; i < domain.length; i++) out.print ("---------------");
            out.println ("-|");
            out.print ("| ");
            for (Class d : domain) out.printf ("%15s", d.getSimpleName ());
            out.println (" |");
        } // if

        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /***************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e.,
     * have the same number of attributes each with the same corresponding domain.
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     * @author  Nicholas Sobrilsky
     */
    private boolean compatible (Table table2)
    {
        if ( this.getAttributeLength()!=table2.getAttributeLength() ){
              return false;
        }
        
        int i=0;
        while(i<this.getDomainLength()){
	    for(int j = 0; j < table2.getDomainLength(); j++){
		if(this.getAttributeAt(i).compareToIgnoreCase(table2.getAttributeAt(j))==0 && this.getDomainAt(i).getName().compareToIgnoreCase(table2.getDomainAt(j).getName())==0){
		    i++;
		}
	    }
        }
        if(i==this.getDomainLength()){
        	return true;
        }
        return false;
        
    } // compatible

    /***************************************************************************
     * Return the column position for the given column/attribute name.
     * @param column  the given column/attribute name
     * @return  the column index position
     */
    private int columnPos (String column)
    {
        for (int j = 0; j < attribute.length; j++) {
           if (column.equals (attribute [j])) return j;
        } // for

        out.println ("columnPos: error - " + column + " not found");
        return -1;  // column name not found in this table
    } // columnPos

    /***************************************************************************
     * Return all the column positions for the given column/attribute names.
     * @param columns  the array of column/attribute names
     * @return  the array of column index positions
     */
    private int [] match (String [] columns)
    {
        int [] colPos = new int [columns.length];

        for (int i = 0; i < columns.length; i++) {
            colPos [i] = columnPos (columns [i]);
        } // for

        return colPos;
    } // match

    /***************************************************************************
     * Check whether the tuple satisfies the condition.  Use a stack-based postfix
     * expression evaluation algorithm.evalTup
     * @param postfix  the postfix expression for the condition
     * @param tup      the tuple to check
     * @return whether to keep the tuple
     * @author Ryan Gell
     */
    @SuppressWarnings("unchecked")
    private boolean evalTup (String [] postfix, Comparable [] tup)
    {
        if (postfix == null) return true;
        Stack <Comparable> s = new Stack <Comparable> ();
	//stack-based postfix evaluation
        for (String token : postfix) {
	//if it is a comparison operator, we need to pop two strings
		if(isComparison(token)){
			checkString(s.peek());
			String one = (String)s.pop();
			checkString(s.peek());
			String two = (String)s.pop();
		//if the first string is an attribute
			if(inAttribute(one)){
		//we get where the attribute is in the database domain
				int pos = attributeIndex(one);
		//we turn the string into a type and grab the attribute from the tup 
				s.push(compare(tup[pos],token,String2Type.cons(domain[pos],two)));
			}//if
		//As above but with second string
			else if (inAttribute(two)){
				int pos = attributeIndex(two);
				s.push(compare(String2Type.cons(domain[pos],one),token,tup[pos]));
			}//else if
		//If there was no attribute in the operation string, exit as the
		//Formula is ill formed
			else{System.out.println("Select string is ill-formed");System.exit(0);}//else
			
		}//if
		//if we have an 'or' we need to pop two booleans and push them or'd
		else if(token.equals("|")){
			checkBool(s.peek());
			Boolean	one = (Boolean)s.pop();
			checkBool(s.peek());
			Boolean two = (Boolean)s.pop();	
			s.push(one||two);
		}//else if
		//if we have an 'or' we need to pop two booleans and push them or'd
		else if(token.equals("&")){
			checkBool(s.peek());
			Boolean	one = (Boolean)s.pop();
			checkBool(s.peek());
			Boolean two = (Boolean)s.pop();
			s.push(one&&two);
		}//else if
		//Otherwise we just have a string token, so we push it into the stack
		else{
			s.push(token);		
		}//elses
        } // for		
	checkBool(s.peek());
	//Return the last Boolean in the stack, e.g. the answer
        return (Boolean) s.pop ();
    	} // evalTup
     /**
     * Check Comparable object is a Boolean
     * @param c  the object to check
     * @author Ryan Gell
     */	
	private void checkBool(Comparable c){
		if(!(c instanceof Boolean)){
			System.out.println("Input to Select ill-formed");
			System.exit(0);
		}
	}
     /**
     * Check Comparable object is a String
     * @param c  the object to check
     * @author Ryan Gell
     */	

	private void checkString(Comparable c){
		if(!(c instanceof String)){
			System.out.println("Input to Select ill-formed");
			System.exit(0);
		}
	}
     /**
     * Check if the string is an attribute in the database
     * @param s  the string we want to check
     * @return if the string is in attribute
     * @author Ryan Gell
     */	
	private boolean inAttribute(String s){
		for(int i = 0; i < attribute.length; i++){
			if(attribute[i].toLowerCase().equals(s.toLowerCase()) ) return true;
		}//for
		return false;
	}//inAttribute
     /**
     * Find where a given attribute is in the attribute list
     * @param s  the string we want to find
     * @return the index of the string or -1 if it's not present
     * @author Ryan Gell
     */	
	private int attributeIndex(String s){
		if(inAttribute(s)){
			for(int i = 0; i < attribute.length; i++){
				if( s.toLowerCase().equals(attribute[i].toLowerCase()) )
					return i;
			}//for
		}//if
		return -1;
	}//attributeIndex
     /**
     * Returns the value of a certain index of a tup
     * @param index  the index we want to access
     * @param tup    the tup we want to access
     * @return the value at that index
     * @author Ryan Gell
     */	
	private Comparable getValueAt(int index, Comparable [] tup){
		return tup[index];	
	}//getValueAt
     /**
     * Returns the value of a certain attribute in a tup
     * @param key  the name of the attribute we want to access
     * @param tup    the tup we want to access
     * @return the value at that index
     * @author Ryan Gell
     */	
	private Comparable getValueOf(String key, Comparable[] tup){
		return getValueAt(attributeIndex(key), tup);	
	}//getValueOf
    /***************************************************************************
     * Pack tuple tup into a record/byte-buffer (array of bytes).
     * @param tup  the array of attribute values forming the tuple
     * @return  a tuple packed into a record/byte-buffer
     * Minh Pham
     */
    byte [] pack (Comparable [] tup)
    {
        byte [] record = new byte [tupleSize ()];
        byte [] b      = null;
        int     s      = 0;
        int     i      = 0;

		for (int j = 0; j < domain.length; j++) {
			if(domain [j].getName().equalsIgnoreCase("java.lang.Integer")){
				b = Conversions.int2ByteArray ((Integer) tup [j]);
				s = 4;
			}
			else if(domain [j].getName().equalsIgnoreCase("java.lang.Short")){
				b = Conversions.short2ByteArray((Short) tup[j]);
				s = 2;			
			}
			else if(domain [j].getName().equalsIgnoreCase("java.lang.String")){
				b = ((String) tup [j]).getBytes ();
				s = 64;
			}
			else if(domain [j].getName().equalsIgnoreCase("java.lang.Double")){
				b = Conversions.double2ByteArray((Double) tup[j]);
				s = 8;			
			}
			else if(domain [j].getName().equalsIgnoreCase("java.lang.Float")){
				b = Conversions.float2ByteArray((Float) tup[j]);
				s = 4;			
			}
			else if(domain [j].getName().equalsIgnoreCase("java.lang.Long")){
				b = Conversions.long2ByteArray((Long) tup[j]);
				s = 8;			
			}
			else if(domain [j].getName().equalsIgnoreCase("java.lang.Character")){
			    b[0] = (byte) ((Character) tup[j] & 0xff);
				s = 1;			
			}
			else{
				System.out.println("cannot recognize type -> cannot pack");
			}
			
			
			if (b == null) {
				out.println ("Table.pack: byte array b is null");
				return null;
			} // if
			for (int k = 0; k < s; k++) {
				if(k<b.length){
					record [i++] = b [k];
				}
				else{
					record[i++] = (byte)'\0';
				}
			}
		} // for
        return record;
    } // pack
     

    /***************************************************************************
     * Unpack the record/byte-buffer (array of bytes) to reconstruct a tuple.
     * @param record the byte-buffer in which the tuple is packed
     * @return  an unpacked tuple
     * @author Zachary Freeland
     */
    Comparable [] unpack (byte [] record)
    {
	Comparable[] result = new Comparable[domain.length];
	ByteBuffer bb = ByteBuffer.wrap(record);

	for(int j=0; j < domain.length; j++) {
	    if( domain [j].getName().equalsIgnoreCase("java.lang.Integer") ) {
		result[j] = bb.getInt();
	    } else if( domain [j].getName().equalsIgnoreCase("java.lang.Short") ) {
		result[j] = bb.getShort();
	    } else if( domain [j].getName().equalsIgnoreCase("java.lang.Double") ) {
		result[j] = bb.getDouble();
	    } else if( domain [j].getName().equalsIgnoreCase("java.lang.Float") ) {
		result[j] = bb.getFloat();
	    } else if( domain [j].getName().equalsIgnoreCase("java.lang.Long") ) {
		result[j] = bb.getLong();
	    } else if( domain [j].getName().equalsIgnoreCase("java.lang.Character") ) {
		result[j] = (char) bb.get();
	    } else if( domain [j].getName().equalsIgnoreCase("java.lang.String") ) {
		byte[] temp = new byte[64];
		if(bb.remaining() < 64)
		    temp = new byte[bb.remaining()];
		bb.get(temp);
		int i=0;
		for(; i < 64 && temp[i] !='\0'; i++);//fixes alignment problems if string length is less than 15 characters
		byte[] temp2 = new byte[i];
		for(i=0; i < 64 && temp[i] !='\0'; i++)
		    temp2[i] = temp[i];
		result[j] = new String(temp2);
	    } else {
		System.out.println("cannot recognize type -> cannot pack");
	    }
	}//for

        return result;
    } // unpack


    /***************************************************************************
     * Determine the size of tuples in this table in terms of the number of bytes
     * required to store it in a record/byte-buffer.
     * @return  the size of packed-tuples in bytes
     * Minh Pham
     */
    private int tupleSize ()
    {
        int s = 0;

	 	for (int j = 0; j < domain.length; j++) {
			if(domain [j].getName ().equalsIgnoreCase("java.lang.Integer")) s+=4;
			else if(domain [j].getName ().equalsIgnoreCase("java.lang.Long")) s+=8;
			else if(domain [j].getName ().equalsIgnoreCase("java.lang.Short")) s+=2;
			else if(domain [j].getName ().equalsIgnoreCase("java.lang.Character")) s++;
			else if(domain [j].getName ().equalsIgnoreCase("java.lang.Double")) s+=8;
			else if(domain [j].getName ().equalsIgnoreCase("java.lang.Float")) s+=4;
			else if(domain [j].getName ().equalsIgnoreCase("java.lang.String")) s+=64;
			else{
				System.out.println("cannot recognize domain");
			}// if clause
		}// for loop
		
        return s;
    } // tupleSize
    /**
    * Returns the value held in the key domains of a tuple
    * @author Ryan Gell 
    */
    public Comparable[] getKeyVal(Comparable[] tuple){
    	Comparable[] keyVal = new Comparable [key.length];
	int [] cols = match(key);
	for(int i = 0; i < keyVal.length; i++) keyVal [i] = tuple[cols[i]];
	return keyVal;
    }
    /**
    *
    */
    public Comparable[] getTupFromKey(Comparable[] key){
    	return index.get(new KeyType(key));
    }

    //------------------------ Static Utility Methods --------------------------

    /***************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the
     * type of each value to ensure it is from the right domain. 
     * @param tup  the tuple as a list of attribute values
     * @param dom  the domains (attribute types)
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     * @author minh pham
     */
    private static boolean typeCheck (Comparable [] tup, Class [] dom)
    { 
    	if(tup.length!=dom.length){
    		return false;
    	}
    	int length = tup.length;
    	for(int i=0; i< length; i++){
    		if(!tup[i].getClass().getName().equalsIgnoreCase(dom[i].getName()) ){
    			return false;
    		}
    	}

        return true;
    } // typeCheck

    /***************************************************************************
     * Determine if the token/op is a comparison operator.
     * @param op  the token/op to check
     * @return  whether it a comparison operator
     */
    private static boolean isComparison (String op)
    {
        return op.equals ("==") || op.equals ("!=") ||
               op.equals ("<")  || op.equals ("<=") ||
               op.equals (">")  || op.equals (">=");
    } // isComparison

    /***************************************************************************
     * Compare values x and y according to the comparison operator.
     * @param   x   the first operand
     * @param   op  the comparison operator
     * @param   y   the second operand
     * @return  whether the comparison evaluates to true or false
     */
    @SuppressWarnings("unchecked")
    private static boolean compare (Comparable x, String op , Comparable y)
    {
        if(op.equals("==")) return x.compareTo (y) == 0;
        else if(op.equals("!=")) return x.compareTo (y) != 0;
        else if(op.equals("<")) return x.compareTo (y) < 0;
        else if(op.equals("<=")) return x.compareTo (y) <= 0;
        else if(op.equals(">"))return x.compareTo (y) > 0;
        else if(op.equals(">="))return x.compareTo (y) >= 0;
        else { out.println ("compare: error - unexpected op"); return false; }
    } // compare

    /***************************************************************************
     * Convert an untokenized infix expression to a tokenized postfix expression.
     * This implementation does not handle parentheses ( ).
     * Ex: "1979 < year & year < 1990" --> { "1979", "year", "<", "year", "1990", "<", "&" } 
     * @param condition  the untokenized infix condition
     * @return  resultant tokenized postfix expression
     * @author Ryan Gell
     */
    @SuppressWarnings("unchecked")
    public static String [] infix2postfix (String condition)
    {
        if (condition == null || condition.trim () == "") return null;
        String [] infix   = condition.split (" ");        // tokenize the infix
        String [] postfix = new String [infix.length];    // same size, since no ( ) 
	//Stack for stack-based implementation	
	Stack ops = new Stack();
	//j follows where we are in postfix
	int j = 0;
	for(int i = 0; i < infix.length; i++){
		//If we have an operator (rather than a token)
		if(isComparison(infix[i]) || infix[i].equals("&") || infix[i].equals("|")){
			if(ops.empty()){//if our stack is empty push
				ops.push(infix[i]);
			}//if
			else{
				while( (ops.empty()==false) && !precedence(infix[i], (String)ops.peek() )){//As long as the stack isn't empty 				//and the operator we're looking at has higher precedence
					postfix[j] = (String)ops.pop();//Take the top operator
					j++;
				}//while
				ops.push(infix[i]);//Then push the one we're looking at
			}//else
		}//if
		else{
			if(infix[i].charAt(0) == '\''){//If our term is in quotes
				postfix[j] = infix[i].substring(1,(infix[i].length())-1);}//Remove the quotes
			else{
			postfix[j] = infix[i];}//And but them in postfix
			j++;		
		}//else
	}
	while(!ops.empty()){//Pop the remaining 
		postfix[j] = (String)ops.pop();
		j++;		
	}//while
        return postfix;
    } // infix2postfix
	/**
	* A function to compare the two precedence of two operators 
	* #usage precedence("==","!=")
	* @author Ryan Gell
	* @return true if first has higher precedence thant second
	*/
	private static boolean precedence(String first, String second){
		if(precedenceInt(first) > precedenceInt(second)){
			return true;
		}
		else{return false;}
	}//precedence
	/**
	* Returns an int equal to the precedence of the operator
	* #usage precedenceInt("==")
	* @author Ryan Gell
	* @return int between 8 and 0
	*/
	private static int precedenceInt(String x){
		if(x.equals("==")){ return 8;}
        	else if(x.equals("!=")){ return 7;}
        	else if(x.equals("<")){  return 6;}
        	else if(x.equals("<=")){ return 5;}
        	else if(x.equals(">")){  return 4;}
        	else if(x.equals(">=")){ return 3;}
        	else if(x.equals("&")){ return 2;}
        	else if(x.equals("|")){ return 1;}
        	else { return 0; }
	}
    /***************************************************************************
     * Find the classes in the "java.lang" package with given names.
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  the array of Java classes for the corresponding names
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /***************************************************************************
     * Extract the corresponding do mains from the group.
     * @param group   where to extract from
     * @param colPos  the column positions to extract
     * @return  the extracted domains
     */

    private static Class [] extractDom (Class [] group, int [] colPos)
    {
        Class [] dom = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            dom [j] = group[colPos [j]];
        } // for

        return dom;
    } // extractDom

    /***************************************************************************
     * Extract the corresponding attribute values from the group.
     * @param group   where to extract from
     * @param colPos  the column positions to extract
     * @return  the extracted attribute values
     * @author Zachary Freeland
     */

    private static Comparable [] extractTup (Comparable [] group, int [] colPos)
    {
        Comparable [] tup = new Comparable [colPos.length];

        for (int i=0; i<tup.length; i++){
        	tup[i]=group[colPos[i]];
        }

        return tup;
    } // extractTup
	/**
	*Compares two tuples and returns true if the two are the same
	*@return true iff the tuples are the same
	*@author Ryan Gell
	*/
    @SuppressWarnings("unchecked")
    private static boolean isEqual(Comparable[] tup1, Comparable[] tup2){
    	int size = tup1.length;
	int j = 0;
	if(tup1 == null && tup2 == null){return true;}
	else if(tup1 == null || tup2 == null){return false;}
    	for(int i = 0; i<size ; i++){ 
    		if(tup1[i].equals(tup2[i])){j++;}
    	}
    	if(j==size){
    		return true;
    	}
    	return false;
    }

} // Table class
