package com.timgroup.ahcleak.client;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;

public class ResponseHandler extends AsyncCompletionHandler<String> {

    @Override
    public String onCompleted(Response response) throws Exception {
        return response.getResponseBody();
    }

}
