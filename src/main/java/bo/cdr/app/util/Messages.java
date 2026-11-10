package bo.cdr.app.util;

import java.util.ResourceBundle;

public class Messages {

  private static final ResourceBundle messages = ResourceBundle.getBundle("notification-messages");

  public static String getProperty(String key) {
    return messages.getString(key);
  }
}
