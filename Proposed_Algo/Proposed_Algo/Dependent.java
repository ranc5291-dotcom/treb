package Proposed_Algo;


import java.util.*;
import java.lang.Math;

class Dependent
{
  //class for fog node ranking (TOPSIS)
  static class fogNode
  {
    int Id;
    double computational_power_of_fogNode;
    double power_of_fogNode;
    double deadline;
    double computational_demand;
    double energy;
    double inputSize;
    double weightage;
    double quota;

  }
  static class iotDevice
  {
    int Id;
    double computational_demand;
    double inputSize;
    double energy;
    double latency;
    double weightage;
    double dependency;
  }
  static class Binarycases
  {
    double energy;
    double total_time;
  }

  public static double log2 (double x)
  {
    return Math.log (x) / Math.log (2);
  }
  public static int MatchingAlgo (iotDevice[]iot, fogNode[]fog, int count)
  {
    //we are implementing one to many like every iot task will be having multiple options to go 
    int total_number_of_iot_tasks = iot.length;
    //storing the matching of iot, fog 
    int total_number_of_fog_nodes = fog.length;
    HashMap < Integer, Integer > matching = new HashMap < Integer, Integer > ();
    //to check weatrher the task went to any fog device or any fog assigned or not to that particular task
    int flag = 0;
    for (int i = 0; i < total_number_of_iot_tasks; i++)
	{
		flag = 0;		//reinitializing the flag 0 to track again
	
		for (int j = 0; j < total_number_of_fog_nodes; j++)
		  {
		    if (fog[j].quota >= iot[i].dependency)
		      {
			fog[j].quota = fog[j].quota - iot[i].dependency;
			matching.put (iot[i].Id, fog[j].Id);
			flag = 1;	//to track that is the iot task assigned to a fog node or node
	
	
		      }
		    if (flag == 1)	//if task is assigned according to preference then breaking the loop and going on next task
		      break;
	
		  }
		if (flag == 0)
		  {
		    matching.put (iot[i].Id, -1);
		    count++;
		  }
      }





    //printing pairs of HashMap
//    System.out.println ("printing matched iot fog pair ...");
//  for (Map.Entry m:matching.entrySet ())
//      {
//	if ((Integer) m.getValue () != -1)	//if none of the fog node is assigned then  it will go in the else case 
//	  System.out.println ("iot task number " + m.getKey () +
//			      " is matched with " +
//			      "fog node " + m.getValue ());
//	else
//	  System.out.println ("iot task number" + m.getKey () +
//			      " is not matched with any fog node due to busyness of fog nodes");
//      }

    return count;
  }
  
  
  // ---------------------------------------------------------------------------
  //  TOPSIS ranking (replaces AHP). Weights come from the Entropy method.
  // ---------------------------------------------------------------------------

  // true  -> a HIGHER value of a criterion gives a HIGHER rank (same behaviour as the old
  //          weighted-sum + descending sort).
  // false -> a LOWER value gives a higher rank (cost-style: cheapest / fastest first).
  static final boolean HIGHER_VALUE_FIRST = true;

  // Entropy weight method: a criterion whose values differ a lot between alternatives
  // carries more information, so it gets a bigger weight. Weights always sum to 1.
  public static double[] entropyWeights (double[][] m)
  {
    int rows = m.length;
    int cols = m[0].length;
    double[] w = new double[cols];

    //with a single alternative there is nothing to compare, so use equal weights
    if (rows < 2)
      {
	for (int j = 0; j < cols; j++)
	  w[j] = 1.0 / cols;
	return w;
      }

    double k = 1.0 / Math.log (rows);
    double[] d = new double[cols];
    double dSum = 0;
    for (int j = 0; j < cols; j++)
      {
	double colSum = 0;
	for (int i = 0; i < rows; i++)
	  colSum += m[i][j];

	double entropy = 1;	//no information -> maximum entropy
	if (colSum > 0)
	  {
	    entropy = 0;
	    for (int i = 0; i < rows; i++)
	      {
		double p = m[i][j] / colSum;
		if (p > 0)
		  entropy -= p * Math.log (p);
	      }
	    entropy = entropy * k;
	  }
	d[j] = 1 - entropy;	//degree of diversification
	dSum += d[j];
      }

    for (int j = 0; j < cols; j++)
      w[j] = (dSum > 1e-12) ? d[j] / dSum : 1.0 / cols;
    return w;
  }

