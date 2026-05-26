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

In the model developed, the structure consists of four parallel processes: two main processes (P1, P2), that represent the participants in the transaction, and two auxiliary processes responsible for modeling the locks (mutual exclusion mechanisms) of their respective accounts (C1 for P1 and C2 for P2).

When process P1 intends to transfer money to P2, it executes the following sequence of actions:
1. Acquires the lock for its own account (For P1, lockA; for P2, lockB), ensuring exclusive access.
2. Tries to acquire the lock for the destination account (For P1, lockA; for P2, lockB)
3. If the locks are acquired, the transaction is processed and the atomic action send€ is emitted, indicating that the transfer is completed.
4. Finally, the resources are released, in the reverse order of their acquisition (For P1-P2, unlockB-unlockA; for P2-P1, unlockA-unlockB)

This architecture allows the demonstration of a concurrency anomaly. The deadlock occurs due to a circular wait condition: if P1 and P2 initiate simulataneous transfers, P1 will retain lockA while P2 retains lockB. When P1 tries to acquire the lock for B, it needs to wait for its release, and the same happens for P2 with lockA. This leads to the purple state in the LTS.

The respective LTS is represented below:

![LTS](/exercise1/lts1_1.png)


## 1.2. Deadlock fix

To fix the deadlock condition on exercise 1.1., the resource acquisition protocol was modified. To fix the circular wait of the previous model, a strict ordering for lock acquisition was established. Now, both processes must acquire the shared resources in the exact same sequential order (lockA then lockB), regardless of the transfer's direction. 

From here, the circular dependency is eliminated, while maintaining the mutual exclusion. If a process initiates a transfer and acquires the first lock in the hierarcy, any parallel process attempting a transfer will be blocked at the beginning of the cycle, not holding any resources. 

The respective LTS is represented below:

![LTS](/exercise1/lts1_2.png)

To prove that the original model (Deadlock) and the modified model (Fixed) are not weakly bisimilar ($Deadlock \not\approx Fixed$), we apply the Attacker/Defenser Bisimulation Game. Let the initial state of the Exercise 1.1 LTS be $Deadlock_0$ and the initial state of the Exercise 1.2 LTS be $Fixed_0$.

### 1.2.1. The Bisimulation Game

The proof consists on demonstrating that the Attacker can reach a state in the $Deadlock$ system whose observable behaviour cannot be matched by any sequence of weak transitions ($\Rightarrow$) made by the Defenser in the $Fixed$ system.

1. Attack: The attacker, playing on the Deadlock LTS, executes a $\tau$ transition. That can lead it to two different states: $Deadlock_0 \xrightarrow{\tau} Deadlock_1$ or $Deadlock_0 \xrightarrow{\tau} Deadlock_2$.
2. Defense: The defender, playing on the Fixed LTS, must match the $\tau$ move with a $\tau$ sequence. It can move to many states ($Fixed_0 \xRightarrow{\tau} Fixed_1$).
3. Attack: The attacker only has $\tau$'s transitions. Two of them leads to a dead state, so we choose $Deadlock_1 \xrightarrow{\tau} Deadlock_stuck$.
4. Defense: The defender matches with another weak transition $Fixed_1 \xRightarrow{\tau} Fixed_2$. 
5. Attack: The attacker is now in $Deadlock_{stuck}$. There are no available actions.

In state $Fixed_2$, the defender is not deadlocked. It possesses valid transitions (e.g. $Fixed_2 \xrightarrow{send} Fixed_3$). Because the sets of available actions do not match ($\emptyset \neq {send,\tau}$), the Defender fails to mimic the Attacker's state. This proves that the two systems are not weakly bisimilar.


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

The parameterized process $Person[x,cmd]$ acts as a template for any participant. It uses the parameters $x$, that represents the ID of the sender (1,2 or 3), and $cmd$, the channel through which the process receives transfer instructions. The process listens on its command channel ($cmd?y$) to receive the ID of thr target recipient $y$. To execute the transfer, it uses the naive sequential locking strategy already defined: it first requests its own accounts lock ($lock!x) and then requests the recipient's lock ($lock!y$). Once both resources are acquired, it executes the $send$ action, and subsequently releases both locks before returning to its initial state.

The component $Utilizador$ is responsible for driving the system by generating concurrent transfer requests. It uses the choice operator (+) to map a fully connected graph, defining all six possible permutations between the three participants. The recursion forces the participants to compete for locks.

GestorContas[c1,c2,c3] is a centralized state machine, where parameters $c1$, $c2$ and $c3$ act as boolean flags tracking the status of each account's lock, where $0$ indicates an available state and $1$ indicates a locked state. The "manager" listens for incoming lock and unlock requests. It evaluates boolean conditions using $when()$ guards. Upon a successful lock, the process recurses with the corresponding parameter updated to $1$ ($GestorContas[1, c2, c3]$).

