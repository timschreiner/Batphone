package com.timpo.batphone.codecs;

import java.io.IOException;

/**
 * Responsible for serializing / deserializing objects to byte arrays
 */
public interface Codec {

	/**
	 * Serialize an object to a byte array
	 *
	 * @param toEncode
	 * @return the encodec byte array
	 * @throws IOException
	 */
	public byte[] encode(Object toEncode) throws IOException;

	/**
	 * Deserialize a byte array into an Object of the specified class
	 *
	 * @param <T>
	 * @param toDecode the byte array to
	 * @param decodeAs the Class of the hydrated object
	 * @return the hydrated object
	 * @throws IOException
	 */
	public <T> T decode(byte[] toDecode, Class<T> decodeAs) throws IOException;	

	/*
	add the topic to the above methods in case someone needs that

	Anyone who wants to serialize / deserialize can use the topic name to serialize to something specific,
	but it will still need to enc/dec arbitrary maps.

	Find out what the performance difference is in terms of enc/dec/size to know if this is even that big of a deal.
	*/
}