  // TOPSIS: returns one closeness score in [0,1] per row of the decision matrix.
  // A score near 1 means "close to the ideal best, far from the ideal worst".
  public static double[] topsisScores (double[][] m, String label)
  {
    int rows = m.length;
    int cols = m[0].length;

    //step 1: weights (entropy)
    double[] w = entropyWeights (m);
    System.out.println ("Entropy weights for " + label + " : " +
			Arrays.toString (w));

    //step 2 + 3: vector normalization, then multiply by the weight
    double[][] v = new double[rows][cols];
    for (int j = 0; j < cols; j++)
      {
	double squareSum = 0;
	for (int i = 0; i < rows; i++)
	  squareSum += m[i][j] * m[i][j];
	double norm = Math.sqrt (squareSum);
	for (int i = 0; i < rows; i++)
	  v[i][j] = (norm > 0 ? m[i][j] / norm : 0) * w[j];
      }

    //step 4: ideal best and ideal worst value of every column
    double[] best = new double[cols];
    double[] worst = new double[cols];
    for (int j = 0; j < cols; j++)
      {
	double max = v[0][j];
	double min = v[0][j];
	for (int i = 1; i < rows; i++)
	  {
	    max = Math.max (max, v[i][j]);
	    min = Math.min (min, v[i][j]);
	  }
	best[j] = HIGHER_VALUE_FIRST ? max : min;
	worst[j] = HIGHER_VALUE_FIRST ? min : max;
      }

    //step 5 + 6: distance to best / worst, then closeness score
    double[] score = new double[rows];
    for (int i = 0; i < rows; i++)
      {
	double dPlus = 0;
	double dMinus = 0;
	for (int j = 0; j < cols; j++)
	  {
	    dPlus += (v[i][j] - best[j]) * (v[i][j] - best[j]);
	    dMinus += (v[i][j] - worst[j]) * (v[i][j] - worst[j]);
	  }
	dPlus = Math.sqrt (dPlus);
	dMinus = Math.sqrt (dMinus);
	double total = dPlus + dMinus;
	//if every alternative is identical the score is 0.5 for all of them
	score[i] = (total > 1e-12) ? dMinus / total : 0.5;
      }
    return score;
  }


  public static fogNode[] fogpreference (double downlink_data_rate,
					 double uplink_data_rate,
					 double output_size,
					 double capacity_of_VRU)
  {
    fogNode[]fog;
    Scanner sc = new Scanner (System.in);	//System.in is a standard input stream  
    System.out.println ("Enter the number of fogNodes");
    int n = sc.nextInt ();
    fog = new fogNode[n];
    for (int i = 0; i < n; i++){
		fog[i] = new fogNode ();
		fog[i].Id = i + 1;
		System.out.println ("enter the following for fog node:" + (i + 1));
		System.out.println ("power of fognode");
		fog[i].power_of_fogNode = sc.nextDouble ();
		System.out.println ("computational power  of fognode");
		fog[i].computational_power_of_fogNode = sc.nextDouble ();
		System.out.println ("deadline of fognode");
		fog[i].deadline = sc.nextDouble ();
		System.out.println ("inputSize of fognode in Kb");
		fog[i].inputSize = sc.nextDouble ();
		System.out.println ("computational_demand of fognode");
		fog[i].computational_demand = sc.nextDouble ();
		System.out.println ("enter quota of the fognde:: ");
		fog[i].quota = sc.nextDouble ();
	
		double upload_latency = fog[i].inputSize / (uplink_data_rate * 1000);
		double download_latency = output_size / (downlink_data_rate * 1000);
		double computational_latency = (fog[i].computational_demand ) / capacity_of_VRU;
		fog[i].energy = fog[i].power_of_fogNode * (upload_latency + download_latency) + computational_latency * fog[i].computational_power_of_fogNode;
    }

    // TOPSIS over all fog nodes: criteria = energy, deadline
    double[][] decision = new double[n][2];
    for (int i = 0; i < n; i++)
      {
	decision[i][0] = fog[i].energy;
	decision[i][1] = fog[i].deadline;
      }
    double[] score = topsisScores (decision, "fog nodes (energy, deadline)");
    for (int i = 0; i < n; i++)
      fog[i].weightage = score[i];

    // sorting the fog nodes based on preference (highest TOPSIS score first)
    for (int i = 0; i < n; i++){
	for (int j = i + 1; j < n; j++){
	    if (fog[i].weightage < fog[j].weightage)
	      {
			fogNode temp = fog[i];
			fog[i] = fog[j];
			fog[j] = temp;
	      }
	  }
    }
    System.out.println ("order of the fog nodes are following\n");
    for (int i = 0; i < n; i++)
      {
    	System.out.print (fog[i].Id + " ");
      }
    return fog;
  }

