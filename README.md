# nomopojo
nomopojo is a set of generic Java CRUD servlets to provide REST services that does NOT use POJOs.

#### WARNING: 

 * STAND BACK!  This is very early pre-pre-alpha software! (at least until I remove this warning!)

#### The name
nomopojo stands for "No More POJOs".  In the Java/J2EE world, there are ton of ways
to set up REST services as Servlets, but almost all of them require you to declare your
object layers as Plain Old Java Objects (also known as DTOs, VO, Beans, what have you).

#### Background on POJOs
The POJO-everywhere mentality made a certain amount of sense back in the RDBMS days when database
schemas were changed only with new releases of software.  The ease at which POJOs can be generated from
JDBC drivers is well-established, and a whole sub-speciality of mappers has arisen: JPA, Hibernate,
Spring Framework, JAXB, Jackson, Jersey, etc.  Part of the accepted paradigm is that these POJO channge
when the database changes, and this all works well enough within one organizaations' applications.

#### JSON is the new way to go.
Now, flexible JSON payloads dominate both client-to-server and even server-to-server interactions
across organizational/corporate boundaries.  The schema-less property of JSON objects lets any
party add data elements easily, without having to coordinate these changes with other parties as
tightly as with other formats.   It is important that when your company's business partners add
new data attributes that your company can at least NOT LOSE THEM, but this is precisely what
happens when the majority of J2EE-based REST frameworks try to map an incoming JSON object to a POJO.
Unmapped attributes simply get DROPPED.  Put another way, the compile-time definition of a POJO
class becomes a destructive filter on all of your incoming JSON payloads.  And of course, modifying
your POJOs requires code modifications, repackaging, retesting, and re-releasing.  

#### JSON is the anti-POJO
In environments where JSON is both on the client and the database, it makes ZERO sense to put POJOs in the middle.
The POJO pattern effectively throws every EVERY benefit using flexible JSON formats in the first place.

#### Why I am developing nomopojo
I spent some time looking for a generic REST servlet for MongoDB that doesn't involve POJOs and is
implemented as a normal Java Servlet.  The closest thing I found was RestHeart [1].  I was able to immediately use it in developing my Angular JS frontend, writing to a MongoDB database via RestHeart.  Unfortunately, I am required 
to deploy my work to Weblogic 12c (not my preference), which is a Serlet 3.0 container. RestHeart 
is written directly to the Undertow.io server [2], not the Servlet specification.  So, as great as 
Undertow.io is, I cannot use it for my day job.

#### Conclusion
This is my attempt to fill this gap: providing a generic REST servlet for MongoDB via the Servlet 3.0 API.

Implementing REST on top of JSON is trivial to do in most programming languges, like Javascript/Node/Express, 
or Python/Flask/Django, and has been implemented dozens of times.  But Java, even after 20+ years, still 
lacks the concept of a first-class Java Object literal.   Once you realize that, it is easy to see why so 
many Java frameworks continue to exhibit the POJO-fetishes.  It then follows that naîve developers who lack 
experience outside of Java simply cannot conceive of any other way to do it.  

#### Roadmap
It turns out I implemented a generic read-only REST servlet in Java a few years ago, but using JDBC as an 
input source.  I hope to add that servlet to nomopojo as well, once my current workload settles down 
(maybe early 2016).  I am open to expanding nomopojo to other NoSQL-type of backend databases.



# nomopojo API examples:

Here are the use cases supported by nomopojo.
The examples use a collection called 'zips', which is publicly available [3] from MongoDB's website.  The attributes in this collection are:

`    {
        "_id": "10001",
        "city": "NEW YORK",
        "loc": [
            -73.996705,
            40.74838
        ],
        "pop": 18913,
        "state": "NY"
    }`


---

## Get all documents

Request:

`GET http://localhost:7001/nomopojo/api/zips`

Response:

`[ {  <each mongo document >, ... } ]`

## Get one document by ID
Request:

`GET http://localhost:7001/nomopojo/api/zips/10001`

Response:

`[ {  <each mongo document > } ]`

Note that there is still an enclosing array, but only one element in that array.

## Get multiple, with pagination support

Nomopojo supports the 'order-by', 'limit' and 'skip' parameters which make it simple to implement basic pagination.

Request:
`GET http://localhost:7001/nomopojo/api/zips?state=CA&limit=10&skip=30&order_by=city`

Using a page size of 10 (limit=10), get the fourth page of zip codes for California (skip=30), sorting by city (ordery=city)

Response:
`[ {  <each mongo document >, ... } ]`


## Create new record with specific ID.

Request:
`POST http://localhost:7001/nomopojo/api/zips` 

with a payload of: 
` { _id: '00000', city:'SILLY VALLEY', 'loc': [1.1, 2.2], 'pop' : 1, state:'CA' } `
Response:
`{ "inserted": 1 }`
Client should verify that `inserted` does return 1.

## Update existing record

Request:
`http://localhost:7001/nomopojo/api/zips/00000`
with a payload of:
`{ city:'SILICON VALLEY' }`

Response:
`{"upsertedId":null,"modified":1,"matched":1}`
Client should verify that `modified` does return 1.

## Deleting existing record:

Request:
`http://localhost:7001/nomopojo/api/zips/00000`
No payload at all.

Response:
`{"deleted":1}`
Client should verify that `deleted` does return 1.

## Next steps
The preceding list of usecases is very basic.  Much more could be done with it in the future.  I cannot guarantee that nomopojo will evolve a lot past this state.

If you need a much more complete REST-on-mongo implementation, I would gladly point you to the RESTHeart API server, by SoftInstigate.   Ah, if only I could deploy RESTheart as a Servlet... [4]


[1] https://github.com/SoftInstigate/restheart

[2] http://undertow.io

[3] http://media.mongodb.org/zips.json

[4] https://github.com/SoftInstigate/restheart/issues/49
