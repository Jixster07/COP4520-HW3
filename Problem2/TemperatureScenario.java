import java.util.*;

public class TemperatureScenario {

    // Config
    final int speedUpFactor = 60;                       // factor by which to speed up the simulation
    boolean enableSimPrinting;                 // whether to actively print simulation info
    boolean noWait;
    int numReports = 2;                           // number of reports to generate before terminating

    final int numThreads = 8;
    final int readingInterval = 60 / speedUpFactor;     // time (in seconds) between each sensor reading
    final int reportInterval = 60*60 / speedUpFactor;   // time (in seconds) between each report
    ArrayList<Sensor> sensors;
    //Reading table has all readings for hour for all threads
    ArrayList<ArrayList<Integer>> readingTable;


    public static void main(String[] args){
        new TemperatureScenario();
    }

    TemperatureScenario(){

        System.out.println("Wait for timer? If no, the time delay will not be simulated. (Y/N) ");
        Scanner scan = new Scanner(System.in);
        String input = scan.next().toLowerCase();
        if (input.equals("y")){
            noWait = false;
        } else if (input.equals("n")){
            noWait = true;
        } else{
            System.out.println("Invalid input, defaulting to Y");
            noWait = true;
        }
        System.out.println("Enable simulation printing? If yes, readings will be actively printed. (Y/N)");
        input = scan.next().toLowerCase();
        if (input.equals("y")){
            enableSimPrinting = true;
        } else if (input.equals("n")){
            enableSimPrinting = false;
        } else{
            System.out.println("Invalid input, defaulting to Y");
            enableSimPrinting = false;
        }

        System.out.println("How many reports to generate? (Minimum 1)");
        numReports = scan.nextInt();


        System.out.println("Starting Temperature Simulation");
        System.out.println("Simulation sped up by a factor of " + speedUpFactor);
        
        readingTable = new ArrayList<ArrayList<Integer>>(numThreads);
        sensors = new ArrayList<>();
        for (int i = 0; i < numThreads; i++){
            sensors.add(new Sensor(i));
            readingTable.add(new ArrayList<Integer>());
        }



        for (int i = 0; i < numReports; i++){

            while(true){
                for (Sensor s : sensors) {

                    // dispatch threads
                    if(!s.hasRead)
                        s.start();
                    else s.run();
                    try {
                        s.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (enableSimPrinting){
                    System.out.print("Got Readings : ");
                    for (ArrayList<Integer> list : readingTable){
                        System.out.print("\t"+list.get((list.size()-1)));
                    }
                    System.out.println();
                }

                if (readingTable.get(0).size()>=(reportInterval/readingInterval)){
                    generateReport();
                    clearTable();
                    break;
                }
                
                if (!noWait){
                    try {
                        Thread.sleep(readingInterval * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
            

        }
    }

    private void clearTable(){
        for(ArrayList<Integer> list: readingTable){
            list.clear();
        }
    }

    public void generateReport(){

        PriorityQueue<Integer> lowPq = new PriorityQueue<Integer>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.compare(o2, o1);
            }
        });
        PriorityQueue<Integer> highPq = new PriorityQueue<>();

        ArrayList<Integer> minReadings = new ArrayList<>();
        ArrayList<Integer> maxReadings = new ArrayList<>();

        for(int i = 0; i < readingTable.get(0).size(); i++){
            Integer min = Integer.MAX_VALUE;
            Integer max = Integer.MIN_VALUE;
            for (int j = 0; j < readingTable.size(); j++){
                Integer reading = readingTable.get(j).get(i);
                min = Math.min(min, reading);
                max = Math.max(max, reading);

                lowPq.add(reading);
                if (lowPq.size() > 5)
                    lowPq.poll();

                highPq.add(reading);
                if (highPq.size() > 5)
                        highPq.poll();
            }
            minReadings.add(min);
            maxReadings.add(max); 
        }
        Integer maxDiff = Integer.MIN_VALUE;
        int a = 0;
        int b = 0;
        for (int i = 0; i < minReadings.size()-10; i++){
            Integer min =  Integer.MAX_VALUE;
            Integer max = Integer.MIN_VALUE;
            Integer diff;
            for (int j = i; j < 10; j++){
                min = Math.min(min, minReadings.get(j));
                max = Math.max(max, maxReadings.get(j));
            }
            diff = max - min;
            if (diff >= maxDiff){
                a = i+1;
                b = i+10+1;

                maxDiff = diff;
            }
        }


        StringBuilder sb = new StringBuilder();
        sb.append("Hourly Temperature Reading\n");
        sb.append("=======================================\n");
        sb.append("Top 5 lowest temperatures:\n");
        for (int i = 0; i < 5; i++){
            sb.append(Integer.toString(lowPq.poll()) + "\n");

        }
        sb.append("Top 5 highest temperatures:\n");
        for (int i = 0; i < 5; i++){
            sb.append(Integer.toString(highPq.poll()) + "\n");

        }
        sb.append("10-minute interval with largest temperature difference:\n");
        sb.append("Minute " + a + " to minute " + b + " with difference of " + maxDiff);
        System.out.println(sb.toString());
    }


    public class Sensor extends Thread{

        final int maxReading = 70;
        final int minReading = -100;

        public boolean hasRead;
        int id;

        Sensor(int id){
            this.id = id;
            this.hasRead = false; 
        }

        @Override
        public void run() {
            hasRead = true;
            readingTable.get(id).add(Integer.valueOf(getReading()));
            
        }

        public int getReading(){
            Random random = new Random();
            return random.nextInt(maxReading - minReading + 1) + minReading;
        }

    }
}
