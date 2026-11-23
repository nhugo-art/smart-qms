# Product Requirements Document (PRD)

# Smart Queue Management System (Smart QMS)

**Document type:** MVP / Academic Project\
**Version:** 1.0\
**Target implementation:** Java 25 + Java Swing\
**Application type:** Single-user desktop simulation\
**Persistence:** In-memory only\
**Build tool:** Maven

------------------------------------------------------------------------

## 1. Product Overview

### 1.1 Purpose

Smart Queue Management System (Smart QMS) is a Java desktop application
that simulates queue operations at a university service center. It
demonstrates how Data Structures and Algorithms (DSA) can be applied to
a practical queue-management problem.

The application supports six independent university services. Customers
can select a service and generate a ticket. An administrator can operate
the service counters, call the next customer, update ticket status,
search tickets, and review basic operational statistics.

This is an academic MVP. It is not intended to be a production
multi-device queue platform.

### 1.2 Problem Statement

A university service center may handle several types of requests at the
same time. Managing all customers in one line can be inefficient because
each service has different staff and workloads. Urgent cases may also
need to be served before ordinary cases.

Smart QMS demonstrates a structured way to manage these requests through
independent service queues, FIFO ordering, priority handling, searching,
and sorting.

### 1.3 Product Goals

1.  Simulate a university service center with six services.
2.  Generate unique tickets for customers.
3.  Maintain an independent queue for each service.
4.  Serve ordinary customers in FIFO order.
5.  Serve urgent customers according to their priority level.
6.  Demonstrate custom implementations of selected data structures and
    algorithms.
7.  Provide a clear, minimalist Java Swing GUI that makes the system
    easy to demonstrate.
8.  Allow the lecturer to test the system through one application and a
    role selector.

### 1.4 Out of Scope

The MVP will not include:

-   A database of any kind.
-   File-based persistence or automatic data recovery.
-   A web application, mobile application, or REST API.
-   Network communication between multiple computers.
-   Real SMS, email, push, or mobile notifications.
-   Real authentication, password management, or account registration.
-   Real printer integration.
-   Production deployment, cloud hosting, or multi-tenant support.
-   Advanced forecasting or AI-based queue optimization.

All application data exists in memory and is cleared when the
application closes or is restarted.

------------------------------------------------------------------------

## 2. Users and Roles

The application has exactly two selectable roles.

### 2.1 Customer

The Customer role represents a person using the university service
center's ticket kiosk.

A customer can:

-   View the six available services.
-   Select one service.
-   Select a priority level from the options enabled by the application.
-   Generate a ticket.
-   View the generated ticket number, selected service, priority, and
    current queue position.
-   Check a ticket's current status using its ticket number during the
    current application session.

The Customer role cannot call or complete tickets, change service
configuration, or view administrator controls.

### 2.2 Administrator

The Administrator role also performs staff/counter operations. A
separate Staff role is intentionally not included in this MVP.

The administrator can:

-   View an overview of all services and queues.
-   Select a service/counter to operate.
-   Call the next eligible ticket.
-   Mark a ticket as completed, skipped, or returned to waiting, where
    appropriate.
-   View the currently serving ticket.
-   View waiting tickets for each service.
-   Search for tickets.
-   Sort ticket/history data using the implemented sorting algorithms.
-   View basic statistics and reports.
-   Reset the in-memory simulation.

The administrator can also generate sample/customer tickets when
preparing a demonstration, but customer-facing ticket creation should
remain available through the Customer role.

### 2.3 Role Selection

On application startup, show a simple role-selection screen with two
clear choices:

-   Continue as Customer
-   Continue as Administrator

The application must use one `JFrame`. Use `CardLayout` or an equivalent
navigation approach to switch between role screens without opening
separate applications.

Provide a visible way to return to the role selector. Returning to the
selector must not clear application data. Only the explicit Reset
Simulation action or restarting the application clears data.

This is a classroom simulation, not a security boundary. No login or
password is required.

------------------------------------------------------------------------

## 3. Services

The application must include these six services, each with its own
independent queue state:

1.  Registration
2.  Finance
3.  Transcript
4.  Student ID
5.  IT Support
6.  General Information