The entire parallel architecture is defined by Sistema in exercises 1.1/1,2/1,3 and 1.4. It uses parallelization ($|$), instantiating three unique Person processes, with IDs 1,2, and 3, the Utilizador process and the GestorContas initialized with all accounts unlocked (0,0,0). It uses a restriction set with all actions except $send$.

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

### 2.2.3. Solution: ConcorrentLinkedQueue

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

The script $tickets1.scala$ implements a concurrency model using a single actor (main-office) to manage a shared resource (ticket stock).

### 3.1.1. State Management
To manage states, the actor uses the Akka primitive $context.become(selling(new\_stock))$ to transition its behaviour to a new state with the updated inventory parameter, instead of using variables ($var$).

### 3.1.2. Message Processing
- $ToSell(m)$: When the actor receives this message, he calculates the new stock ($n+m$) and transitions to the new state, adding tickets to the available stock.
- $Buy(m)$: The actor eveluates the condition $n>=m$. If true, the transaction is processed, and the state is updated to $n-m$. If false, the transaction is rejected and an error is logged to the system console. This happens because $n$ can't be less than 0 - it's physically impossible.
- $Bye$: This termination message triggers the $context.stop(self)$ instruction, terminating the actor's lifecycle and freeing memory.

## 3.2. Child Actor Architecture 

The script $tickets2.scala$ uses a more advanced architecture: it transitions to a parent-child hierarchy with automated delegation and a round-robin routing algorithm.

### 3.2.1. Inventory Delegation ($ToSell$)
When the main-office receives a large batch of tickets, it evaluates a threshold condition ($while(stock > 100)$). If the inventory exceeds 100 units, the parent actor instantiates new child actors ($context.actorOf(...)$). It then delegates batches of 50 tickets to each child via the $ToSell(50)$ message, distributing the operational load.

### 3.2.2. Round-Robin Routing ($Buy$):
When a $Buy(m)$ request arrives, the actor attempts to do it himself locally. If the parent does not have sufficient local stock ($n \le m$), it checks its child registry, i.e., if the main-office has any children ($context.children.toList$). If they exist, the parent acts as a balancer. It utilizes a $rotateIdx$ variable, that is updates via $\% filhos.size$ to select a child and uses the $.forward(Buy(m))$ method. The $forward$ primitive passes the message to the child while preserving the original sender's reference, meaning the child would reply directly to the client, bypassing the parent. It uses the round-robin algorithm so the "tasks" are distributed by all children, making it faster.

### 3.2.3. Stock Return ($Return$):
If a child actor receives a $Buy(m)$ request but has emptied its stock, it identifies its inability to process the transaction. To prevent resource leakage, the child sends its reamining stock back to the parent using $context.parent ! Return(n)$. After that, the child invokes $context.stop(self)$ to commit "suicide", self-terminating its process.
The parent actor listens for $Return(m)$ messages from terminating children. Upon receipt, it aggregates the returned fragments back into its own local state ($context.become(selling(n+m)))$, ensuring zero inventory is lost during the destruction of child nodes.

### 3.2.4. Diagrams
The hierarchy diagram represents the supervision tree generated by the Akka ActorSystem: 

![Hierarchy Diagram](/figures/hierarchyDiagram.png)

At the top level, the $TicketSys$ represents the broad ActorSystem environment. It divides into the default internal guardians: the $System$ guardian, that manages logging and processes), and the $User$ guardian, the parent of all user-created actors.

The main-office, instantiated as a $SellerActor$, operates directly under the $User$ guardian. It serves as the primary entry point for all client requests.

The bottom layer demonstrates the dynamic scaling capability of the system. Controlled by the conditional guard $stock > 100$, the main-office instantiates multiple anonymous child actors (internally named $\$a, \$b, ..., \$n$ by Akka), delegating a fixed quota of 50 tickets to each. 

The sequence diagram models the asynchronous message-passing protocol and the lifecycle management of the actors:

![Sequence Diagram](/figures/sequenceDiagram.png)

The purple fragment labeled $loop [stock > 100]$ encapsulates the iterative instantiation and delegation phase. Upon receiving the initial $ToSell(2000)$ message, the main-office continuously creates child actors and sends them $ToSell(50)$ messages until the threshold condition is false.

The orange fragment ($ opt [parent stock \le 20]$) demonstrates the load-balancing logic. An external $Buy(20)$ request is only forwarded to a child node if the parent actor lacks sufficient local inventory to process the transaction.

The blue fragment ($opt [stock child \le 20]$) demonstrates the failure recovery mechanism. If a delegated child cannot fullfilll a forwarded request, it returns its residual stock ($Return(10)$) upstream.

The diagram accurately represents actor termination using "X" markers at the bottom of the lifelines. The child actor terminates itself immediately after returning stock, while the main-office terminates upon receiving the $Bye$ message from the testing routine.