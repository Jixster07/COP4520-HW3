# COP4520-HW3
## Problem 1

file : PresentScenario.java

### How to run
1. clone this repo ```git clone https://github.com/Jixster07/COP4520-HW3.git```
2. enter directory with ```cd Problem1```
3. compile with  ```javac PresentScenario.java```
4. run with ```java PresentScenario```<br>


### Discussion
To solve the problem I used a concurrent lazy list, in which nodes are logically removed before they are physically removed. It uses validation to check that neither the predecessor or current nodes have been logically deleted and that predecessor points to curr. Atomic variables were used on the shared list data, and locks were used to lock predecessor and current nodes.

### Implementation
First the bag was a stack populated by randomly generating unique id's from the minimum to maximum signed integer. Each "present" a node with the attached id. Each servant would alternate pulling from the bag and removing from chain, with a 1% chance of being told to caall contains(). If pulling from bag they would pop from stack and use the add() method. The servant always removed the first node of list to write the note and flagged all threads to halt when list is empty and bag is empty.

### Evaluation / Experimentation
Tests were run on an Intel i7-4790 CPU
| Test      | Run time (seconds) |
| ----------- | ----------- |
| 1      | 0.6288269        |
| 2   | 0.5204392         |
| 3   | 0.5747944         |
| 4   | 0.5485916         |
| 5   | 0.5257867       |
| 6   | 0.5306597      |

Average: 0.55484975 sec

## Problem 2
file : TemperatureScenario.java

### How to run
1. clone this repo, if you haven't ```git clone https://github.com/Jixster07/COP4520-HW3.git```
2. enter directory with ```cd Problem2```
3. compile with  ```javac TemperatureScenario.java```
4. run with ```java TemperatureScenario```


### Discussion / Implementation
The logical solution was to have the master thread dispatch threads, then wait for the set time interval (1 minute) before dispatching again. It does this until 60 iterations (1 hour) have been passed in which it generates the report. 
