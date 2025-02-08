This proxy is designed to be run on a computer and wired up to a web browser. When a GET request is made from the browser, the request is forwarded to the program, which then handles the request. All other requests (POST, PUT, etc.) are ignored. The program then forwards the request to the destination, retrieves that request, then writes it to a file, and stores the filename and the server address in memory. When another request is made from the same URL, the program will check the in memory cache for that URL, and if it is found, it will forward the cached version of that webpage to the browser.

# VS Code Support

files are included to support debugging through visual studio code. Simply open the repository in VS Code and make sure you have the Java Platform Extension for Visual Studio Code installed. This will allow you to set break points to inspect variables and debug the application.

# proxy setup

to get this project up and running, first make sure you have the Java SDK installed. BUild the files with

`javac .\Proxy\ProxyServer.java .\Proxy\RequestHandler.java`

and then run the program with

`java ProxyServer 8080`

you can replace the port number with whatever port you want, but the default is 8080.

# browser setup

Using firefox, click on the menu icon in the browser, then the settings option. Scroll down on the general tab until you get to Network Settings and select the settings button. You will then click the Manual proxy configuration radio button and enter localhost for the http proxy and the port you ran the program with in the port box. I have only tested this with HTTP, so lets just use the HTTP proxy for now.

# Test the http request

Using the browser, type in `http://example.com` If you have the program running in a console window, you should see all the appropriate logging that occurs.You will see that a log is made in the file proxy.log as well. This request should create a file that ends with `.dat` with a random filename associated with it. To test that the request successfully resends a cached response, refresh the browser page by hitting `ctrl + shift + r` to do a hard refresh. This bypasses the browser cache and resends the request to the proxy. You should see in the console log a message along the lines of  
`Cache hit for URL: http://example.com/`  
`Sending cached content from file: cached/J3lcENI9TV.dat`  
this is how you know that the proxy is effectively sending a cached response instead of making another network call.

# Challenges

My biggest challenge was writing the application for Java, which is a language I have no experience in. However since I am experienced in another object oriented language (C#) I was able to get something written that I could understand. THe two languages are very similar, but the libraries used for HTTP requests are completely different. So sometimes I would write the implementation in C# and then use Copilot to rewrite the code in Java. THe libraries were very different, but thanks to my background as a dotnet developer, I was able to hobble through it.

Another challenge was in properly debugging network requests, since there were socket itemout issues and IO timing issues associated with debugging the program. WIth the request handler being multithreaded and asynchronous, there are a lot of requests that get fired off at the same time, so while I was dubbing one request, other requests were hitting the proxy and resetting my breakpoint. For this reason I had to turn off a number of features in Firefox as well as restrict the proxy to only HTTP requests.

Finally, I was challenged to implement all of the requirements for proxy servers as outlined in RFC 2616, I attempted to implement some requirements for handling headers that you can see on line 56 of RequestHandler.java.

```
String[] lines = requestString.split("\r\n");
boolean connectionKeepAlive = false;
for (String line : lines) {
    if (line.toLowerCase().startsWith("connection: keep-alive")) {
    connectionKeepAlive = true;
    break;
    }
}
```

After determining that the header is set to keep the connection open or not, the proxy attempts to honor the value of this header. This is mentioned in RFC 2616: "HTTP/1.1 proxies MUST parse the Connection header field before a message is forwarded". This is only one small requirement among many that are mentioned in the document in regards to handling headers. Unfortunately there were so many small details regarding headers I decided it would impossible to implement and test them all within the scope of this application.

# Limitations
There are a number of obvious limitations. It only processes http requests, it only works with simple http websites that contain minimal source files, it does not fully implement all of the requirements for a http 1.1 proxy server, and it only handles GET requests. If I had more time, I would add more testing with different web pages to see if it can handle more complex applications like SPAs and other javascript heavy pages. The in memory caching is also severly limited. I think the default size of the hashmap was 16, so I had to increase that to the max int 32 size of 2147483647. Still, that is a small number, and it could only hold that amount of key value pairs for in memory caching before the application blows up. I also came upon some http encoding problems with some sites where the text stored text file was not encoding the body of the message properly. Other than that, i'm actually surprised it works! At least it does with http://example.com when it runs on my machine!