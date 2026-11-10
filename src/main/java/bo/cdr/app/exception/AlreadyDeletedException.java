package bo.cdr.app.exception;

import bo.cdr.app.util.Messages;

public class AlreadyDeletedException extends RuntimeException {

  public AlreadyDeletedException(String message) {
    super(message);
  }

  public AlreadyDeletedException(String entity, Integer id) {
    super(String.format(Messages.getProperty("exception.alreadyDeleted.message"), entity, id));
  }
}
