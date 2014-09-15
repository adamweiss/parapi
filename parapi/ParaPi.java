/////////////////////////////////////////////////////////////
// Author:      Adam Weiss
// Description: Parallel Pi Computation using Machin's
//              forumula for computation of Pi combined with 
//              Taylor expansion of arctan.
/////////////////////////////////////////////////////////////

package parapi;

import java.lang.Math;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;
import java.util.LinkedList;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.math.BigInteger;
import java.io.*;

public class ParaPi{
  public static BigInteger pi;  

  // Synchronized counter class
  public static class SynchronizedCounter {
    private int c = 0;
    public SynchronizedCounter(int value){
      set(value);
    }
  
    public synchronized int incrementAndGet() {
      return ++c;
    }

    public synchronized int decrementAndGet() {
      return --c;
    }

    public synchronized int value() {
      return c;
    }
  
    public synchronized void set(int value){
      c = value;
    }
  }
  
  // Most basic implementation of Machin's algorithm
  // This class will take a thread ID and the total threads and will skip values
  // based off the total number of threads
  public static class MachinTaylorBigDecimalTask implements Callable<BigDecimal>{
    public BigDecimal pi;
    final int precision;
    // mc is used to set precision
    public MathContext mc; 
    private BigDecimal FOUR;
    private BigDecimal FIVE;
    private BigDecimal SIXTEEN;
    private BigDecimal TWO_THIRTY_NINE;
    public int threadID;
    public int totalThreads;
  
    // initialize class at given precision, threadID and totalThreads
    public MachinTaylorBigDecimalTask(int precision, int threadID, int totalThreads){
      this.mc = new MathContext(precision, RoundingMode.HALF_DOWN);
      this.precision = precision;
      this.threadID = threadID;
      this.totalThreads = totalThreads;
      this.FOUR = new BigDecimal(4, this.mc);
      this.FIVE = new BigDecimal(1,this.mc).divide(new BigDecimal(5, this.mc), this.mc);
      this.SIXTEEN = new BigDecimal(16, mc);
      this.TWO_THIRTY_NINE = new BigDecimal(1,this.mc).divide(new BigDecimal(239, this.mc), this.mc);
    
    }
  
    // Arctangent taylor expansion function
    public BigDecimal arctanTaylor(int start, int skip, BigDecimal x){
      BigDecimal result = new BigDecimal(0);
      BigDecimal delta = null;
      
      // Calculate the values here, skip based off number of threads
      for(; ; start = start + skip){
        delta = x.pow(2 * start + 1, this.mc).divide(new BigDecimal(2 * start + 1), this.mc);
        if(delta.setScale(this.precision-1, RoundingMode.HALF_DOWN).compareTo(BigDecimal.ZERO) == 0)
          break;
      
        if(Math.pow(-1, start) < 0)
          result = result.subtract(delta);
        else
          result = result.add(delta);
      }
    
      return result;
    }

    // Used with executor service
    public BigDecimal call(){
      BigDecimal pi = arctanTaylor(threadID, totalThreads, this.FIVE).multiply(this.SIXTEEN);
      return pi.subtract(arctanTaylor(threadID, totalThreads, this.TWO_THIRTY_NINE).multiply(this.FOUR));
    }  
    
  }

  // This uses a shared blocking counter between threads.  
  // Each arctanTaylor needs its own counter, so two are created here
  public static class MachinTaylorBigDecimalBlockingCounterTask implements Callable<BigDecimal>{
    public BigDecimal pi;
    final int precision;
    
    // counter for the value 5
    static SynchronizedCounter counterFive = new SynchronizedCounter(-1);
    // count for the value 239
    static SynchronizedCounter counterTwoThirtyNine = new SynchronizedCounter(-1);
    // MathContext used for rounding and precision
    public MathContext mc;    
    private BigDecimal FOUR;
    private BigDecimal FIVE;
    private BigDecimal SIXTEEN;
    private BigDecimal TWO_THIRTY_NINE;
    public int threadID;
    public int totalThreads;
  
