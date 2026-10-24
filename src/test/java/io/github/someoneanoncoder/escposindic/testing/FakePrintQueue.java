/*
 * File:      FakePrintQueue.java
 * Module:    escpos-indic
 * Layer:     test
 * Purpose:   A scriptable javax.print service that records the bytes of each job.
 * Owns:      Supported flavour switch, captured data, scripted job events, scripted print failure.
 * Uses:      javax.print interfaces via java.lang.reflect.Proxy.
 * Used by:   Gradle test tasks.
 * Not here:  Production code.
 * Reuse:     Test only.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.testing;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.print.Doc;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

/** A fake print queue. Configure it, hand {@link #service()} to the code under test. */
public final class FakePrintQueue {

  private final List<byte[]> jobs = new ArrayList<>();
  private final List<PrintJobListener> listeners = new ArrayList<>();
  private boolean rawSupported = true;
  private boolean failOnPrint;
  private Consumer<FakePrintQueue> script = q -> q.fire(Event.NO_MORE_EVENTS);
  private final PrintService service;

  /** Job events the fake can raise. */
  public enum Event {
    COMPLETED,
    TRANSFER_COMPLETE,
    NO_MORE_EVENTS,
    FAILED,
    CANCELED,
    REQUIRES_ATTENTION
  }

  /** Creates a queue named "fake" that accepts raw data and ends each job with NO_MORE_EVENTS. */
  public FakePrintQueue() {
    DocPrintJob job = (DocPrintJob) proxy(DocPrintJob.class, this::handleJob);
    this.service =
        (PrintService)
            proxy(
                PrintService.class,
                (p, m, a) ->
                    switch (m.getName()) {
                      case "getName" -> "fake";
                      case "isDocFlavorSupported" -> rawSupported;
                      case "createPrintJob" -> job;
                      case "toString" -> "FakePrintQueue";
                      case "hashCode" -> 1;
                      case "equals" -> p == a[0];
                      default -> throw new UnsupportedOperationException(m.getName());
                    });
  }

  /**
   * Returns the service to give to the code under test.
   *
   * @return the fake service
   */
  public PrintService service() {
    return service;
  }

  /**
   * Chooses whether the queue claims to accept raw byte arrays.
   *
   * @param supported true to accept
   * @return this fake
   */
  public FakePrintQueue rawSupported(boolean supported) {
    rawSupported = supported;
    return this;
  }

  /**
   * Makes {@code print} throw a {@link PrintException}.
   *
   * @param fail true to fail
   * @return this fake
   */
  public FakePrintQueue failOnPrint(boolean fail) {
    failOnPrint = fail;
    return this;
  }

  /**
   * Sets what happens when a job is submitted. The default raises {@link Event#NO_MORE_EVENTS}.
   *
   * @param newScript called with this fake after the data is captured
   * @return this fake
   */
  public FakePrintQueue onJob(Consumer<FakePrintQueue> newScript) {
    script = newScript;
    return this;
  }

  /**
   * Returns the data of every submitted job.
   *
   * @return the jobs, oldest first
   */
  public List<byte[]> jobs() {
    return jobs;
  }

  /**
   * Raises an event on every listener.
   *
   * @param event the event
   */
  public void fire(Event event) {
    PrintJobEvent e = new PrintJobEvent(service.createPrintJob(), PrintJobEvent.NO_MORE_EVENTS);
    for (PrintJobListener l : List.copyOf(listeners)) {
      switch (event) {
        case COMPLETED -> l.printJobCompleted(e);
        case TRANSFER_COMPLETE -> l.printDataTransferCompleted(e);
        case NO_MORE_EVENTS -> l.printJobNoMoreEvents(e);
        case FAILED -> l.printJobFailed(e);
        case CANCELED -> l.printJobCanceled(e);
        case REQUIRES_ATTENTION -> l.printJobRequiresAttention(e);
      }
    }
  }

  private Object handleJob(Object p, java.lang.reflect.Method m, Object[] a) throws Throwable {
    switch (m.getName()) {
      case "addPrintJobListener" -> listeners.add((PrintJobListener) a[0]);
      case "print" -> {
        if (failOnPrint) {
          throw new PrintException("scripted failure");
        }
        jobs.add((byte[]) ((Doc) a[0]).getPrintData());
        script.accept(this);
      }
      case "toString" -> {
        return "FakeJob";
      }
      case "hashCode" -> {
        return 2;
      }
      case "equals" -> {
        return p == a[0];
      }
      default -> throw new UnsupportedOperationException(m.getName());
    }
    return null;
  }

  private static Object proxy(Class<?> type, InvocationHandler handler) {
    return Proxy.newProxyInstance(
        FakePrintQueue.class.getClassLoader(), new Class<?>[] {type}, handler);
  }
}
