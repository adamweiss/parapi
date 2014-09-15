/////////////////////////////////////////////////////////////
// Author:      Adam Weiss
// Description: Parallel Pi Computation using Machin's
//              forumula for computation of Pi combined with 
//              Taylor expansion of arctan.
/////////////////////////////////////////////////////////////

import parapi.ParaPi;

public class ParaPiBenchmark
{
  // Run benchmark of all methods
  public static void main(String[] args){
    long sumTime = 0;
    // Get number of cores on system
    int cores = Runtime.getRuntime().availableProcessors();
    // set total number of runs per algorithm
    double runs = 50.0;
    
    // Loop through different levels of precision
    for(int precision = 1000; precision <= 10000; precision *= 10){
      System.out.println("--------------------------------------------------\n");      
      System.out.println("******Setting precision to approximately " + precision + " digits.*****\n");

      // Iterate through number of threads for each of the following
      for(int threads = 1; threads <= cores; threads++){
        System.out.println("\n**Testing inefficient BigDecimal algo - " + threads + " thread(s)\n");        
        
        for(int run = 0; run < runs; run++){
          sumTime +=  ParaPi.executeMachinTaylorBigDecimalTask(precision, threads);
        }
        System.out.println("\nAverage time: " + sumTime / runs);
        
        sumTime = 0;
        
        System.out.println("\n--------------------------------------------------\n");      
        System.out.println("**Testing inefficient BigDecimal Blocking Counter algo - " + threads + " thread(s)\n");        
        
        for(int run = 0; run < runs; run++){
          sumTime += ParaPi.executeMachinTaylorBigDecimalBlockingCounterTask(precision, threads);
        }
        System.out.println("\nAverage time: " + sumTime / runs);
        
        sumTime = 0;
        
        System.out.println("\n--------------------------------------------------\n");      
        System.out.println("**Testing inefficient BigDecimal Atomic Counter algo - " + threads + " thread(s)\n");        
        
        for(int run = 0; run < runs; run++){
          sumTime += ParaPi.executeMachinTaylorBigDecimalAtomicCounterTask(precision, threads);
        }
        System.out.println("\nAverage time: " + sumTime / runs);
        
        sumTime = 0;

        System.out.println("\n--------------------------------------------------\n");      
        System.out.println("**Testing efficient multithreaded - " + threads + " thread(s)\n");        

        for(int run = 0; run < runs; run++){
          sumTime += ParaPi.executeMachinTaylorDividedTask(precision, threads);
        }
        System.out.println("\nAverage time: " + sumTime / runs);
        
        sumTime = 0;

        System.out.println("\n--------------------------------------------------\n");      
        System.out.println("**Testing efficient multithreaded locked - " + threads + " thread(s)\n");        

        for(int run = 0; run < runs; run++){
          sumTime += ParaPi.executeMachinTaylorDividedLockedTask(precision, threads);
        }
        System.out.println("\nAverage time: " + sumTime / runs);
        
        sumTime = 0;
      }
            
      // Do the following two only once
      System.out.println("\n--------------------------------------------------");      
      System.out.println("\n**Testing single threaded\n");
      
      for(int run = 0; run < runs; run++){
        sumTime += ParaPi.executeMachinTaylorSingleThreaded(precision);
      }
      System.out.println("\nAverage time: " + sumTime / runs);
      
      sumTime = 0;
            
      
      System.out.println("\n--------------------------------------------------");      
      System.out.println("\n**Testing split thread (2 threads)\n");
      
      for(int run = 0; run < runs; run++){
        sumTime += ParaPi.executeMachinTaylorSplitTask(precision);
      }
      System.out.println("\nAverage time: " + sumTime / runs);
            
      
    }
  } 
}