    // Constructor - takes a given precision
    public MachinTaylorBigDecimalBlockingCounterTask(int precision){
      this.mc = new MathContext(precision, RoundingMode.HALF_DOWN);
      this.precision = precision;
      this.FOUR = new BigDecimal(4, this.mc);
      this.FIVE = new BigDecimal(1,this.mc).divide(new BigDecimal(5, this.mc), this.mc);
      this.SIXTEEN = new BigDecimal(16, mc);
      this.TWO_THIRTY_NINE = new BigDecimal(1,this.mc).divide(new BigDecimal(239, this.mc), this.mc);   
    }

    // Reset counters used when the class is done so there can be more testing
    public static void resetCounters(){
      counterFive.set(-1);
      counterTwoThirtyNine.set(-1);
    }
  
    // Arctangent Taylor expansion function, pass the value of x to calculate
    // along with which counter we will use
    public BigDecimal arctanTaylor(BigDecimal x, SynchronizedCounter counter){
      BigDecimal result = new BigDecimal(0);
      BigDecimal delta = null;
      int n = counter.incrementAndGet();

      // Perform calculations here.  Set n from shared counter
      for(; ; n = counter.incrementAndGet()){
        delta = x.pow(2 * n + 1, this.mc).divide(new BigDecimal(2 * n + 1), this.mc);
        if(delta.setScale(this.precision-1, RoundingMode.HALF_DOWN).compareTo(BigDecimal.ZERO) == 0)
          break;
      
        if(Math.pow(-1, n) < 0)
          result = result.subtract(delta);
        else
          result = result.add(delta);
      }
    
      return result;
    }
    
    // For executor service
    public BigDecimal call(){
      BigDecimal pi = arctanTaylor(this.FIVE, counterFive).multiply(this.SIXTEEN);
      return pi.subtract(arctanTaylor(this.TWO_THIRTY_NINE, counterTwoThirtyNine).multiply(this.FOUR));
    }  
    
  }

  
  // This uses an AtomicInteger shared counter between threads.  
  // Each arctanTaylor needs its own counter, so two are created here
  public static class MachinTaylorBigDecimalAtomicCounterTask implements Callable<BigDecimal>{
    public BigDecimal pi;
    final int precision;
    
    // counter for the value 5
    static AtomicInteger counterFive = new AtomicInteger(-1);
    // count for the value 239
    static AtomicInteger counterTwoThirtyNine = new AtomicInteger(-1);
    // Math context used for rounding and precision
    public MathContext mc;    
    private BigDecimal FOUR;
    private BigDecimal FIVE;
    private BigDecimal SIXTEEN;
    private BigDecimal TWO_THIRTY_NINE;
  
    // Initialize class, set math context and precision 
    public MachinTaylorBigDecimalAtomicCounterTask(int precision){
      this.mc = new MathContext(precision, RoundingMode.HALF_DOWN);
      this.precision = precision;
      this.FOUR = new BigDecimal(4, this.mc);
      this.FIVE = new BigDecimal(1,this.mc).divide(new BigDecimal(5, this.mc), this.mc);
      this.SIXTEEN = new BigDecimal(16, mc);
      this.TWO_THIRTY_NINE = new BigDecimal(1,this.mc).divide(new BigDecimal(239, this.mc), this.mc);
    
    }
    
    // Reset counters for future testing
    public static void resetCounters(){
      counterFive.set(-1);
      counterTwoThirtyNine.set(-1);
    }
  
    // Arctangent Taylor expansion function, pass the value of x to calculate
    // along with which counter we will use
    public BigDecimal arctanTaylor(BigDecimal x, AtomicInteger counter){
      BigDecimal result = new BigDecimal(0);
      BigDecimal delta = null;
      int n = counter.incrementAndGet();

      // calculate results
      for(; ; n = counter.incrementAndGet()){
        delta = x.pow(2 * n + 1, this.mc).divide(new BigDecimal(2 * n + 1), this.mc);
        if(delta.setScale(this.precision-1, RoundingMode.HALF_DOWN).compareTo(BigDecimal.ZERO) == 0)
          break;
      
        if(Math.pow(-1, n) < 0)
          result = result.subtract(delta);
        else
          result = result.add(delta);
      }
    
      return result;
    }
    