Requirements:

-   A ticket created for one service must never enter another service's
    queue.
-   Calling the next ticket for one service must not affect the other
    five services.
-   Each service must display its own waiting count, priority waiting
    count, and currently serving ticket.
-   Ticket IDs must be unique across the whole application session.
-   Ticket IDs should include a service-specific prefix, for example
    `REG-001`, `FIN-001`, and `IT-001`.
-   The ticket sequence must not be shared in a way that creates
    duplicate IDs across services. Each service may maintain its own
    sequence because the prefix makes IDs unique.

Suggested prefixes:

  Service               Prefix
  --------------------- --------
  Registration          REG
  Finance               FIN
  Transcript            TRN
  Student ID            SID
  IT Support            ITS
  General Information   GEN

------------------------------------------------------------------------

## 4. Priority and Queue Rules

### 4.1 Priority Levels

Every ticket has one of four priority levels:

  Priority      Rank Queue behavior
  ----------- ------ ---------------------
  Low              1 Ordinary FIFO queue
  Medium           2 Priority heap
  High             3 Priority heap
  Emergency        4 Priority heap

The priority level is assigned when a ticket is created. The UI must
clearly show the priority on ticket cards and queue tables.

For this academic MVP, the Customer role may select a priority level to
make the behavior easy to demonstrate. The interface should label this
as a simulation choice. In a real service center, priority would
normally be verified and controlled by staff.

### 4.2 Queue Structure per Service

Each service maintains:

-   One custom FIFO queue for Low-priority tickets.
-   One custom binary max-heap for Medium, High, and Emergency tickets.
-   A collection of completed/skipped tickets for the current session.
-   At most one currently serving ticket per simulated counter.

For this MVP, each service has one logical counter. The design may allow
the model to be extended to multiple counters later, but multiple
counters are not required.

### 4.3 Calling the Next Ticket

When the administrator selects **Call Next** for a service:

1.  If that service's priority heap is not empty, extract the ticket
    with the highest priority.
2.  If multiple tickets have the same priority, serve the
    earliest-created ticket first.
3.  If the priority heap is empty, dequeue the oldest ticket from the
    Low-priority FIFO queue.
4.  If both queues are empty, show a clear "No customers waiting"
    message.
5.  The selected ticket becomes `SERVING` and is displayed prominently.

Priority order:

`Emergency > High > Medium > Low`

Low-priority tickets preserve FIFO order. Tickets with equal non-Low
priority also preserve FIFO order through an arrival sequence/timestamp
tie-breaker.

### 4.4 Ticket Statuses

Use an enum for ticket status:

-   `WAITING`
-   `SERVING`
-   `COMPLETED`
-   `SKIPPED`

Rules:

-   A newly generated ticket starts as `WAITING`.
-   Calling a ticket changes it to `SERVING`.
-   Completing a serving ticket changes it to `COMPLETED`.
-   Skipping a serving ticket changes it to `SKIPPED`.
-   A skipped ticket must not silently disappear. It must remain visible
    in the current session's ticket records.
-   A completed or skipped ticket cannot be called again.
-   The application must prevent a service from calling another ticket
    while its single logical counter already has a `SERVING` ticket,
    unless the current ticket is first completed or skipped.

### 4.5 Queue Position

The Customer screen should display a queue position for the generated
ticket.

Because priority tickets may be called before Low-priority tickets, the
position is an estimate based on the current queue state, not a
guaranteed service order. The UI should label it as an estimated
position or show the number of tickets currently ahead.

The position must be recalculated when queue state changes. Do not
mutate the actual queues just to calculate a position.

------------------------------------------------------------------------

## 5. Functional Requirements

### FR-01: Role Selection

-   Show Customer and Administrator choices at startup.
-   Switch screens within the same main window.
-   Allow returning to role selection without clearing data.

### FR-02: Customer Service Selection

-   Display all six services.
-   Each service should have a clear name and short description.
-   Selecting a service opens the ticket creation view.

### FR-03: Ticket Creation

