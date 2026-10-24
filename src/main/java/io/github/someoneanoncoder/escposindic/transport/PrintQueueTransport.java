/*
 * File:      PrintQueueTransport.java
 * Module:    escpos-indic
 * Layer:     transport
 * Purpose:   Sends the byte stream to an installed printer queue, the usual way on Windows.
 * Owns:      Print service lookup, raw document submission, waiting for the job outcome.
 * Uses:      javax.print (DocFlavor.BYTE_ARRAY.AUTOSENSE).
 * Used by:   USB and driver-installed printers.
 * Not here:  Driver installation, status monitoring.
 * Reuse:     Pass any PrintService, which also allows a fake in tests.
 * Plan:      L3-05
 */
package io.github.someoneanoncoder.escposindic.transport;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;

/**
 * Submits each job as a byte-array document with the flavour {@code BYTE_ARRAY.AUTOSENSE}, which
 * asks the platform to pass the bytes to the printer as they are instead of rendering them. On
 * Windows the data goes to the spooler of the named queue; the queue's driver must accept raw data
 * (a "Generic / Text Only" or the vendor's driver both do). Whether a given driver really passes
 * the bytes through untouched cannot be known without that printer; see the README under "Not
 * verified".
 *
 * <p>The call waits for the job's first final event (completed, data transferred, no more events,
 * failed or cancelled) up to the timeout.
 */
public final class PrintQueueTransport implements Transport {

  private static final DocFlavor RAW = DocFlavor.BYTE_ARRAY.AUTOSENSE;

  private final PrintService service;
  private final Duration timeout;

  /**
   * Creates a transport for a print service.
   *
   * @param service the queue, not null
   * @param timeout longest wait for the job outcome, more than zero
   * @throws IllegalArgumentException if the timeout is not positive
   */
  public PrintQueueTransport(PrintService service, Duration timeout) {
    this.service = Objects.requireNonNull(service, "service");
    Objects.requireNonNull(timeout, "timeout");
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be more than zero");
    }
    this.timeout = timeout;
  }

  /**
   * Lists the names of the print queues installed on this machine.
   *
   * @return the names, possibly empty
   */
  public static List<String> queueNames() {
    List<String> names = new ArrayList<>();
    for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
      names.add(s.getName());
    }
    return names;
  }

  /**
   * Finds a queue by its exact name.
   *
   * @param name the queue name as listed by {@link #queueNames()}, not null
   * @param timeout longest wait for the job outcome, more than zero
   * @return the transport, or empty if no queue has that name
   */
  public static Optional<PrintQueueTransport> named(String name, Duration timeout) {
    Objects.requireNonNull(name, "name");
    for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
      if (s.getName().equals(name)) {
        return Optional.of(new PrintQueueTransport(s, timeout));
      }
    }
    return Optional.empty();
  }

  @Override
  public void send(byte[] data) throws IOException {
    TransportChecks.requireData(data);
    if (!service.isDocFlavorSupported(RAW)) {
      throw new IOException("the print queue does not accept raw data");
    }
    Doc doc = new SimpleDoc(data, RAW, null);
    DocPrintJob job = service.createPrintJob();
    Outcome outcome = new Outcome();
    job.addPrintJobListener(outcome);
    try {
      job.print(doc, null);
    } catch (PrintException e) {
      throw new IOException("the print queue refused the job", e);
    }
    outcome.await(timeout);
  }

  /** Collects the first final event of a job. */
  private static final class Outcome implements PrintJobListener {
    private final CountDownLatch done = new CountDownLatch(1);
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile String failure;

    void await(Duration timeout) throws IOException {
      try {
        if (!done.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
          throw new IOException("the print queue did not finish in time");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new InterruptedIOException("interrupted while printing");
      }
      String reason = failure;
      if (reason != null) {
        throw new IOException(reason);
      }
    }

    private void finish(String failureReason) {
      if (finished.compareAndSet(false, true)) {
        failure = failureReason;
        done.countDown();
      }
    }

    @Override
    public void printDataTransferCompleted(PrintJobEvent event) {
      finish(null);
    }

    @Override
    public void printJobCompleted(PrintJobEvent event) {
      finish(null);
    }

    @Override
    public void printJobFailed(PrintJobEvent event) {
      finish("the print job failed");
    }

    @Override
    public void printJobCanceled(PrintJobEvent event) {
      finish("the print job was cancelled");
    }

    @Override
    public void printJobNoMoreEvents(PrintJobEvent event) {
      finish(null);
    }

    @Override
    public void printJobRequiresAttention(PrintJobEvent event) {
      // Paper out or cover open: the job stays pending, so keep waiting for a final event.
    }
  }
}
