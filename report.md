# Report
Group 6: Rita Moreira (202303885) and Orlando Soares (202303606)

# 0. AI Support
In the development of this project, LLMs were strictly used as an auxiliary aid and validation instrument, rather than as code generators. 

Specifically, AI support was engaged for the following purposes:
- To verify the correctness of manually developed answers, particularly in the analytical proof for the Bisimulation Game
- To assist in translating code into formal explanations
- To act as a tutor for clarifying advanced concurrency concepts, specifically regarding Actors
- To clarify best practices for the hierarchy and sequence diagrams

# 1. Exercise 1: Sending money using locks

## 1.1. Simple CCS Model (with deadlock)

The system is modeled as a composition of four concurrent processes `TransferDeadlock = (P1|P2|C1|C2) \ L`, where $L$ is the restriction set `{lockA, lockB, unlockA, unlockB}`, that hides the internal locking actions. The processes $P1$ and $P2$ represent the two participants in the transaction, while $C1$ and $C2$ are auxiliary processes that model the locks for their respective accounts, i.e., control access to the critical section.

In this model, mutual exclusion is enforced via syncronization over complementary ($?$ and $!$) actions. When $P1$ executes `lockA!`, it must synchronize with $C1$'s complementary `lockA?` action, which represents the acquisition of the lock for account A. The same applies for $P2$ and $C2$ with lockB.

When process P1 intends to transfer money to P2, it executes the following sequence of actions:
1. Acquires the lock for its own account (For P1, lockA; for P2, lockB), ensuring exclusive access.
2. Tries to acquire the lock for the destination account (For P1, lockB; for P2, lockA)
3. If the locks are acquired, the transaction is processed and the atomic action `send€` is emitted, indicating that the transfer is completed.
4. Finally, the resources are released, in the reverse order of their acquisition (For P1-P2, `unlockB`-`unlockA`; for P2-P1, `unlockA`-`unlockB`)

This naive implementation leads to a circular wait condition, resulting in a deadlock:
1. P1 acquires lockA, by executing `lockA!` and synchronizing with C1's `lockA?`
2.  The scheduler switches to P2, which acquires lockB by executing `lockB!` and synchronizing with C2's `lockB?`
3. P1 attempts to acquire `lockB`, but is blocked because P2 holds it
4. P2 attempts to acquire `lockA`, but is blocked because P1 holds it


![LTS](/exercise1/lts1_1.png)

*Figure 1: LTS of the model with deadlock. The deadlock is represented by the purple node, where both processes are stuck waiting for each other to release the locks and there are no valid transitions.*


## 1.2. Deadlock fix

To fix the deadlock condition on exercise 1.1., the resource acquisition protocol was modified. To fix the circular wait of the previous model, a strict ordering for lock acquisition was established. Now, both processes must acquire the shared resources in the exact same sequential order (`lockA` then `lockB`), regardless of the transfer's direction. Consequently, if $P1$ acquires $C1$, $P2$ must wait until $P1$ releases $C1$ before it can acquire it, even if $P2$ is trying to transfer to $P1$.

From here, the circular dependency is eliminated, while maintaining the mutual exclusion. If a process initiates a transfer and acquires the first lock in the hierarcy, any parallel process attempting a transfer will be blocked at the beginning of the cycle, not holding any resources. 

![LTS](/exercise1/lts1_2.png)

*Figure 2: LTS of the model with the deadlock fix. The circular wait is eliminated, as both processes must acquire locks in the same order. There are no dead states in this model, as any process that initiates a transfer will be blocked at the beginning of the cycle, not holding any resources.*

To prove that the original model (Deadlock) and the modified model (Fixed) are not weakly bisimilar ($Deadlock \not\approx Fixed$), we apply the Attacker/Defenser Bisimulation Game. Let the initial state of the Exercise 1.1 LTS be $Deadlock_0$ and the initial state of the Exercise 1.2 LTS be $Fixed_0$.

### 1.2.1. The Bisimulation Game