-   Allow selection of Low, Medium, High, or Emergency.
-   Generate a unique service-prefixed ticket ID.
-   Record creation time and priority.
-   Insert Low tickets into the service's FIFO queue.
-   Insert Medium, High, and Emergency tickets into the service's
    priority heap.
-   Show a ticket confirmation view.

### FR-04: Ticket Lookup

-   Allow a customer to enter a ticket ID.
-   Display service, priority, creation time, and current status.
-   If the ticket does not exist in the current session, show a friendly
    not-found message.

### FR-05: Administrator Dashboard

Display at least:

-   Total tickets created in the current session.
-   Total currently waiting.
-   Total currently serving.
-   Total completed.
-   Total skipped.
-   Waiting count per service.
-   Current ticket for each service, if any.

Statistics must be calculated from the in-memory application state, not
hard-coded sample values.

### FR-06: Queue Monitor

-   Show all six services.
-   Show each service's Low FIFO queue and priority queue.
-   Show priority labels and ticket IDs.
-   Allow the administrator to select a service and inspect its waiting
    tickets.
-   Refresh the displayed data after every queue operation.

### FR-07: Counter Operations

For the selected service, the administrator can:

-   Call Next.
-   Complete Serving Ticket.
-   Skip Serving Ticket.
-   View the current serving ticket.
-   See a message if no ticket is waiting.
-   See a message if a ticket is already being served.

### FR-08: Search

Implement and demonstrate:

1.  **Linear Search:** search current active/waiting tickets when an
    unsorted traversal is appropriate.
2.  **Binary Search:** search a sorted list of ticket IDs or another
    clearly documented sorted field. The input collection must be sorted
    before binary search is called.
3.  **Hash Lookup:** retrieve a ticket directly by ticket ID through a
    hash-based index.

The UI should allow the administrator to choose or demonstrate the
search method. Show whether a ticket was found and, where practical, the
number of comparisons or lookup steps.

Do not claim that binary search is being used on an unsorted collection.

### FR-09: Sorting

Implement these algorithms manually:

1.  Insertion Sort.
2.  Quick Sort.
3.  Merge Sort.

Allow the administrator to sort a current-session ticket/report table by
at least:

-   Ticket ID.
-   Creation time.
-   Priority.
-   Waiting duration (for tickets that are still waiting).

The UI should allow selection of the sorting algorithm and sort field.
The sorting implementation must operate on a copy or view of the data so
that sorting a report does not change queue order or ticket service
order.

Display the selected algorithm and its expected time complexity in the
UI or a small information panel.

### FR-10: Analytics and Reports

Show session-based statistics:

-   Total tickets created.
-   Waiting tickets.
-   Serving tickets.
-   Completed tickets.
-   Skipped tickets.
-   Average waiting time for completed tickets.
-   Ticket counts by service.
-   Ticket counts by priority.

Waiting time is the duration from ticket creation to the time service
begins. For completed tickets, record both service-start and completion
timestamps if displaying service duration.

All statistics must be calculated from real in-memory records. If there
is no data, show an empty state rather than fabricated values.

Charts are optional. Simple statistic cards and a table are sufficient
for the MVP.

### FR-11: Reset Simulation

-   Provide an administrator-only Reset Simulation action.
-   Ask for confirmation before resetting.
-   Clear all tickets, queues, ticket indexes, counters, and session
    statistics.
-   Restart ticket numbering for each service.
-   Return to a clean state.

### FR-12: Demo Data (Recommended)

Provide an optional **Load Demo Data** action for the administrator.

-   Generate a small, deterministic set of tickets across all six
    services.
-   Include all four priority levels.
-   Use the same ticket creation and queue insertion methods as normal
    ticket creation.
-   Make it possible to reset the simulation afterward.

Demo data must not be loaded automatically unless explicitly selected.

------------------------------------------------------------------------

## 6. Data Structures and Algorithms Requirements

This project is assessed as a Data Structures & Algorithms application.
Core structures must be implemented manually and used by the actual
application.

### 6.1 Custom FIFO Queue

Create a custom queue implementation, preferably using a circular array
or linked nodes.

Required operations:

-   `enqueue(Ticket ticket)`
-   `dequeue()`
-   `peek()`
-   `isEmpty()`
-   `size()`
-   `clear()`
-   Traversal or safe snapshot method for displaying queue contents