  //function for both local execution
  public static Binarycases both_locally_executed (double
						   local_execution_time,
						   double uplink_time,
						   double downlink_time,
						   double power_of_iotd1,
						   double power_of_iotd2,
						   double
						   computational_power_of_fognode,
						   double computational_delay,
						   double X, double S,
						   double N,
						   double power_of_fogDevice)
  {
    double total_time =
      2 * local_execution_time + uplink_time + downlink_time;
    double total_energy =
      power_of_iotd1 * (local_execution_time) +
      power_of_iotd2 * (local_execution_time) + (uplink_time +
						 downlink_time) *
      power_of_fogDevice;

    double computation_time_for_iotd1 =
      (uplink_time + computational_delay) * (X - 1);
    double computation_time_for_iotd2 =
      (uplink_time + computational_delay) * (S - 1);
    double remaining_time = (uplink_time + computational_delay) * (N - S);
    double waiting_time =
      Math.max (computation_time_for_iotd1, computation_time_for_iotd2);
    total_energy =
      total_energy + (X - 2 +
		      N) * (computational_power_of_fognode *
			    computational_delay +
			    power_of_fogDevice * (uplink_time +
						  downlink_time));

    total_time = total_time + remaining_time + waiting_time;
    Binarycases bn = new Binarycases ();
    bn.total_time = total_time;
    bn.energy = total_energy;

    System.out.println ("CASE 1: both tasks are executed locally!!");
    // System.out.println("total time is:: "+total_time);
    //     System.out.println("total energy is:: "+total_energy+"\n");
    return bn;

  }

  //function for device 1 on fog node and 2 local execution
  public static Binarycases first_onfog_second_locally (double
							local_execution_time,
							double uplink_time,
							double downlink_time,
							double power_of_iotd1,
							double power_of_iotd2,
							double
							computational_power_of_fognode,
							double
							computational_delay,
							double X, double S,
							double N,
							double
							power_of_fogDevice)
  {
    double total_time =
      local_execution_time + uplink_time + downlink_time +
      computational_delay;
    double total_energy =
      power_of_fogDevice * (uplink_time + downlink_time) +
      computational_power_of_fognode * computational_delay +
      power_of_iotd2 * (local_execution_time);
    double computation_time_for_iotd1 =
      (uplink_time + computational_delay) * (X - 1);
    double computation_time_for_iotd2 =
      (uplink_time + computational_delay) * (S - 1);
    double remaining_time = (uplink_time + computational_delay) * (N - S);
    double waiting_time =
      Math.max (computation_time_for_iotd1, computation_time_for_iotd2);
    total_energy =
      total_energy + (X - 2 +
		      N) * (computational_power_of_fognode *
			    computational_delay +
			    power_of_fogDevice * (uplink_time +
						  downlink_time));


    total_time = total_time + remaining_time + waiting_time;
    Binarycases bn = new Binarycases ();
    bn.total_time = total_time;
    bn.energy = total_energy;
    System.out.println ("CASE 2: iotd1 execute at fog and d2 locally!!");
    // System.out.println("total time is:: "+total_time);
    //     System.out.println("total energy is:: "+total_energy+"\n");
    return bn;

  }