The proof consists on demonstrating that the Attacker can reach a state in the $Deadlock$ system whose observable behaviour cannot be matched by any sequence of weak transitions ($\Rightarrow$) made by the Defenser in the $Fixed$ system.

1. Attack: The attacker, playing on the Deadlock LTS, executes an internal transition $\tau$. That can lead it to two different states: $Deadlock_0 \xrightarrow{\tau} Deadlock_1$ or $Deadlock_0 \xrightarrow{\tau} Deadlock_2$.
2. Defense: The defender, playing on the Fixed LTS, must match the $\tau$ transition ($\rightarrow$) with a $\tau$ sequence ($\Rightarrow$). It can move to many states ($Fixed_0 \xRightarrow{\tau} Fixed_1$).
3. Attack: The attacker only has $\tau$'s transitions. Two of them leads to a dead state, so we choose $Deadlock_1 \xrightarrow{\tau} Deadlock_stuck$, i.e., we choose the transition that leads to the stuck state.
4. Defense: The defender matches with another weak transition $Fixed_1 \xRightarrow{\tau} Fixed_2$. 
5. Attack: The attacker is now in $Deadlock_{stuck}$. There are no available actions.

In state $Fixed_2$, the defender is not deadlocked. It possesses valid transitions (e.g. $Fixed_2 \xrightarrow{send} Fixed_3$). Because the sets of available actions do not match ($\emptyset \neq {send,\tau}$), the Defender fails to mimic the Attacker's state. This proves that the two systems are not weakly bisimilar.

Even if there are transitions that don't lead to a deadlock in the original model, the Attacker can always choose the path that leads to the deadlock, which cannot be mimicked by the Defender in the modified model. Therefore, $Deadlock \not\approx Fixed$.


## 1.3. Original CCS Model (with deadlock)

To define the model for the three-participant scenario represented in Figure 1, the system structure was expanded using the mutual exclusive mechanism developed in Exercise 1.1.

The system consists of three different participants (Eren, Hinata, and Zenitsu) and three corresponding auxiliary processes (CEren, CHinata, CZenitsu) that manage the access to each participant's account. The communication is strictly directional, matching the provided graph:
- Zenitsu only initiates transfers to Hinata
- Hinata only initiates transfers to Eren
- Eren can initiate transfers to both Hinata and Zenitsu

To model Eren's ability to choose between two different destination accounts, the choice operator (+) is used, to allow the Eren process to branch into two distinct execution paths.

Because the system must allow for deadlocks, each transfer sequence strictly follows the naive protocol (circular wait for resources).

## 1.4. Value passing CCS

To define the generalized model for the three-participant scenario, where three participants can transfer money to one another without restrictions, the system uses value-passing CCS.

The parameterized process `Person[x,cmd]` acts as a template for any participant. It uses the parameters $x$, that represents the ID of the sender (1,2 or 3), and $cmd$, the channel through which the process receives transfer instructions. The process listens on its command channel ($cmd?y$) to receive the ID of thr target recipient $y$. To execute the transfer, it uses the naive sequential locking strategy already defined: it first requests its own accounts lock (`lock!x`) and then requests the recipient's lock (`lock!y`). Once both resources are acquired, it executes the $send$ action, and subsequently releases both locks before returning to its initial state.

The component `Utilizador` is responsible for driving the system by generating concurrent transfer requests. It uses the choice operator (+) to map a fully connected graph, defining all six possible permutations between the three participants. The recursion forces the participants to compete for locks.

`GestorContas[c1,c2,c3]` is a centralized state machine, where parameters $c1$, $c2$ and $c3$ act as boolean flags tracking the status of each account's lock, where $0$ indicates an available state and $1$ indicates a locked state. The "manager" listens for incoming lock and unlock requests. It evaluates boolean conditions using `when()` guards. Upon a successful lock, the process recurses with the corresponding parameter updated to $1$ (`GestorContas[1, c2, c3]`).