Expected complexity:

  Operation   Complexity
  ----------- -----------------------------------------------------
  Enqueue     O(1) amortized or O(1), depending on implementation
  Dequeue     O(1)
  Peek        O(1)
  isEmpty     O(1)
  Size        O(1)

Do not use Java's built-in `Queue`, `Deque`, `ArrayDeque`, or
`LinkedList` as the implementation of the required FIFO queue.

### 6.2 Custom Binary Max-Heap

Create a custom array-backed binary max-heap for Medium, High, and
Emergency tickets.

The heap comparison must use:

1.  Higher priority rank first.
2.  Earlier arrival sequence first when priority ranks are equal.

Required operations:

-   `insert(Ticket ticket)`
-   `extractMax()`
-   `peek()`
-   `isEmpty()`
-   `size()`
-   `heapify()` or an equivalent build operation
-   `clear()`
-   Safe snapshot/traversal for display

Expected complexity:

  Operation     Complexity
  ------------- ----------------------------------------------
  Insert        O(log n)
  Extract max   O(log n)
  Peek          O(1)
  Build heap    O(n), if implemented using bottom-up heapify

Do not use Java's built-in `PriorityQueue` for the required priority
queue.

### 6.3 Hash Lookup

Use a hash-based index keyed by ticket ID.

For a stronger DSA demonstration, implement a simple custom hash table
with collision handling (chaining or open addressing). If time is
limited, Java `HashMap` is acceptable for the lookup index, but the
PRD's core FIFO queue and binary heap must remain custom
implementations.

Document the average and worst-case lookup complexity.

### 6.4 Searching Algorithms

Implement linear and binary search manually. Binary search must only
receive data sorted according to the same comparison key used by the
search.

### 6.5 Sorting Algorithms

Implement insertion sort, quick sort, and merge sort manually. Do not
delegate the actual sorting to `Arrays.sort()` or `Collections.sort()`.

Use a comparator or clearly defined key strategy so the algorithms can
sort tickets by different supported fields.

### 6.6 DSA Visibility

The application should make the DSA behavior understandable to a
lecturer:

-   Show separate Low FIFO and priority heap sections for each service.
-   Show the priority level of each ticket.
-   Show the next ticket selected by the queue manager.
-   Provide a search-method selector.
-   Provide a sorting-algorithm selector.
-   Include a concise explanation of the selected algorithm and its
    complexity.

------------------------------------------------------------------------

## 7. GUI and User Experience Requirements

### 7.1 Design Direction

Use a minimalist modern desktop design.

Suggested visual direction:

-   Light background.
-   White or very light cards.
-   Dark navy text.
-   One restrained accent color such as teal or blue.
-   Clear spacing and alignment.
-   Consistent typography.
-   Simple icons where useful.
-   Avoid excessive gradients, shadows, animations, and decorative
    elements.

The GUI should look like a small modern university service-center
application, not a web page copied into a desktop window.

### 7.2 Main Window

-   One main `JFrame`.
-   Recommended initial size: approximately 1200 × 800.
-   Set a reasonable minimum size, such as 1000 × 650.
-   Use `BorderLayout` for the main shell.
-   Use `CardLayout` for screen navigation.
-   Use reusable Swing components for cards, buttons, headers, and
    tables.
-   Ensure layouts resize gracefully.

### 7.3 Customer Screens

Required views:

1.  Customer home/service selection.
2.  Ticket priority selection.
3.  Ticket confirmation.
4.  Ticket status lookup.

The ticket confirmation view should prominently display the ticket ID.

### 7.4 Administrator Screens

Required views:

1.  Admin dashboard.
2.  Queue monitor.
3.  Service/counter operation view.
4.  Ticket search.
5.  Sorting and report view.
6.  Analytics view.

A sidebar or top navigation bar may be used. Keep navigation consistent.

### 7.5 Tables and Empty States

-   Use `JTable` for ticket lists and reports.
-   Use meaningful column names.
-   Use readable date/time formatting.
-   Use visual labels for priority and status.
-   Show helpful empty states when no tickets exist.
-   Confirm destructive actions such as Reset Simulation.