    // Used for executor service to do the work and return the results
    public BigDecimal call(){
      BigDecimal pi = arctanTaylor(this.FIVE, counterFive).multiply(this.SIXTEEN);
      return pi.subtract(arctanTaylor(this.TWO_THIRTY_NINE, counterTwoThirtyNine).multiply(this.FOUR));
    }  
    
  }
  
  
  // Will execute a given x to a certain precision.  
  // allows for splitting arguments of arctan into individual threads
  // for a total of two threads.  Useful for seeing how long each term takes 
  // to calculate.
  // This algorithm differs from the ones previous in that it uses the more 
  // efficient "delta" method.
  // Uses BigInteger for fixed point math instead of BigDecimal
  public static class MachinTaylorSplitTask implements Callable<BigInteger>{
    final BigInteger precision;
    final BigInteger x;
    final BigInteger returnMultiplier;

    // Initialize class
    // Return multiplier is how much to multiply the results by based off which
    // arctan term we are calculating.  We need to also set the precision and 
    // which term (x) we are calculating
    public MachinTaylorSplitTask(BigInteger x, BigInteger returnMultiplier, int precision){
      this.precision = BigInteger.valueOf(10).pow(precision + 3);
      this.returnMultiplier = returnMultiplier;
      this.x = x;
    }
  
    // Used for Exeuctor service to do the calculations
    public BigInteger call(){
      BigInteger power = precision.divide(x);
      BigInteger total = precision.divide(x);
      BigInteger x_delta = x.multiply(x);
      BigInteger divisor = BigInteger.valueOf(1);
      BigInteger delta = null;
      while(true){
        power = power.negate().divide(x_delta);
        divisor = divisor.add(TWO);
        delta = power.divide(divisor);
        if(delta.equals(ZERO))
          break;
        total = total.add(delta);
      }
      return total.multiply(returnMultiplier);
    }  
  
  }  
  
  // Optimized single threaded algorithm using delta method.
  // Only single threaded
  public static class MachinTaylorSingleThreaded{
    final BigInteger precision;
    
    // Constructor - set precision
    public MachinTaylorSingleThreaded(int precision){
      this.precision = BigInteger.valueOf(10).pow(precision+3);
    }
  
    // Arctangent Taylor series expansion
    // Precalculates as much as possible ahead of time
    // Takes term x to calculate
    public BigInteger arctanTaylor(BigInteger x){
      BigInteger power = precision.divide(x);
      BigInteger total = precision.divide(x);
      BigInteger x_delta = x.multiply(x);
      BigInteger divisor = BigInteger.valueOf(1);
      BigInteger delta = null;
      while(true){
        power = power.negate().divide(x_delta);
        divisor = divisor.add(TWO);
        delta = power.divide(divisor);
        if(delta.equals(ZERO))
          break;
        total = total.add(delta);
      }
      return total.divide(BigInteger.valueOf(1000));
    }
    
    // Calculate pi
    public long machinPi(){
      long start = System.currentTimeMillis();
      BigInteger pi = arctanTaylor(FIVE).multiply(SIXTEEN);
      pi = pi.subtract(arctanTaylor(TWO_THIRTY_NINE).multiply(FOUR));
      long end = System.currentTimeMillis();
      long time = end-start;
      System.out.println("Total calculation time: " + (time));
      String s_pi = pi.toString();
      ParaPi.verifyFixed("pi1000000.txt", s_pi);
      return time;
    }  
    
  }
  
  // Will divide the work of the thread by a given threadId, 
  // splitting the work efficiently
  // Uses efficient delta method
  public static class MachinTaylorDividedTask implements Callable<BigInteger>{
    final BigInteger precision;
    private int threadId;
    private long totalThreads;
    public BigInteger threadAdder;
    public BigInteger result;  
  
