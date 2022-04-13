import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PresentScenario{
    
    public final int numPresents = 1_000;
    public final int numServants = 4;
    public final Integer maxId = Integer.MAX_VALUE;

    AtomicBoolean stopFlag;
    ArrayList<Servant> servants;
    Bag bag;
    PresentChain presentChain;

    public static void main(String[] args){
        PresentScenario ps = new PresentScenario();
    }
    PresentScenario(){
        this.presentChain = new PresentChain();
        this.bag = new Bag(numPresents);
        this.stopFlag = new AtomicBoolean(false);

        servants = new ArrayList<>();
        for (int i = 1; i <= numServants; i++){
            Servant s = new Servant(i);
            servants.add(s);
            System.out.println("Master starting servant " + i);
            //s.setDaemon(true);
            s.start();
        }
    }

    int getRandomID(){
        Random random = new Random();
        return random.nextInt(maxId);
    }

    public class PresentChain{

        AtomicInteger length;
        Present head;

        PresentChain(){
            // add the sentinel presents but don't count them
            this.head = new Present(Integer.MIN_VALUE);
            this.head.next = new Present(Integer.MAX_VALUE);
            this.length = new AtomicInteger(0);
        }

        public void add(Present p) {
            while (true) {  
                Present pred = head;
                Present curr = head.next;
                while (curr.id < p.id) {
                    pred = curr;
                    curr = curr.next;
                }
                pred.lock();
                if (head == null){

                }

                try {
                    curr.lock();
                    try {
                        if (validate(pred, curr)) {
                            if (curr.id != p.id) {
                                p.next = curr;
                                pred.next = p;

                                this.length.incrementAndGet();
                                System.out.println("Added present " + p.id + " new length " + this.length.get());
                                
                                return;
                            }
                        }
                    } finally { // always unlock
                        curr.unlock();
                    }
                } finally { // always unlock
                    pred.unlock();
                }
            }
	    }

        public boolean remove(Present p) {

            while (true) {
                Present pred = this.head;
                Present curr = head.next;
                while (curr.id < p.id) {
                    pred = curr;
                    curr = curr.next;
                }
                pred.lock();
                try {
                    curr.lock();
                    try {
                        if (validate(pred, curr)) {
                            if (curr.id != p.id) { // present
                                return false;
                            } else { // absent
                                curr.marked = true; // logically remove
                                pred.next = curr.next; // physically remove
                                this.length.getAndDecrement();
                                System.out.println("Removed present " + p.id + " new length " + this.length.get());

                                return true;
                            }
                        }
                    } finally { // always unlock curr
                        curr.unlock();
                    }
                } finally { // always unlock pred
                    pred.unlock();
                }
            }
        }

        public boolean contains(int id){
            Present curr = head;
            while(curr.id < id){
                curr = curr.next;
            }
            return curr.id == id && !curr.marked && !curr.tagged;
        }
    
        private boolean validate(Present pred, Present curr) {
            return !pred.marked && !curr.marked && pred.next == curr;

        } 

    
    }
    
    public class Present implements Comparable<Present>{

        public volatile Present next;
        public volatile Present prev;
        // marked presents are deleted logically
        public volatile boolean marked;
        public volatile boolean tagged;
        public Integer id;

        Lock lock;

        Present(Integer id){
            this.id = id;
            this.next = null;
            this.prev = null;
            this.marked = false;
            this.lock = new ReentrantLock();

        }

        void lock(){
            lock.lock();
        }
        void unlock(){
            lock.unlock();
        }

        @Override
        public int compareTo(Present other) {
            return this.id - other.id;
        }

    }

    public class Bag{
        AtomicInteger size;
        Set<Present> set;
        Stack<Present> stack;

        Bag(int size){

            set = Collections.synchronizedSet(new HashSet<Present>());
            stack = new Stack<>();
            for(int i = 0; i < size; i++){
                Integer id = getRandomID();
                Present p = new Present(id);
                
                while(set.contains(p)){
                    p.id = getRandomID();
                }   
                stack.add(p);
                set.add(p);
            }
            this.size = new AtomicInteger(size);
            
        }
        public Present take(){
            Present p = stack.pop();
            set.remove(p);
            size.getAndDecrement(); 
            return p;
        }
    }  

    public class Servant extends Thread{
        int id;
        private boolean switchFlag = true;
        Servant(int id){
            this.id = id;
        }

        @Override
        public void run() {
            Random rand = new Random();
            while(stopFlag.get() == false){
                //System.out.println("Worker " + this.id + " starting new task");
                // random chance for minotaur to ask servant
                if (rand.nextInt(100) == 0){
                    presentChain.contains(getRandomID());
                }
                else{
                    // take from bag, add to chain
                    boolean bagEmpty = bag.size.get() <= 0;
                    if (bagEmpty){
                        if (presentChain.length.get() > 0){
                            presentChain.remove(presentChain.head.next);
                        }
                    }
                    else if (switchFlag){
                        System.out.println("Worker " + this.id + " taking from bag");
                        // take from chain, write note
                        Present p = bag.take();
                        System.out.println("Worker " + this.id + " adding present to chain");

                        presentChain.add(p);
                        switchFlag = !switchFlag;

                    }else if (!switchFlag){
                        System.out.println("Worker " + this.id + " removing from chain");

                        presentChain.remove(presentChain.head.next);

                        switchFlag = !switchFlag;

                    }
                } 

            }
        }
    }
}
