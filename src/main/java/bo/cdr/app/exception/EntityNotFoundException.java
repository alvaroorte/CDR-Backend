package bo.cdr.app.exception;


import bo.cdr.app.util.Messages;

import java.time.LocalDate;

public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException(String entity, Integer id) {
    super(String.format(Messages.getProperty("exception.entityNotFoundById.message"), entity,
            id.toString()));
  }

  public EntityNotFoundException(String entity, String invalidValue) {
    super(String.format(Messages.getProperty("exception.entityNotFoundByValue.message"), entity,
            invalidValue));
  }

  public EntityNotFoundException(String entity, String column, String invalidValue) {
    super(String.format(Messages.getProperty("exception.entityNotFoundByColumn.message"), entity,
            column, invalidValue));
  }

  public EntityNotFoundException(String entity, String column, String invalidValue,
                                 LocalDate date) {
    super(String.format(Messages.getProperty("exception.entityNotFoundByColumnAndDate.message"),
            entity, column, invalidValue, date));
  }
}