    // Constructor.  Takes precision, a unique thread ID, and the total number
    // of threads that will be running concurrently
    public MachinTaylorDividedTask(int precision, int threadId, long totalThreads){

      // Increase precision for overflow
      this.precision = BigInteger.valueOf(10).pow(precision + 3);
      this.threadId = threadId;
      this.totalThreads = totalThreads;
      this.threadAdder = BigInteger.valueOf(2 * totalThreads);
    }
  
    // Arctangent Taylor series expansion.  Computes as much ahead of time 
    // as possible.  Takes term x to calculate
    public BigInteger arctanTaylor(BigInteger x){
      BigInteger power = null;
      BigInteger total = null;
      
      // initialize values
      if(this.threadId % 2 == 0){
        power = precision.negate().divide(x.pow(2*(this.threadId-1)+1));
        total = precision.negate().divide(x.pow(2*(this.threadId-1)+1));
      }
      else
      {
        power = precision.divide(x.pow(2*(this.threadId-1)+1));
        total = precision.divide(x.pow(2*(this.threadId-1)+1));
      }
        
      BigInteger x_delta = x.pow((int)(2 * totalThreads));
      BigInteger divisor = BigInteger.valueOf(2*(this.threadId-1)+1);
      BigInteger delta = null;
      if(this.threadId != 1){
        delta = power.divide(divisor);
        total = delta;
      }
      
      // Perform the bulk of the work here
      while(true){
        if(this.totalThreads % 2 == 0)
          power = power.divide(x_delta);
        else
          power = power.negate().divide(x_delta);
        divisor = divisor.add(this.threadAdder);
        delta = power.divide(divisor);
        if(delta.equals(ZERO))
          break;
        total = total.add(delta);
      }
      return total;
      
    }
    
    // Method for Executor service.  Will return the work for this thread
    public BigInteger call(){
      this.threadAdder = BigInteger.valueOf(2 * totalThreads);
      BigInteger plus = arctanTaylor(FIVE).multiply(SIXTEEN);
      return plus.subtract(arctanTaylor(TWO_THIRTY_NINE).multiply(FOUR));
    }  
  
  }
  
  // Will divide the work of the thread by a given threadId using delta method
  // will combine results in a synchronized BigInteger instead of returning the 
  // results
  public static class MachinTaylorDividedLockedTask implements Callable<BigInteger>{
    final BigInteger precision;
    private int threadId;
    private long totalThreads;
    public BigInteger threadAdder;
    public static BigInteger result = ZERO;  
  
    // Constructor, takes precision, unique thread ID, and the total number of 
    // threads that will be running concurrently
    public MachinTaylorDividedLockedTask(int precision, int threadId, long totalThreads){
      // Increase precision to compensate for overflow
      this.precision = BigInteger.valueOf(10).pow(precision + 3);
      this.threadId = threadId;
      this.totalThreads = totalThreads;
      this.threadAdder = BigInteger.valueOf(2 * totalThreads);
    }
  
    // arctangent taylor expansion method, takes parameter x to calculate
    public BigInteger arctanTaylor(BigInteger x){
      BigInteger power = null;
      BigInteger total = null;
      if(this.threadId % 2 == 0){
        power = precision.negate().divide(x.pow(2*(this.threadId-1)+1));
        total = precision.negate().divide(x.pow(2*(this.threadId-1)+1));
      }
      else
      {
        power = precision.divide(x.pow(2*(this.threadId-1)+1));
        total = precision.divide(x.pow(2*(this.threadId-1)+1));
      }
        
      BigInteger x_delta = x.pow((int)(2 * totalThreads));
      BigInteger divisor = BigInteger.valueOf(2*(this.threadId-1)+1);
      BigInteger delta = null;
      if(this.threadId != 1){
        delta = power.divide(divisor);
        total = delta;
      }
      
      // loop through taylor series until done
      while(true){
        if(this.totalThreads % 2 == 0)
          power = power.divide(x_delta);
        else
          power = power.negate().divide(x_delta);
        divisor = divisor.add(this.threadAdder);
        delta = power.divide(divisor);
        // stop if delta is equal to zero
        if(delta.equals(ZERO))
          break;
        total = total.add(delta);
      }
      return total;
      
    }
    