  //function for task 1 local execution(on iotd1) and  task 2 on fog node
  public static Binarycases first_locally_second_onfog (double
							local_execution_time,
							double uplink_time,
							double downlink_time,
							double power_of_iotd1,
							double power_of_iotd2,
							double
							computational_power_of_fognode,
							double
							computational_delay,
							double X, double S,
							double N,
							double
							power_of_fogDevice)
  {
    double total_time =
      local_execution_time + 2 * uplink_time + computational_delay;
    double total_energy =
      (2 * uplink_time * power_of_fogDevice) +
      computational_delay * computational_power_of_fognode +
      power_of_iotd1 * (local_execution_time);
    double computation_time_for_iotd1 =
      (uplink_time + computational_delay) * (X - 1);
    double computation_time_for_iotd2 =
      (uplink_time + computational_delay) * (S - 1);
    double remaining_time = (uplink_time + computational_delay) * (N - S);
    double waiting_time =
      Math.max (computation_time_for_iotd1, computation_time_for_iotd2);
    total_energy = total_energy + (X - 2 + N) * (computational_power_of_fognode * computational_delay + power_of_fogDevice * (uplink_time + downlink_time));	//X and Sth task are dependent so X-1+S-1+Y-S will be remaining task that is X-2+N

    total_time = total_time + remaining_time + waiting_time;
    Binarycases bn = new Binarycases ();
    bn.total_time = total_time;
    bn.energy = total_energy;
    System.out.println ("CASE 3: iotd1 execute locally and d2 at fog!!");
    // System.out.println("total time is:: "+total_time);
    //     System.out.println("total energy is:: "+total_energy+"\n");

    return bn;
  }
  //both execute on fog node (iotd1 and iotd2)
  public static Binarycases both_onfog (double local_execution_time,
					double uplink_time,
					double downlink_time,
					double power_of_iotd1,
					double power_of_iotd2,
					double computational_power_of_fognode,
					double computational_delay, double X,
					double S, double N,
					double power_of_fogDevice)
  {
    double total_time = 2 * uplink_time + 2 * computational_delay;
    double total_energy =
      computational_power_of_fognode * (2 * uplink_time +
					2 * computational_delay);
    double computation_time_for_iotd1 =
      (uplink_time + computational_delay) * (X - 1);
    double computation_time_for_iotd2 =
      (uplink_time + computational_delay) * (S - 1);
    double remaining_time = (uplink_time + computational_delay) * (N - S);
    double waiting_time =
      Math.max (computation_time_for_iotd1, computation_time_for_iotd2);
    total_energy =
      total_energy + (X - 2 +
		      N) * (computational_power_of_fognode *
			    computational_delay +
			    power_of_fogDevice * (uplink_time +
						  downlink_time));


    total_time = total_time + remaining_time + waiting_time;
    Binarycases bn = new Binarycases ();
    bn.total_time = total_time;
    bn.energy = total_energy;
    System.out.println ("CASE 4: iotd1 and d2 both execute at fog!!");
    // System.out.println("total time is:: "+total_time);
    //     System.out.println("total energy is:: "+total_energy+"\n");

    return bn;
  }


