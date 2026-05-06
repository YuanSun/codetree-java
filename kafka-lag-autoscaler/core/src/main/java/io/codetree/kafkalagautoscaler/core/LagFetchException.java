package io.codetree.kafkalagautoscaler.core;

/** Wraps any infrastructure error raised while fetching lag from the backend. */
public class LagFetchException extends RuntimeException {

  public LagFetchException(String message, Throwable cause) {
    super(message, cause);
  }
}
