package mpi.aida.util;

import java.util.Arrays;

import edu.stanford.nlp.util.StringUtils;


public class DebugUtils {
  public static String getCallingMethods() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    String callers = StringUtils.join(Arrays.copyOfRange(stackTraceElements, 2, stackTraceElements.length), "\t\n");
    return callers;
  }
  
  public static String getCallingMethod() {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    String caller = stackTraceElements[4].getClassName()+"."+stackTraceElements[4].getMethodName() + ":" + stackTraceElements[4].getLineNumber();
    return caller;
  }
}
