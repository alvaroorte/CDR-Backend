package bo.cdr.app.appbase.presentation.response;

import java.time.LocalDateTime;

import bo.cdr.app.common.Pojo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Pojo
@Getter
@Setter
@NoArgsConstructor
public class BasePojo {

  protected Boolean active;

  protected String createdBy;

  protected String updatedBy;

  protected LocalDateTime createdAt;

  protected LocalDateTime updatedAt;

}