  public static iotDevice[] iotpreference (double local_execution_time,
					   double uplink_time,
					   double downlink_time,
					   double power_of_iotd1,
					   double power_of_iotd2,
					   double computational_power_of_fognode,
					   double computational_delay,
					   double X, double S, double N,
					   double power_of_fogDevice,
					   double downlink_data_rate,
					   double uplink_data_rate,
					   double output_size,
					   double capacity_of_VRU)
  {
    Scanner sc = new Scanner (System.in);
    iotDevice[]iot;

    System.out.println ("\nEnter the number of dependent task : ");
    int number_of_depenenttask = sc.nextInt ();
    int n = number_of_depenenttask;
    iot = new iotDevice[n];
    Binarycases bn = new Binarycases ();

    for (int i = 0; i < n; i++)
      {
	int taskNumber = i % 4;	//the 4 binary cases repeat for all dependent tasks
	if (taskNumber == 0)
	  {
	    bn = both_locally_executed (local_execution_time, uplink_time,
					downlink_time, power_of_iotd1,
					power_of_iotd2,
					computational_power_of_fognode,
					computational_delay, X, S, N,
					power_of_fogDevice);
	  }
	else if (taskNumber == 1)
	  {
	    bn = first_onfog_second_locally (local_execution_time, uplink_time,
					     downlink_time, power_of_iotd1,
					     power_of_iotd2,
					     computational_power_of_fognode,
					     computational_delay, X, S, N,
					     power_of_fogDevice);
	  }
	else if (taskNumber == 2)
	  {
	    bn = first_locally_second_onfog (local_execution_time, uplink_time,
					     downlink_time, power_of_iotd1,
					     power_of_iotd2,
					     computational_power_of_fognode,
					     computational_delay, X, S, N,
					     power_of_fogDevice);
	  }
	else
	  {
	    bn = both_onfog (local_execution_time, uplink_time, downlink_time,
			     power_of_iotd1, power_of_iotd2,
			     computational_power_of_fognode, computational_delay,
			     X, S, N, power_of_fogDevice);
	  }
	iot[i] = new iotDevice ();
	iot[i].Id = i + 1;
	iot[i].energy = bn.energy;
	iot[i].dependency = 2;
	iot[i].latency = bn.total_time;
	iot[i].computational_demand = 210;
	iot[i].inputSize = 400;
      }

    // TOPSIS over all tasks: criteria = energy, computational demand, latency
    double[][] decision = new double[n][3];
    for (int i = 0; i < n; i++)
      {
	decision[i][0] = iot[i].energy;
	decision[i][1] = iot[i].computational_demand;
	decision[i][2] = iot[i].latency;
      }
    double[] score =
      topsisScores (decision, "IoT tasks (energy, computational demand, latency)");
    for (int i = 0; i < n; i++)
      iot[i].weightage = score[i];

    //ordering the iot devices according to their TOPSIS score (highest first)
    for (int i = 0; i < n; i++)
      {
	for (int j = i + 1; j < n; j++)
	  {
	    if (iot[i].weightage < iot[j].weightage)
	      {
		iotDevice temp = iot[i];
		iot[i] = iot[j];
		iot[j] = temp;
	      }
	  }
      }
    System.out.println ("order of the iot devices are following\n");
    for (int i = 0; i < n; i++)
      {
	System.out.print (iot[i].Id + " ");
      }
    System.out.println ("\n");
    return iot;
  }

  public static double log10 (int x)
  {
    return Math.log (x) / Math.log (10);
  }

