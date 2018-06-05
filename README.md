
## Salsify Coding Assignment


#### How does your system work?

Addressing only the salient bits here, the application essentially serves the requested
line of a "Very Large File" (**VLF**) by directly accessing the file on disk. At startup
the application creates an in-memory index of the file in a map. For each line number,
we store the byte offset to the requested line. Using a 
RandomAccessFile we can set a file pointer to the start byte of the requested line and 
read bytes until a `newline` or `EOF` is encountered. The application converts the resulting 
byte array into a String which is then returned to the client.

The email I received with the assignment indicated I should steer clear of putting the data into a 
database, which I interpreted to mean any data store (RDBMS, NoSql, etc). This,
along with the knowledge that we might be dealing with a VLF, informed my decision
to read simply the data directly from the file on disk. 

Instead of packaging a large file with the codebase, the application generates a 100MB text 
file on startup (if not already generated).  Each line is roughly 200 bytes and consists of 
randomly generated "words" of varying length.  This takes about a minute on my mac.  It takes
less than 1 second to create the in-memory line index.

#### How will your system perform with a 1 GB file? a 10 GB file? a 100 GB file?

I measured a zero millisecond difference between accessing the first and last lines of the file. 
I'm fairly confident that scaling up the file size will also perform well. 

One concern with a VLF might be the size of the in-memory index. 
A 100GB file of 200 byte lines could result in a 8GB in-memory index.
One possible optimization is to not index every line. For example, if we index every 10th
line we can still compute our desired line efficiently with a bit of `newline` counting
and it cuts our index size by an order of magnitude.  

#### How will your system perform with 100 users? 10000 users? 1000000 users?

I performed some quick and dirty concurrency tests to see how we perform when 
having many open file pointers to the same file. I also implemented caching 
(using the out-of-the-box Spring Boot configuration with EHCache)
since it seems like a good assumption some lines might be accessed more 
frequently than others.

In my testing, I found I was CPU bound, rather than IO bound. This is a really
good sign. It indicates I could have significantly ramped up the number of concurrent
requests without impacting performance much. Of course, I have the file resident on
the filesystem. In a real system, this file would likely be accessed by a bank of 
web servers via the network (NAS or S3). Network access to the file adds significant
latency. The good news is that it appears a large number of concurrent requests
to a single file can be made (up to the system's limit of open file handles?)
without impacting performance.

Here is a graphic showing a request for 50,000 random lines in the file using 8 worker
threads. Notice we are CPU bound with nearly zero time spent collecting garbage and
very little impact on heap memory usage.

![](https://www.evernote.com/l/AGY3afCLZBVN65hbH-iYTIEwqpRmJjBixJ4B/image.png)

On my mac, 8 threads was optimal, which makes sense with 4 cores and hyperthreading.
Anything over that and you start to lose performance due to scheduling/switching. 
Caching provided no utility for this test due to random line access. To increase the
number of concurrent requests, I would need to add more CPU cores.

#### What documentation, websites, papers, etc did you consult in doing this assignment?

My first resource was Google, which turns out to be a great index for StackOverflow. I
was probably evenly split between StackOverflow, various blogs found via Google, and to a
lesser degree, the Spring Boot docs. In total I probably spent about an hour or two in
research for this project.

#### What third-party libraries or other tools does the system use? How did you choose each library or framework you used?

* Spring Boot: Such a great project for getting up and running quickly. Using this allowed
me to focus on the problem at hand and eliminated any yak shaving to get a REST API server
up and going. This is pretty much it. Everything else was of basic utility. 
Spring Boot is the workhorse of this project and was the obvious choice. 
I used just the very basics of what this framework has to offer. 
* EHCache: Used as a plugin to Spring Boot's caching mechanism. Required almost no code to setup.
* Guava/Vavr: I'm lumping these together since they provided some utility but were not
foundational to the solution.
 
#### How long did you spend on this exercise? If you had unlimited more time to spend on this, how would you spend it and how would you prioritize each item?

In total I spent about 8 hours. About 2 of that was spent in research/documentation.
I spent a good percentage of time reading about performance / concurrency of file access
as I worked to nail down the approach (partition file or no? how to index into the file, etc)
Coding up the solution took about a third of the time and turned out to be very straightforward.
Finally, I spent an hour or more doing some quick and dirty performance testing using
some simple tooling I put together (in the project) to gather empirical evidence of various
concurrency loads. 

Given unlimited time, I would want to test with a much larger file. I'd also deploy the
app somewhere, perhaps Heroku or AWS via Docker, where I could load test in a production-like
setting.  I'd also rebuild the app using modern non-blocking Reactive APIs to get more experience
with them.

#### If you were to critique your code, what would you have to say about it?

Given that this project isn't designed to be a production system, a number of aspects
were overlooked. Apart from what was laid out in the requirements, I spent very little time 
worrying about exception handling. As a corollary to that, the application doesn't validate 
input. Also, The caching system should be tuned. For simplicity, I opted for the default 
configuration. If this were destined to be a real application these points would have 
been engineered properly.

With an assignment such as this, taking these shortcuts is a bit of a gamble since
I don't know how the project will be evaluated. It seemed more important to
focus on performance/scalability as the primary concerns and take other shortcuts
where I could, to save time.