The entire parallel architecture is defined by Sistema in exercises 1.1/1,2/1,3 and 1.4. It uses parallelization ($|$), instantiating three unique Person processes, with IDs 1,2, and 3, the Utilizador process and the `GestorContas` initialized with all accounts unlocked (0,0,0). It uses a restriction set with all actions except $send$.

Because the Person template utilizes the naive locking sequence, the circular wait deadlock is guaranteed.


# 2. Simulate dependent processes in a server

## 2.1. Counter Correction and Analysis

### 2.1.1. The Anomaly: Lost Updates

The original implementation uses a non-atomic read-modify-write sequence for request ID generation:

```
state.counter += 1
```

This operation is not atomic and can fail under concurrent load. When multiple threads execute this instruction simultaneously, a race condition occurs:

1. Thread A reads counter = 41
2. Thread B reads counter = 41
3. Thread A writes 42
4. Thread B writes 42 (lost update)

Result: One increment is lost; the counter advances by only 1 instead of 2.

### 2.1.2. Failure Detection

The anomaly is detected using `StressTest.scala`, which simulates a burst of concurrent `/run-simulation` requests. Each request increments the counter, and with sufficient concurrent load, the lost update phenomenon becomes observable.

### 2.1.3. The Solution: AtomicInteger

The fix uses Java's `java.util.concurrent.atomic.AtomicInteger`:

```scala
import java.util.concurrent.atomic.AtomicInteger

class ServerState {
  private val counter = new AtomicInteger(0)

  def nextRequestId(): Int = counter.incrementAndGet()
  def resetCounter(): Unit = counter.set(0)
  def currentCounter: Int = counter.get()
}
```

The `incrementAndGet()` method is guaranteed to be atomic: it performs the read-modify-write sequence as a single indivisible operation. Under concurrency, each thread receives a unique incremented value, and no updates are lost.

## 2.2. Lock-Free Data Structure: Log Queue Protection

### 2.2.1. The Concurrency Problem

The original log queue implementation is not thread-safe. When multiple threads attempt to append log entries concurrently, several failure modes can occur:

- **Lost log entries**: Two threads write at the same index simultaneously, overwriting each other's data
- **Corrupted internal state**: Size counters become inconsistent
- **Inconsistent log sizes**: The queue reports different sizes at different times
- **Exceptions during iteration**: Concurrent modifications throw `ConcurrentModificationException`
- **Duplicated or partially written entries**: Incomplete writes visible to other threads

### 2.2.2. Concrete Failure Scenario

With `StressTest.scala` simulating 50 concurrent requests to `/run-simulation`:

1. Thread X and Thread Y attempt to append simultaneously
2. Initial queue size = 10
3. Thread X reads size = 10
4. Thread Y reads size = 10
5. Thread X writes at index 10
6. Thread Y also writes at index 10
7. Result: One message is overwritten and disappears; the final queue contains fewer than 50 entries

### 2.2.3. Solution: ConcurrentLinkedQueue

ConcurrentLinkedQueue[String]()

`java.util.concurrent.ConcurrentLinkedQueue` provides lock-free thread-safe appends.

## 2.3. Dependency Mechanism

The dependency system allows specifying that instruction execution must respect ordering constraints. Instructions can declare dependencies using the `after` clause.

### 2.3.1. Example 1: Sequential Dependency with Independent Instruction

```
print "A" @3; print "B"; print "C" after 1
```

- Instruction 1 (A) has a 3-second delay
- Instruction 2 (B) is independent with no delay
- Instruction 3 (C) depends on instruction 1 (A) completing

Expected behavior:

