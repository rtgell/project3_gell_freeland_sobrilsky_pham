/*******************************************************************************
 * @file  Conversions.java
 *
 * @author   John Miller
 *
 * @see http://snippets.dzone.com/posts/show/93
 */

/*******************************************************************************
 * This class provides methods for converting Java's primitive data types into
 * byte arrays.
 */
public class Conversions
{
    /***************************************************************************
     * Convert short into a byte array.
     * @param value  the short value to convert
     * @return  a corresponding byte array
     */
    public static byte [] short2ByteArray (short value)
    {
        return new byte [] { (byte) (value >>> 8),
                             (byte) value };
    } // short2ByteArray

    /***************************************************************************
     * Convert int into a byte array.
     * @param value  the int value to convert
     * @return  a corresponding byte array
     */
    public static byte [] int2ByteArray (int value)
    {
        return new byte [] { (byte) (value >>> 24),
                             (byte) (value >>> 16),
                             (byte) (value >>> 8),
                             (byte) value };
    } // int2ByteArray

    /***************************************************************************
     * Convert long into a byte array.
     * @param value  the long value to convert
     * @return  a corresponding byte array
     */
    public static byte [] long2ByteArray (long value)
    {
        return new byte [] { (byte) (value >>> 56),
                             (byte) (value >>> 48),
                             (byte) (value >>> 40),
                             (byte) (value >>> 32),
                             (byte) (value >>> 24),
                             (byte) (value >>> 16),
                             (byte) (value >>> 8),
                             (byte) value };
    } // long2ByteArray

    /***************************************************************************
     * Convert float into a byte array.
     * @param value  the float value to convert
     * @return  a corresponding byte array
     * Minh Pham
     */
    public static byte [] float2ByteArray (float value)
    {
        int bits = Float.floatToIntBits(value);
    	byte[] bytes = new byte[4];
    	bytes[0] = (byte)(bits & 0xff);
    	bytes[1] = (byte)((bits >> 8) & 0xff);
    	bytes[2] = (byte)((bits >> 16) & 0xff);
    	bytes[3] = (byte)((bits >> 24) & 0xff);
    	return bytes;
    } // float2ByteArray

    /***************************************************************************
     * Convert double into a byte array.
     * @param value  the double value to convert
     * @return  a corresponding byte array
     * Minh Pham
     */
    public static byte [] double2ByteArray (double value)
    {
        byte[] bytes = new byte[8];
    	long lng = Double.doubleToLongBits(value);
    	for(int i = 0; i < 8; i++) bytes[i] = (byte)((lng >> ((7 - i) * 8)) & 0xff);
    	return bytes;
    } // double2ByteArray

} // Conversions