### 7.6 GUI Threading

-   Create and update Swing components on the Event Dispatch Thread.
-   Use `SwingUtilities.invokeLater` to start the GUI.
-   Use `javax.swing.Timer` only for lightweight periodic UI refresh if
    required.
-   Do not perform slow work on the Event Dispatch Thread.
-   Do not update Swing components directly from background threads.

------------------------------------------------------------------------

## 8. Technical Stack

  Area             Technology / Choice
  ---------------- ------------------------------------------------------------
  Language         Java 25
  GUI framework    Java Swing
  Desktop window   `JFrame`
  Navigation       `CardLayout`
  Layouts          `BorderLayout`, `GridBagLayout`, `FlowLayout`, `BoxLayout`
  Tables           `JTable` with `TableModel`
  Build            Maven
  Testing          JUnit 5
  Data storage     In-memory Java objects only
  Charts           Optional; avoid adding a chart dependency unless needed
  IDE              IntelliJ IDEA or another Java 25-compatible IDE

### 8.1 Java Version

Use Java 25 as the project language/runtime target.

Configure Maven compiler settings for Java 25. Do not silently downgrade
the project to Java 17 or 21.

### 8.2 Dependencies

Keep dependencies minimal.

Required development/test dependency:

-   JUnit Jupiter (JUnit 5) for unit tests.

No database driver, ORM, web framework, or backend framework is
required.

Avoid adding a third-party Swing theme library unless there is a clear
benefit. Prefer standard Swing components with reusable styling helpers.

------------------------------------------------------------------------

## 9. Suggested Architecture

Use a simple layered architecture:

``` text
Presentation Layer (Swing)
        |
        v
Application / Service Layer
        |
        v
DSA Layer + Domain Model
        |
        v
In-Memory State
```

### 9.1 Presentation Layer

Responsible for:

-   Rendering screens.
-   Collecting user input.
-   Displaying ticket and queue information.
-   Calling application service methods.
-   Refreshing views after operations.

It must not contain queue or heap algorithms.

### 9.2 Application / Service Layer

Responsible for:

-   Creating tickets.
-   Routing tickets to the correct service queue.
-   Calling the next ticket.
-   Updating ticket status.
-   Searching tickets.
-   Sorting report snapshots.
-   Calculating statistics.

### 9.3 DSA Layer

Responsible for custom data structure and algorithm implementations.

### 9.4 Domain Model

Suggested classes:

-   `Ticket`
-   `ServiceType`
-   `ServiceQueue`
-   `Counter`
-   `TicketPriority`
-   `TicketStatus`

### 9.5 In-Memory State

A central application state object should own:

-   The six service queue instances.
-   Ticket ID index.
-   All ticket records for the current session.
-   Per-service ticket sequence counters.
-   Current serving ticket for each service.

Use one shared application state so switching roles or screens does not
create duplicate queue instances.

------------------------------------------------------------------------

## 10. Suggested Package and Project Structure

Use a Maven project with a package structure similar to:

``` text
smart-queue-management/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── smartqms/
    │               ├── Main.java
    │               ├── app/
    │               │   ├── ApplicationContext.java
    │               │   └── AppState.java
    │               ├── model/
    │               │   ├── Ticket.java
    │               │   ├── ServiceType.java
    │               │   ├── TicketPriority.java
    │               │   ├── TicketStatus.java
    │               │   └── ServiceQueue.java
    │               ├── dsa/
    │               │   ├── ArrayQueue.java
    │               │   ├── BinaryMaxHeap.java
    │               │   ├── CustomHashTable.java
    │               │   ├── SearchAlgorithms.java
    │               │   └── SortingAlgorithms.java
    │               ├── service/
    │               │   ├── QueueManager.java
    │               │   ├── TicketService.java
    │               │   ├── SearchService.java
    │               │   ├── ReportService.java
    │               │   └── DemoDataService.java
    │               ├── gui/
    │               │   ├── MainFrame.java
    │               │   ├── NavigationController.java
    │               │   ├── components/
    │               │   ├── common/
    │               │   ├── role/
    │               │   ├── customer/
    │               │   └── admin/
    │               └── util/
    │                   ├── DateTimeFormatterUtil.java
    │                   └── UiStyle.java
    └── test/
        └── java/
            └── com/
                └── smartqms/
                    ├── dsa/
                    ├── service/
                    └── model/
```

