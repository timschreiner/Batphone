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
}