  public static void main (String[]args)
  {
    Scanner sc = new Scanner (System.in);
    //taking all required inputs
    System.out.println ("please provide the following inputs::\n");
    System.out.println ("enter the computational demand of the task in million cycle::");
    double computational_demand = 300; //sc.nextDouble();
    System.out.println ("enter frequency in megahertz::");
    double frequency = 16; //sc.nextDouble ();
    System.out.println ("enter channel frequency(bandwidth) in megahetz::");
    double bandwidth = 10; //sc.nextDouble ();
    System.out.println ("enter Distance of IOT device to fog node in meter::");
    int distance_to_fog_node = 100; //sc.nextInt ();
    System.out.println ("enter the power of iot device in watts::");
    double power_of_iot_device = 0.5; //sc.nextDouble ();
    System.out.println ("enter the power of fog node in watts :: ");
    double power_of_fogDevice = 1.5; //sc.nextDouble ();
    System.out.println ("enter the output size in Kb::");
    double output_size = 15; //sc.nextDouble ();
    System.out.println ("enter the computational power of fog node::");
    double computational_power_of_fognode = 0.35; //sc.nextDouble ();

    System.out.println ("enter the value of X::");
    double X = 3; //sc.nextDouble ();
    System.out.println ("enter the value of S::");
    double S = 4; //sc.nextDouble ();
    System.out.println ("enter the value of N::");
    double N = 5; //sc.nextDouble ();


    double local_execution_time = computational_demand / frequency;
    System.out.println ("local execution time is:: " + local_execution_time + "\n");

    double log10distance_to_fog_node = log10 (distance_to_fog_node);

    double PLIJ = 38.02 + 20 * log10distance_to_fog_node;
    //   System.out.println(PLIJ);
    double power_to_calculate = -(PLIJ / 10);
    double channel_gain = Math.pow (10, power_to_calculate);

    double noise_power = Math.pow (10, -10);

    double in_the_log = 1 + (power_of_iot_device * channel_gain) / noise_power;

    double log2in_the_log = log2 (in_the_log);
    double uplink_data_rate = bandwidth * log2in_the_log;

    System.out.println ("uplink data rate is in megahetz :: " + uplink_data_rate + "\n");

    double downlink_data_rate = bandwidth * log2 (1 + (power_of_fogDevice * channel_gain) / noise_power);
    System.out.println ("downlink  data rate is in megahetz :: " + downlink_data_rate + "\n");
    double Energy_of_IOT_device = power_of_iot_device * local_execution_time;
    System.out.println ("enery of iot device is in joule :: " +	Energy_of_IOT_device + "\n");
    
//    double computational_capacity_of_one_fognode = 50000;
//    double Iij = 300;
// 
//    double computational_delay =
//      (computational_demand * Iij) / (computational_capacity_of_one_fognode);
    
    double fog_node_capacity = 6 * Math.pow(10, 9);
    double temp_quota = 50.0;
    double capacity_of_VRU = fog_node_capacity/temp_quota ;
    
    double computational_delay = (computational_demand) / (capacity_of_VRU);

    double uplink_data_rate_time = output_size / (uplink_data_rate * 1000);
    double downlink_data_rate_time = output_size / (downlink_data_rate * 1000);



    //calling function to rank fognodes 
    fogNode[]fog;
    iotDevice[]iot;
    //calling fog preferrence to get the order of fog nodes 
    fog = fogpreference (downlink_data_rate, uplink_data_rate, output_size, capacity_of_VRU);
    //calling iot preferrence to get the order of iot devices
    iot = iotpreference (local_execution_time, uplink_data_rate_time,
		     downlink_data_rate_time, power_of_iot_device,
		     power_of_iot_device, computational_power_of_fognode,
		     computational_delay, X, S, N, power_of_fogDevice,
		     downlink_data_rate, uplink_data_rate, output_size, capacity_of_VRU);
    
    //Calling Matching Theory algorithm
    
    int count = MatchingAlgo (iot, fog, 0);


    //printing total energies
//    System.out.println("Printing Energy values for Fog Nodes  ---------->");
    double total_energy = 0.0;
    int no_fog_nodes = fog.length;
    for(int i=0; i<no_fog_nodes; i++) {
    	total_energy += fog[i].energy;
    }

    int no_iots = iot.length;
    System.out.println("No of IoT : "+no_iots);
    for(int i=0; i<no_iots; i++) {
    	total_energy += iot[i].energy;
    }
    System.out.println("\nTotal Energy for " + no_iots +" devices = "+ total_energy);
    
    double total_latency = 0.0;
    for(int i=0; i<no_iots; i++) {
    	total_latency += iot[i].latency;
    }
    System.out.println("\nTotal Latency for " + no_iots +" devices = "+ total_latency);
    
    System.out.println("Number of Outages : " + count);
    sc.close();
  }
}