    // Will simply block final addition result until this is finished
    private synchronized BigInteger addValue(BigInteger value){
      result = result.add(value);
      return result;
    }
    
    // method reset the result for future runs
    public static synchronized void reset(){
      result = ZERO;
    }
  
    // Method for executor service
    public BigInteger call(){
      this.threadAdder = BigInteger.valueOf(2 * totalThreads);
      BigInteger plus = arctanTaylor(FIVE).multiply(SIXTEEN);
      return this.addValue(plus.subtract(arctanTaylor(TWO_THIRTY_NINE).multiply(FOUR)));
    }  
  
  }
  

  // Precalculated BigInteger values used for various arctan methods
  public static final BigInteger ZERO = BigInteger.valueOf(0);;
  public static final BigInteger ONE = BigInteger.valueOf(1);
  public static final BigInteger TWO = BigInteger.valueOf(2);
  public static final BigInteger SIXTEEN = BigInteger.valueOf(16);
  public static final BigInteger TWO_THIRTY_NINE = BigInteger.valueOf(239);
  public static final BigInteger FOUR = BigInteger.valueOf(4);
  public static final BigInteger FIVE = BigInteger.valueOf(5);
  
  // Nothing to do for constructor
  public ParaPi(){
  
  }
  
  // Verify digits of pi against a file using fixed precision
  // Method assumes the 3 and . are present, starts counting with 
  // 1415926 etc
  // Will return the number of matched characters (numbers)
  public static void verifyFixed(String filename, String verify){
    try{
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
      int c;
      int counter = 2;

      try{
        // skip the first two characters (3.) 
        c = reader.read();
        c = reader.read();
        while((c = reader.read()) != -1 && (counter < verify.length() - 1)){
          if((char) c != verify.charAt(counter-1))
            break;
          counter++;
        }
      } catch(IOException io){
        // There was a problem reading the file
        System.out.println(io.getMessage());
      }
      System.out.println("Accurate to " + (counter) + " digits");
      // where's the file?
    } catch(FileNotFoundException fnf){
      System.out.println(fnf.getMessage());
    }
  }
  
  // Verify digits of pi against a file verbatim
  // Checks every digit, will return number of matches characters (digits)
  public static void verifyDecimal(String filename, String verify){
    try{
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
      int c;
      int counter = 0;
      try{
        while((c = reader.read()) != -1 && (counter < verify.length()-1)){
          if((char) c != verify.charAt(counter))
            break;
          counter++;
        }
      } catch(IOException io){
        // There was a problem reading the file
        System.out.println(io.getMessage());
      }
      System.out.println("Accurate to " + counter + " digits");
    } catch(FileNotFoundException fnf){
      // where's the file?
      System.out.println(fnf.getMessage());
    }
  }


  // Single threaded pi iterative execution
  public static long executeMachinTaylorSingleThreaded(int precision){
    MachinTaylorSingleThreaded m = new MachinTaylorSingleThreaded(precision);
    return m.machinPi();
  }
  
  // Task for basic iterative BigDecimal execution
  public static long executeMachinTaylorBigDecimalTask(int precision, int numThreads){
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    LinkedList<FutureTask<BigDecimal>> threads = new LinkedList<FutureTask<BigDecimal>>();
    long start = System.currentTimeMillis();
    BigDecimal pi = BigDecimal.ZERO;
    try{
      // Create tasks, put them in a list, execute them
      for(int i=0; i < numThreads; i++){
        MachinTaylorBigDecimalTask proc = new ParaPi.MachinTaylorBigDecimalTask(precision, i, numThreads);
        FutureTask<BigDecimal> task = new FutureTask<BigDecimal>(proc);
        threads.push(task);
        executor.submit(task);
      }
    
      executor.shutdown();
      
      // get results from threads when finished and add them together
      for(int i=1; i <= numThreads; i++){
        pi = pi.add(threads.pop().get());
      }
      
      // Set the correct precision
      pi = pi.setScale(precision-1, RoundingMode.HALF_DOWN);
      long end = System.currentTimeMillis();
      long time = end - start;
      // Print run time
      System.out.println("Total calculation time: " + (time));
      // Verify results
      ParaPi.verifyDecimal("pi1000000.txt", pi.toString());
      return time;
      
    }catch(Exception e){
      e.printStackTrace();
    }
    return 0;
    
  }