- B can print immediately (concurrency proof: B runs while A is delayed)
- C must always print after A finishes
- Valid output: `B A C` (B runs during A's delay)

### 2.3.2. Example 2: Multi-Parent Synchronization

```
print "FetchData" @2; print "Auth" @1; print "Process" after 1,2; print "LogAuth" after 2
```

- Instruction 3 (Process) depends on both instructions 1 and 2 completing
- Instruction 4 (LogAuth) depends only on instruction 2

Expected behavior:

- LogAuth may appear before or after FetchData (no dependency between them)
- Process never appears before both FetchData and Auth complete
- Valid outputs include: `Auth, LogAuth, FetchData, Process` or `Auth, FetchData, LogAuth, Process`

### 2.3.3. Example 3: Sequential Chain with Independent Task and Final Barrier

```
print "Compile" @2; print "Test" @1 after 1; print "Package" after 2; 
print "Notify"; print "Deploy" after 3,4
```

- A sequential chain: Compile (1) → Test (2) → Package (3)
- Notify (4) is independent and can execute anytime
- Deploy (5) is a final barrier that depends on both Package (3) and Notify (4)

Expected behavior:

- Notify can appear anywhere relative to Compile/Test/Package
- Deploy is always last among {Package, Notify, Deploy}
- Demonstrates mixed behavior: a dependency chain running concurrently with an independent task, concluding with a join barrier

### 2.3.4. Cycle Detection

The implementation includes a dependency cycle detector. Invalid input such as:

```
print "first" after 2; print "second" after 1;
```

Where instruction 1 depends on 2 and instruction 2 depends on 1, triggers an error:

```
Invalid dependency graph: cycle detected in after-clause
```

This ensures that the dependency graph remains a DAG (Directed Acyclic Graph).

## 2.4. @volatile Variable Addition

### 2.4.1. The Visibility Problem

A common concurrency issue is the lack of visibility guarantee when one thread modifies a variable and other threads read it. Without proper synchronization, threads may cache variable values locally and fail to observe updates from other threads.

Example: A shared `paused` flag controlling whether instruction execution is suspended. Multiple worker threads repeatedly check this flag before continuing execution. Without `@volatile`, threads may cache the variable locally and continue executing despite updates from the main thread setting `paused = true`.

### 2.4.2. The @volatile Solution

By declaring the variable as `@volatile`:

```scala
@volatile var paused: Boolean = false
```

All writes to the variable become immediately visible to all other threads. This provides the happens-before visibility guarantee necessary for this pattern.

### 2.4.3. Practical Demonstration

`VolatileTest.scala` implements the following test scenario:

1. Call `/pause` to set the pause flag
2. Submit `/run-simulation` with a dependency-rich input program
3. Wait 1.5 seconds for worker threads to enter the paused loop
4. Call `/resume` to clear the pause flag (write `paused = false`)
5. Fetch `/status` and verify that execution resumed

This scenario is a pure visibility problem: one thread writes `paused = false`, other threads are busy-reading `paused`. Without `@volatile`, worker threads would not observe the write and would remain blocked indefinitely. With `@volatile`, the happens-before guarantee ensures that all threads immediately see the updated value and resume execution.

# 3. Selling tickets with actors

## 3.1. General Actor Architecture

The script `tickets1.scala` implements a concurrency model using a single actor (main-office) to manage a shared resource (ticket stock).

### 3.1.1. State Management
To manage states, the actor uses the Akka primitive `context.become(selling(new_stock))` to transition its behaviour to a new state with the updated inventory parameter, instead of using variables (`var`).

### 3.1.2. Message Processing
- `ToSell(m)`: When the actor receives this message, he calculates the new stock (`n+m`) and transitions to the new state, adding tickets to the available stock.
- `Buy(m)`: The actor evaluates the condition `n>=m`. If true, the transaction is processed, and the state is updated to `n-m`. If false, the transaction is rejected and an error is logged to the system console. This happens because `n` can't be less than 0 - it's physically impossible.
- `Bye`: This termination class triggers the `context.stop(self)` instruction, terminating the actor's lifecycle and freeing memory.

## 3.2. Child Actor Architecture 

The script `tickets2.scala` uses a more advanced architecture: it transitions to a parent-child hierarchy with automated delegation and uses a rejection system for when the child actor does not have enough stock to process a request. It also uses two classes, one for the Main Seller (Parent) and another for the Child Sellers, to better separate the logic of each type of actor.

### 3.2.1. Inventory Delegation (`excessStock` function)
When the main-office receives a large batch of tickets, it evaluates a threshold condition (`while(stock > 100)`). If the inventory exceeds 100 units, the parent actor instantiates new child actors (`context.actorOf(...)`). It then delegates batches of 50 tickets to each child via the `ToSell(50)` message, distributing the operational load. It also updates the list of active children to keep track of the current child actors. This dynamic scaling mechanism allows the system to handle large inventories efficiently, while maintaining a manageable workload for each actor. This function is only defined in the parent class, as the children are only responsible for processing delegated requests, not for managing inventory or creating new actors.

### 3.2.2. Selling function
This function manages the selling system `ToSell(m)`, `Buy(m)`, `Return(m)` and `Bye`. 

The `ToSell(m)` case creates a tuple by calling the `excessStock(stock, children)` function, which returns the new stock and the list of children. The parent actor then updates its state with the new stock and children list (`context.become(selling(newStock, newChildren))`). For the child actor class, the `ToSell(m)` case simply updates the local stock (`context.become(selling(stock+m))`), as they don't have the authority to create new actors or delegate inventory.

The `Buy(m)` case is defined in both classes, but with different logic. The parent actor first checks if it can process the request locally. If not, it checks for available children and forwards the request to one of them. The child actor, on the other hand, only checks its local stock and processes the request if it has enough inventory. If it doesn't have enough stock, it sends a `Return(n)` message back to the parent and terminates itself.

The `Return(n)` case, in the parent actor, calls `excessStock` to update its state with the new stock and children list, as it may need to create new child actors if the returned stock exceeds the threshold. In the child actor, this case is not defined.

The `Bye` case is defined in both classes, and it simply terminates the actor's lifecycle using `context.stop(self)`.

The auxiliary function `rejection` is defined in the child actor class. While in the rejection state, the child continues to forward incoming `Buy(m)`requests back to the parent. It only terminates its lifecycle when it receives a formal acknowledgement message (AcknowledgeDeath) from the parent, confirming that the return process is complete and the child can safely shut down.

Both programs use an interactive input testing, where the user can input the initial stock, tickets to buy and number of requests. The system will process the requests and print the final stock after all transactions are completed. The `Bye` message is sent at the end of the testing routine to terminate the main-office actor.


### 3.2.3. Diagrams
The hierarchy diagram represents the supervision tree generated by the Akka ActorSystem: 

![Hierarchy Diagram](/figures/hierarchyDiagram.png)

At the top level, the `TicketSys` represents the broad ActorSystem environment. It divides into the default internal guardians: the `System` guardian, that manages logging and processes), and the `User` guardian, the parent of all user-created actors.

