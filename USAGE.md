# nomopojo API examples:

Here are the use cases supported by nomopojo.
The examples use a collection called 'zips', which is publicly available [1] from MongoDB's website.  The attributes in this collection are:

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

[1] http://media.mongodb.org/zips.json

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

Nomopojo supports the 'order_by', 'limit' and 'skip' parameters which make it simple to implement basic pagination.

Request:
`GET http://localhost:7001/nomopojo/api/zips?state=CA&limit=10&skip=30&order_by=city`

Using a page size of 10 (limit=10), get the fourth page of zip codes for California (skip=30), sorting by city (order_by=city)

Response:
`[ {  <each mongo document >, ... } ]`


## Create new record with specific ID.

Request:
`POST http://localhost:7001/nomopojo/api/zips` 

with a payload of: 
` { _id: '00000', city:'SILLY VALLEY', 'loc': [1.1, 2.2], 'pop' : 1, state:'CA' }`
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

If you need a much more complete REST-on-mongo implementation, I would gladly point you to the RESTHeart API server, by SoftInstigate [2].   Ah, if only I could deploy RESTheart as a Servlet... [3]

[2] https://github.com/SoftInstigate/restheart
[3] https://github.com/SoftInstigate/restheart/issues/49