This is a suggested structure. Keep it understandable and avoid creating
classes that do not have a clear responsibility.

### 10.1 Important Class Responsibilities

  Class                 Responsibility
  --------------------- ------------------------------------------------------
  `Ticket`              Ticket ID, service, priority, timestamps, and status
  `ServiceType`         Defines the six services and their ticket prefixes
  `ServiceQueue`        Owns one service's FIFO queue and priority heap
  `ArrayQueue`          Custom FIFO queue implementation
  `BinaryMaxHeap`       Custom priority queue implementation
  `CustomHashTable`     Ticket ID lookup index
  `SearchAlgorithms`    Linear and binary search
  `SortingAlgorithms`   Insertion, quick, and merge sort
  `QueueManager`        Coordinates all six service queues
  `TicketService`       Creates and looks up tickets
  `ReportService`       Produces sorted report snapshots and statistics
  `AppState`            Holds shared in-memory state
  `MainFrame`           Main Swing application window

------------------------------------------------------------------------

## 11. Domain Model Requirements

### 11.1 Ticket

Suggested fields:

-   `String ticketId`
-   `ServiceType serviceType`
-   `TicketPriority priority`
-   `TicketStatus status`
-   `LocalDateTime createdAt`
-   `LocalDateTime serviceStartedAt`
-   `LocalDateTime completedAt`
-   `long arrivalSequence`

The `arrivalSequence` must be unique within the application session and
used as a deterministic tie-breaker for tickets with equal priority.

### 11.2 ServiceType

Enum values:

-   `REGISTRATION`
-   `FINANCE`
-   `TRANSCRIPT`
-   `STUDENT_ID`
-   `IT_SUPPORT`
-   `GENERAL_INFORMATION`

Each enum value should contain a display name and ticket prefix.

### 11.3 TicketPriority

Enum values:

-   `LOW(1)`
-   `MEDIUM(2)`
-   `HIGH(3)`
-   `EMERGENCY(4)`

### 11.4 TicketStatus

Enum values:

-   `WAITING`
-   `SERVING`
-   `COMPLETED`
-   `SKIPPED`

### 11.5 ServiceQueue

Each `ServiceQueue` must contain:

-   Its `ServiceType`.
-   One custom FIFO queue for Low-priority tickets.
-   One custom binary max-heap for Medium, High, and Emergency tickets.
-   The current serving ticket, if any.

Do not store the same waiting ticket in both queue structures.

------------------------------------------------------------------------

## 12. Validation and Error Handling

The application must:

-   Reject blank or invalid ticket lookup input.
-   Prevent duplicate ticket IDs.
-   Prevent calling the next ticket when a service already has a serving
    ticket.
-   Prevent completing or skipping when no ticket is serving.
-   Prevent invalid status transitions.
-   Handle empty queues without throwing an uncaught exception.
-   Display user-friendly error and confirmation messages.
-   Avoid application crashes from invalid user input.
-   Handle empty report/search results gracefully.

Use clear return values or domain-specific exceptions in the service
layer. Do not use GUI message dialogs as the application service layer's
error-handling mechanism.

------------------------------------------------------------------------

## 13. Non-Functional Requirements

### NFR-01: Usability

A first-time user should be able to generate a ticket and an
administrator should be able to call the next ticket without
instructions.

### NFR-02: Responsiveness

Normal queue operations and screen changes should feel immediate for
classroom-sized datasets.

### NFR-03: Maintainability

Separate GUI, business logic, domain models, and DSA implementations.

### NFR-04: Correctness

Queue ordering, priority ordering, status transitions, and search/sort
results must be deterministic and testable.

### NFR-05: Portability

The application should run on operating systems supported by a
compatible Java 25 runtime.

### NFR-06: Data Lifecycle

All data is in memory. Display a brief notice that restarting the
application clears the simulation.

### NFR-07: Demonstrability

