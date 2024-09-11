package org.example;

import java.time.LocalDateTime;

public class RLPQ_Config {

    static final int MAX_REQUESTS = 10000;
    static final int REQUEST_COUNT_LIMIT = 50;

    final static Long PENALTY_LIMIT = /* REQUEST_COUNT_LIMIT + */ Long.MAX_VALUE; //was @ 10;



    final static int HIGH_PRIO = 100;
    final static int LOW_PRIO = 20;
    final static int REJECTED = 0;

    /* after dequeueing swapQ times, swap from fastQ to slowQ once */
    final static int swapQ = 10;

    /* after RESET_CNT dequeues, reset the dQcnt to avoid overflows*/
    final static int RESET_CNT = swapQ + 1;

    final static long ARRIVL_TIME_DIFF_THRESH = 15_00_000L; //nanos!

    final static String FILE_OUT = "./log." + LocalDateTime.now() + ".csv";

    final static double REQ_DIST_PROBABILITY = 0.7;

    final static int FAVORED_USER = 2;

    final static int MAX_USER = 10;

    final static Long RANDOM_SEED = 42L;

    final static int D_QUEUE_THREAD_EXTRA_ROUNDS = 2000; //dev only, TODO: remove later!!


}
