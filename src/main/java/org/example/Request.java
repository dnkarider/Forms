package org.example;

import org.apache.http.NameValuePair;

import java.util.List;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final String body;
    private final List<NameValuePair> nameValuePairs;
    private final String protocol;

    public Request(String method, String path, List<String> headers, String body, List<NameValuePair> nameValuePairs, String protocol) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.nameValuePairs = nameValuePairs;
        this.protocol = protocol;
    }
    public String getQueryParams(){
        StringBuilder result = new StringBuilder();
        for(NameValuePair nameValuePair : nameValuePairs){
            result.append(nameValuePair.getName()).append(" = ").append(nameValuePair.getValue()).append("\n\r");
        }
        return result.toString();
    }

    public String getQueryParam(String name){
        for(NameValuePair nameValuePair: nameValuePairs){
            if(name.equals(nameValuePair.getName())){
                return name + " = " + nameValuePair.getValue();
            }
        }
        return null;
    }

    public String getMethod() {
        return method;
    }
    public String getPath() {
        return path;
    }
    public List<String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }
    public String getProtocol(){
        return protocol;
    }
}