  // Task for basic iterative BigDecimal execution using shared blocking counter
  public static long executeMachinTaylorBigDecimalBlockingCounterTask(int precision, int numThreads){
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    LinkedList<FutureTask<BigDecimal>> threads = new LinkedList<FutureTask<BigDecimal>>();
    long start = System.currentTimeMillis();
    BigDecimal pi = BigDecimal.ZERO;
    try{
      // Create tasks, put them in a list, execute them
      for(int i=0; i < numThreads; i++){
        MachinTaylorBigDecimalBlockingCounterTask proc = new ParaPi.MachinTaylorBigDecimalBlockingCounterTask(precision);
        FutureTask<BigDecimal> task = new FutureTask<BigDecimal>(proc);
        threads.push(task);
        executor.submit(task);
      }
    
      executor.shutdown();
      
      // get results from threads when finished and add them together
      for(int i=1; i <= numThreads; i++){
        pi = pi.add(threads.pop().get());
      }
      
      // Set the correct precision
      pi = pi.setScale(precision-1, RoundingMode.HALF_DOWN);
      long end = System.currentTimeMillis();
      long time = end - start;
      // Print run time
      System.out.println("Total calculation time: " + (time));
      // Verify results
      ParaPi.verifyDecimal("pi1000000.txt", pi.toString());
      return time;
      
    }catch(Exception e){
      e.printStackTrace();
      return 0;
      // Be sure to reset counters when finished
    } finally {MachinTaylorBigDecimalBlockingCounterTask.resetCounters();}
    
  }

  
  // Task for basic iterative BigDecimal execution using shared counter
  public static long executeMachinTaylorBigDecimalAtomicCounterTask(int precision, int numThreads){
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    LinkedList<FutureTask<BigDecimal>> threads = new LinkedList<FutureTask<BigDecimal>>();
    long start = System.currentTimeMillis();
    BigDecimal pi = BigDecimal.ZERO;
    try{
      // Create tasks, put them in a list, execute them
      for(int i=0; i < numThreads; i++){
        MachinTaylorBigDecimalAtomicCounterTask proc = new ParaPi.MachinTaylorBigDecimalAtomicCounterTask(precision);
        FutureTask<BigDecimal> task = new FutureTask<BigDecimal>(proc);
        threads.push(task);
        executor.submit(task);
      }
    
      executor.shutdown();
      
      // get results from threads when finished and add them together
      for(int i=1; i <= numThreads; i++){
        pi = pi.add(threads.pop().get());
      }
      
      // Set the correct precision
      pi = pi.setScale(precision-1, RoundingMode.HALF_DOWN);
      long end = System.currentTimeMillis();
      long time = end - start;
      // Print run time
      System.out.println("Total calculation time: " + (time));
      // Be sure to reset counters when finished
      ParaPi.verifyDecimal("pi1000000.txt", pi.toString());
      return time;
      
    }catch(Exception e){
      e.printStackTrace();
      return 0;
      // Be sure to reset counters when finished
    } finally {MachinTaylorBigDecimalAtomicCounterTask.resetCounters();}
    
  }
  
