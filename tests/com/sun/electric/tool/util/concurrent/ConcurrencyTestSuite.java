package com.sun.electric.tool.util.concurrent;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;


@RunWith(Suite.class)
@Suite.SuiteClasses({
	ThreadPool_T.class,
	PForJob_T.class,
    PReduceJob_T.class
})
public class ConcurrencyTestSuite {

}
