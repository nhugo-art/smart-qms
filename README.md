# Smart QMS

Smart QMS is a Java Swing classroom simulation for six university service counters. The Customer and Administrator views share one in-memory session. Restarting the app clears all tickets.

## Build and run

Install JDK 25 and Maven. Confirm that `mvn -version` reports Java 25, then run these commands in the project directory.

```sh
mvn clean verify
java -jar target/smart-qms-1.0.0.jar
```

Run just the JUnit 5 suite with `mvn test`. The desktop app needs a graphical environment.

## Demonstration

1. Continue as Customer. Create three Low Finance tickets, then one each at Medium, High, and Emergency. Create tickets at Registration and IT Support to compare independent queues.
2. Return to Role selection and continue as Administrator. Open **Queues & counters**, then select Finance. The FIFO table shows arrival order and the heap table shows its internal array order. The counter displays the next waiting ticket.
3. Call and complete each ticket. Finance serves Emergency, High, Medium, and then Low in arrival order. Try calling twice while one ticket is serving. Skip a ticket and find it by ID afterward.
4. Open **Search**. Linear search traverses waiting and serving tickets. Binary search sorts a session snapshot lexicographically by ID using merge sort before searching. Hash lookup uses the ticket ID index. Use Binary Search or Hash Lookup for completed or skipped tickets.
5. Open **Reports** and try all three sorting algorithms. Sorting by waiting duration includes waiting tickets and puts the longest waits first. Open **Analytics** to see counts and the average elapsed time from creation to service start for completed tickets.
6. Return to Customer and check a ticket's current status. The estimated position counts waiting tickets ahead at lookup time and changes as the queue changes.
7. Use **Reset Simulation** on the dashboard to clear the session and restart numbering. **Load Demo Data** adds a deterministic set of 26 tickets across the six services to the current session.

**Return to Waiting** restores a serving ticket to its original arrival order within its priority level. For a completed ticket that was returned earlier, its wait ends at the service start that led to completion.

## Source layout

| Package | Responsibility |
| --- | --- |
| `model` | Tickets, service configuration, status transitions, and each service's waiting structures |
| `dsa` | Circular-array FIFO, array-backed binary max-heap, linear and binary search, and insertion, quick, and merge sort |
| `service` | Shared session, ticket creation and lookup, counter operations, search, and reporting |
| `gui` | Swing role navigation, customer screens, and administrator screens |

The FIFO uses amortized O(1) enqueue, O(1) dequeue and peek. The heap uses O(log n) insert and extraction, O(1) peek, and O(n) bottom-up heapify. Equal heap priorities compare by earliest arrival sequence. The ID index uses Java `HashMap`, with average O(1) lookup and a conservative O(n) worst-case bound. Search comparisons in the UI count key comparisons during search. Report algorithms sort copies of the records and leave the FIFO and heap intact.
