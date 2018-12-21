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

## Database

- In memory H2 for the transactions.
- ConcurrentHashMap for caching the last 60s transactions.

## Stack

- Java 8 with Spring boot

## Development mode

- Used TDD.

## Running the app

- ./gradlew bootRun

## Testing

- ./gradlew test