  // Task for delta BigInteger "split thread" execution - only 2 threads
  public static long executeMachinTaylorSplitTask(int precision){
    ExecutorService executor = Executors.newFixedThreadPool(2);
    
    
    long start = System.currentTimeMillis();

    // One thread calculates the first term, the second thread calculates
    // the second
    MachinTaylorSplitTask a = new ParaPi.MachinTaylorSplitTask(FIVE, SIXTEEN, precision);
    MachinTaylorSplitTask b = new ParaPi.MachinTaylorSplitTask(TWO_THIRTY_NINE, FOUR, precision);
    
    try{
      FutureTask<BigInteger> aTask = new FutureTask<BigInteger>(a);
      FutureTask<BigInteger> bTask = new FutureTask<BigInteger>(b);
      executor.submit(aTask);
      executor.submit(bTask);
      
      executor.shutdown();
      BigInteger plus = aTask.get();
      BigInteger minus = bTask.get();
      // subtract the results of second task from the first, divide by
      // 1000 to set correct precision
      pi = plus.subtract(minus).divide(BigInteger.valueOf(1000));
      long time = System.currentTimeMillis() - start;
      
      // Print run time
      System.out.println("Total calculation time: " + (time));
      String s_pi = pi.toString();
      //System.out.println(s_pi);
      ParaPi.verifyFixed("pi1000000.txt", s_pi);
      return time;
      
    }catch(Exception e){
      e.printStackTrace();
      return 0;
    }
  }

  // Task for delta BigInteger equally divided task.  Takes a given precision
  // and the number of threads to execute
  public static long executeMachinTaylorDividedTask(int precision, int numThreads){
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    LinkedList<FutureTask<BigInteger>> threads = new LinkedList<FutureTask<BigInteger>>();
    long start = System.currentTimeMillis();
    ParaPi m = new ParaPi();
    m.pi = ZERO;
    try{
      // Create tasks and add to linked list
      for(int i=1; i <= numThreads; i++){
        MachinTaylorDividedTask proc = new ParaPi.MachinTaylorDividedTask(precision, i, numThreads);
        FutureTask<BigInteger> task = new FutureTask<BigInteger>(proc);
        threads.push(task);
        executor.submit(task);
      }
    
      executor.shutdown();
      
      for(int i=1; i <= numThreads; i++){
        m.pi = m.pi.add(threads.pop().get());
      }
      // take care of overflow
      m.pi = m.pi.divide(BigInteger.valueOf(1000));
      // get total execution time
      long time = System.currentTimeMillis() - start;
      System.out.println("Total calculation time: " + (time));
      String pi = m.pi.toString();
      // verify results
      ParaPi.verifyFixed("pi1000000.txt", pi);
      return time;
      
    }catch(Exception e){
      e.printStackTrace();
      return 0;
    }
  }

  // Task for delta BigInteger equally divided task using locking shared value.
  // Takes a given precision and the number of threads to execute
  public static long executeMachinTaylorDividedLockedTask(int precision, int numThreads){
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    LinkedList<FutureTask<BigInteger>> threads = new LinkedList<FutureTask<BigInteger>>();
    long start = System.currentTimeMillis();
    ParaPi m = new ParaPi();
        
    try{
      // Initialize tasks and add to linked list
      for(int i=1; i <= numThreads; i++){
        MachinTaylorDividedLockedTask proc = new ParaPi.MachinTaylorDividedLockedTask(precision, i, numThreads);
        FutureTask<BigInteger> task = new FutureTask<BigInteger>(proc);
        threads.push(task);
        executor.submit(task);
      }
    
      executor.shutdown();
      
      // Spin until threads are finished 
      for(int i=1; i <= numThreads; i++){
        threads.pop().get();
      }
      
      // take care of overflow
      BigInteger pi = ParaPi.MachinTaylorDividedLockedTask.result.divide(BigInteger.valueOf(1000));
      // get total execution time
      long time = System.currentTimeMillis() - start;
      System.out.println("Total calculation time: " + (time));
      String pi_s = pi.toString();
      // Verify results
      ParaPi.verifyFixed("pi1000000.txt", pi_s);
      ParaPi.MachinTaylorDividedLockedTask.reset();
      return time;
      
    }catch(Exception e){
      e.printStackTrace();
      return 0;
    }
  }
    
}
