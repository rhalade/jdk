/*
 * Copyright (c) 2001, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jdi.EventSet.eventIterator;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * EventSet.                                                    <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.EventSet.eventIterator()</code>            <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks that for VMStart, VMDeath, and               <BR>
 * VMDisconnect Events:                                         <BR>
 *  - the method returns non-null object, and                   <BR>
 *  - object's class is subclass of class Iterator.             <BR>
 * <BR>
 * The test works as follows.                                   <BR>
 * <BR>
 * Upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent, it checks up on the method and        <BR>
 * sets up the request for debuggee's                                   <BR>
 * ClassPrepareEvent with SUSPEND_EVENT_THREAD, resumes the VM,         <BR>
 * and waits for the event within the predefined time interval.         <BR>
 * If no the ClassPrepareEvent received, the test is FAILED.            <BR>
 * <BR>
 * The debugger then resumes the debuggee which will normally end       <BR>
 * that will result in VMDeathEvent and waits for the event.            <BR>
 * Upon getting the VMDeathEvent, it checks up on the method and        <BR>
 * waits for VMDisconnectEvent, and upon getting it, checks up on this. <BR>
 * <BR>
 */

public class eventiterator004 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        if (result != 0) {
            throw new RuntimeException("TEST FAILED with result " + result);
        }
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new eventiterator004().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return exitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventSet.eventIterator.eventiterator004a";

    private String testedClassName =
      "nsk.jdi.EventSet.eventIterator.TestClass";

    //====================================================== test program

    //  Event #:
    //  0-6  : AccessWatchpoint, ModificationWatchpoint, Breakpoint, Exception,
    //         MethodEntry, MethodExit, Step
    //  7-8  : ClassPrepare, ClassUnload
    //  9-10 : ThreadDeath, ThreadStart
    // 11-13 : VMDeath, VMDisconnect, VMStart

    EventSet     eventSets[] = new EventSet [14];
    EventRequest eRequests[] = new EventRequest[14];

    int eventFlags[] = { 0,0,0,0, 0,0,0,0, 3,0,0,0, 1,1 };

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = argsHandler.getWaitTime() * 60000;

        try {
            log2("launching a debuggee :");
            log2("       " + debuggeeName);
            if (argsHandler.verbose()) {
                debuggee = binder.bindToDebugeeNoWait(debuggeeName + " -vbs");
            } else {
                debuggee = binder.bindToDebugeeNoWait(debuggeeName);
            }
            if (debuggee == null) {
                log3("ERROR: no debuggee launched");
                return FAILED;
            }
            log2("debuggee launched");
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            log2("       test cancelled");
            return FAILED;
        }

        debuggee.redirectOutput(logHandler);

        vm = debuggee.VM();

        eventQueue = vm.eventQueue();
        if (eventQueue == null) {
            log3("ERROR: eventQueue == null : TEST ABORTED");
            vm.exit(PASS_BASE);
            return FAILED;
        }

        log2("invocation of the method runTest()");
        switch (runTest()) {

            case 0 :  log2("test phase has finished normally");
                      log2("   waiting for the debuggee to finish ...");
                      debuggee.waitFor();

                      log2("......getting the debuggee's exit status");
                      int status = debuggee.getStatus();
                      if (status != PASS_BASE) {
                          log3("ERROR: debuggee returned UNEXPECTED exit status: " +
                              status + " != PASS_BASE");
                          testExitCode = FAILED;
                      } else {
                          log2("......debuggee returned expected exit status: " +
                              status + " == PASS_BASE");
                      }
                      break;

            default : log3("ERROR: runTest() returned unexpected value");

            case 1 :  log3("test phase has not finished normally: debuggee is still alive");
                      log2("......forcing: vm.exit();");
                      testExitCode = FAILED;
                      try {
                          vm.exit(PASS_BASE);
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;

            case 2 :  log3("test cancelled due to VMDisconnectedException");
                      log2("......trying: vm.process().destroy();");
                      testExitCode = FAILED;
                      try {
                          Process vmProcess = vm.process();
                          if (vmProcess != null) {
                              vmProcess.destroy();
                          }
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;
            }

        return testExitCode;
    }


   /*
    * Return value: 0 - normal end of the test
    *               1 - ubnormal end of the test
    *               2 - VMDisconnectedException while test phase
    */

    private int runTest() {

        try {
            log2("waiting for VMStartEvent");
            getEventSet();
            check();


            if (eventIterator.nextEvent() instanceof VMStartEvent) {
                log2("VMStartEvent received; test begins");

                testRun();

                log2("waiting for VMDeathEvent");
                getEventSet();
                check();

                if ( !(eventIterator.nextEvent() instanceof VMDeathEvent) ) {
                    log3("ERROR: last event is not the VMDeathEvent");
                    return 1;
                }

                log2("waiting for VMDisconnectEvent");
                getEventSet();
                check();
                if ( !(eventIterator.nextEvent() instanceof VMDisconnectEvent) ) {
                    log3("ERROR: last event is not the VMDisconnectEvent");
                    return 1;
                }

                return 0;
            } else {
                log3("ERROR: first event is not the VMStartEvent");
                return 1;
            }
        } catch ( VMDisconnectedException e ) {
            log3("ERROR: VMDisconnectedException : " + e);
            return 2;
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            return 1;
        }

    }

    private void testRun()
                 throws JDITestRuntimeException, Exception {

        eventRManager = vm.eventRequestManager();

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(debuggeeName);

        log2("......setting up ClassPrepareRequest");
        eRequests[7] = cpRequest;

        cpRequest.enable();
        vm.resume();

        getEventSet();
        eventSets[7] = eventSet;

        cpRequest.disable();

        ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();
        debuggeeClass = event.referenceType();

        if (!debuggeeClass.name().equals(debuggeeName))
           throw new JDITestRuntimeException("** Unexpected ClassName for ClassPrepareEvent **");

        log2("      received: ClassPrepareEvent for debuggeeClass");

        log2("......setting up ClassPrepareEvent");

        setupBreakpointForCommunication(debuggeeClass);

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        for (int i = 0; ; i++) {

            vm.resume();
            breakpointForCommunication();

            int instruction = ((IntegerValue)
                               (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1(":::::: case: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private void check() {

        log2("......checking up on EventIterator");
        if (eventIterator == null) {
            testExitCode = FAILED;
            log3("ERROR: eventIterator == null");
        }
        if ( !(eventIterator instanceof Iterator) ) {
            testExitCode = FAILED;
            log3("ERROR: eventIterator is NOT instanceof Iterator");
        }
    }

}
