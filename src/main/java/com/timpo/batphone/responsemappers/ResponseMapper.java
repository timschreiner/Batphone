package com.timpo.batphone.responsemappers;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;

/**
 * Tracks Futures waiting for Responses and maps incoming Responses to their
 * waiting futures
 *
 * @param <T> the type of the responses this resolver can handle
 */
public interface ResponseMapper<T> {

	/**
	 * Maps responses to their waiting futures
	 *
	 * @param response
	 * @throws IOException
	 */
	void resolveResponse(T response) throws IOException;

	/**
	 * Creates a ListenableFuture that be used to get the Response for a Request
	 *
	 * @param requestID the correlation id between requests and their responses
	 * @return a ListenableFuture that can be used to get responses to request
	 */
	ListenableFuture<T> makeFuture(String requestID);
}