A lecturer should be able to observe the queue structures and trigger
the algorithms through the GUI without reading source code first.

------------------------------------------------------------------------

## 14. Acceptance Criteria

The MVP is considered complete when all of the following are true:

### Application and Navigation

-   [ ] The project builds and runs using Java 25 and Maven.
-   [ ] One main application window is used.
-   [ ] Customer and Administrator roles are selectable.
-   [ ] Switching roles does not clear application state.
-   [ ] The GUI uses a consistent minimalist design.

### Services and Tickets

-   [ ] All six services are available.
-   [ ] Each service has independent queue structures.
-   [ ] Tickets receive unique service-prefixed IDs.
-   [ ] Customers can create tickets with all four priority levels.
-   [ ] Ticket status can be looked up during the session.

### Queue Behavior

-   [ ] Low-priority tickets are served in FIFO order.
-   [ ] Emergency tickets are served before High tickets.
-   [ ] High tickets are served before Medium tickets.
-   [ ] Medium tickets are served before Low tickets.
-   [ ] Equal-priority tickets are served in arrival order.
-   [ ] A service cannot call a second ticket while one is already
    serving.
-   [ ] Operations on one service do not change another service's
    queues.

### Administrator Operations

-   [ ] Administrator can call the next ticket.
-   [ ] Administrator can complete a serving ticket.
-   [ ] Administrator can skip a serving ticket.
-   [ ] Queue views refresh after operations.
-   [ ] Dashboard statistics reflect actual session data.

### Algorithms

-   [ ] Custom FIFO queue passes unit tests.
-   [ ] Custom binary max-heap passes unit tests.
-   [ ] Hash lookup works for ticket IDs.
-   [ ] Linear search returns correct results.
-   [ ] Binary search returns correct results on sorted input.
-   [ ] Insertion sort returns correctly ordered data.
-   [ ] Quick sort returns correctly ordered data.
-   [ ] Merge sort returns correctly ordered data.
-   [ ] Sorting reports does not mutate actual queue order.

### MVP Constraints

-   [ ] No database is used.
-   [ ] No backend or network service is required.
-   [ ] Data is cleared when the application restarts.
-   [ ] Reset Simulation clears all in-memory state after confirmation.
-   [ ] The application handles empty queues and invalid input
    gracefully.

------------------------------------------------------------------------

## 15. Testing Plan

Use JUnit 5 for unit tests.

### 15.1 FIFO Queue Tests

-   Enqueue multiple tickets and verify dequeue order.
-   Verify peek does not remove an item.
-   Verify empty queue behavior.
-   Verify size after enqueue/dequeue.
-   Verify clear.

### 15.2 Binary Heap Tests

-   Insert tickets with different priorities.
-   Verify Emergency is extracted before High, Medium, and Low-priority
    tickets if Low is represented in a heap test.
-   Verify equal-priority tickets are extracted in arrival order.
-   Verify heap property after insert and extraction.
-   Verify empty heap behavior.
-   Verify clear and size.

In the actual application, Low-priority tickets are stored in the FIFO
queue, not in the priority heap.

### 15.3 Queue Manager Tests

-   Verify each service has independent queues.
-   Verify priority tickets are called before Low tickets.
-   Verify Low tickets preserve FIFO order.
-   Verify equal-priority urgent tickets preserve FIFO order.
-   Verify a second call is rejected while a ticket is serving.
-   Verify completion and skip operations update status correctly.

### 15.4 Search and Sorting Tests

-   Test each search algorithm with found and not-found cases.
-   Test binary search only with correctly sorted input.
-   Test each sorting algorithm with empty, single-item, duplicate-key,
    and larger input cases.
-   Verify sorting a report snapshot does not change the underlying
    service queues.

### 15.5 UI / Manual Test Scenarios

1.  Start the application and select Customer.
2.  Create tickets in all six services.
3.  Return to the role selector and select Administrator.
4.  Verify all tickets are visible.
5.  Call tickets and verify priority/FIFO ordering.
6.  Complete and skip tickets.
7.  Search for an existing and a missing ticket.
8.  Sort report data using each algorithm.
9.  Verify analytics update.
10. Reset the simulation and verify all data is cleared.

