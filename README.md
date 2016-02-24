# Multithread-Web-Server
## Brief Introduction
This is a simple multithread web server implement through java socket programming, which handles the "GET" and "HEAD" http request
from client. Some features this web server has are:

* Handle multiple HTTP requests of differrent types of files like HTML, CSS, JS and common image types, through multi-threading, at a max number of threads that user specifies
* Identify different request and send back the proper response like 200, 400, 403, 404 with an HTML page displaying human-readable response

Note: this web server is not sophisticated enough yet to handle "POST" request. May consider to add this functionality in the future.

## Usage
$ javac WebServer.java

$ java WebServer maxConnection rootDirectory

This program assumes that the server's ip address is the localhost and it listens at port 8888. The user can open a browser window and enter something like "localhost:8888/index.html", and the browser will render the homepage of Carleton's CS department. 
