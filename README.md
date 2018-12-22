# transaction-service

Small service that puts transactions into a in-memory database and provides the statistics summary of the last 60s.

## Specs

1. 2 endpoints: `POST /transaction | body: amount: Double, timestamp: Timestamp) | response 201/204` and `GET /statitics`
2. `GET /statistics` should be _O(1)_.
3. Both endpoints should be _thread-safe_.
4. Possible to post older transactions.

## Reasonings

- For Spec#2 I thought ConcurrentSkipListMap would be enough but found out its get method's runtime complexity is rather O(log(n)), so that's excluded.
- Since syncrhonized HashMap locks at object level, I chose for the ConcurrentHashMap implementation that uses lock stripping mechanism and its get/put runtime complexity is also O(1), given its hashcode is correct.
- ConcurrentHashMap solves the thread-safe caching for a single instance of the application, but doesn't work properly in multiple-instances as the in-memory cache will be different among instances.
 But since the database is also in-memory anyways, multiple-instance doesn't seem to be the scope of this exercise. Therefore, only single instance approach will be considered. 
- To maintain both a database and a fast O(1) statitics retrieval, a strategy of cache eviction was used (a job run every 1s).

## Database

- In memory H2 for the transactions (easily switchable to production using JPA).
- ConcurrentHashMap for caching the last 60s transactions.
- 1s eviction strategy for the cache.
- Older transactions are still stored in the database.

## Stack

- Java 8
- Spring boot

## Development mode

- TDD (continuously coded test cases followed by production code)

## Running the app

- ./gradlew bootRun

## Testing

- ./gradlew test