------------------------------------------------------------------------

## 16. Development Phases

Implement in the following order. Do not start by building every GUI
screen before the DSA engine is tested.

### Phase 1: Project Setup and Domain Model

-   Create Maven project configured for Java 25.
-   Create enums and domain models.
-   Create shared in-memory application state.
-   Add JUnit 5.

**Deliverable:** Project skeleton and tested domain model.

### Phase 2: DSA Implementations

-   Implement custom FIFO queue.
-   Implement custom binary max-heap.
-   Implement hash table or hash-based ticket index.
-   Implement linear and binary search.
-   Implement insertion, quick, and merge sort.
-   Write unit tests.

**Deliverable:** Independently tested DSA layer.

### Phase 3: Queue and Ticket Services

-   Implement six independent service queues.
-   Implement ticket ID generation.
-   Implement ticket creation and routing.
-   Implement call-next behavior.
-   Implement completion and skip operations.
-   Implement queue position calculation.

**Deliverable:** Working application logic without GUI.

### Phase 4: Swing Shell and Customer Interface

-   Create main window and role selector.
-   Implement navigation using CardLayout.
-   Implement service selection.
-   Implement priority selection and ticket confirmation.
-   Implement ticket status lookup.

**Deliverable:** Working customer experience.

### Phase 5: Administrator Interface

-   Implement admin dashboard.
-   Implement queue monitor.
-   Implement counter operations.
-   Implement ticket search.
-   Implement sorting and reports.
-   Implement analytics and reset.

**Deliverable:** Complete MVP GUI.

### Phase 6: Integration and Final Testing

-   Connect all screens to the shared application state.
-   Test all acceptance criteria.
-   Fix UI refresh and status consistency issues.
-   Add optional demo data.
-   Prepare a short classroom demonstration.

**Deliverable:** Final runnable academic project.

------------------------------------------------------------------------

## 17. Classroom Demonstration Script

Use the following scenario to demonstrate the system:

1.  Start the application and choose Customer.
2.  Generate three Low-priority Finance tickets.
3.  Generate one Medium, one High, and one Emergency Finance ticket.
4.  Generate tickets for Registration and IT Support to demonstrate
    independent service queues.
5.  Return to the role selector and choose Administrator.
6.  Open the Finance queue monitor and show the separate FIFO and
    priority heap.
7.  Call Next repeatedly and demonstrate Emergency → High → Medium → Low
    FIFO ordering.
8.  Complete each serving ticket.
9.  Open Ticket Search and demonstrate hash lookup and linear/binary
    search.
10. Open Reports and demonstrate insertion sort, quick sort, and merge
    sort.
11. Open Analytics and explain the session statistics.
12. Reset the simulation.

This demonstration should make the relationship between the GUI and the
DSA implementation clear.

------------------------------------------------------------------------

## 18. Implementation Guidance for the Coding Agent

When implementing this PRD:

1.  Read the entire PRD before coding.
2.  Use Java 25. Do not downgrade the Java version.
3.  Build the DSA layer before the GUI.
4.  Implement FIFO and binary heap manually. Do not substitute Java
    collection classes for these required structures.
5.  Keep all data in memory. Do not add a database, file persistence,
    backend, or network layer.
6.  Use one shared application state across role changes and screens.
7.  Keep Swing event handlers thin. Call service-layer methods and
    refresh the relevant views.
8.  Use `CardLayout` for navigation within one `JFrame`.
9.  Use clear class and method names and add comments where algorithm
    behavior is not obvious.
10. Write and run unit tests for the DSA and queue-management logic.
11. Keep the UI minimalist, consistent, and easy to demonstrate.
12. Implement the acceptance criteria before adding optional features.
13. Do not invent extra roles or features outside this MVP.
14. Update `README.md` with Java 25 prerequisites, build/run
    instructions, project structure, and a short demonstration guide.

**Definition of Done:** The application builds with Maven on Java 25,
runs as one Swing desktop application, supports Customer and
Administrator roles, manages six independent service queues in memory,
correctly demonstrates FIFO and priority-heap behavior, and exposes the
required searching and sorting algorithms through the GUI.