The main-office, instantiated as a `SellerActor`, operates directly under the `User` guardian. It serves as the primary entry point for all client requests.

The bottom layer demonstrates the dynamic scaling capability of the system. Controlled by the conditional guard `stock > 100`, the main-office instantiates multiple anonymous child actors (internally named $\$a, \$b, ..., \$n$ by Akka), delegating a fixed quota of 50 tickets to each. 

The sequence diagram models the asynchronous message-passing protocol and the lifecycle management of the actors:

![Sequence Diagram](/figures/sequenceDiagram.png)

The purple fragment labeled `loop [stock > 100]$ encapsulates the iterative instantiation and delegation phase. Upon receiving the initial `ToSell(n)` message, the main-office continuously creates child actors and sends them `ToSell(50)` messages until the threshold condition is false.

The orange fragment (`opt [parent stock \le m]`) demonstrates the load-balancing logic. An external `Buy(m)` request is only forwarded to a child node if the parent actor lacks sufficient local inventory to process the transaction.

The blue fragment (`opt [stock child \le m]`) demonstrates the failure recovery mechanism. If a delegated child cannot fulfill a forwarded request, it returns its residual stock (`Return(10)`) upstream.

The diagram accurately represents actor termination using "X" markers at the bottom of the lifelines. The child actor terminates itself immediately after returning stock, while the main-office terminates upon receiving the `Bye` message from the testing routine.