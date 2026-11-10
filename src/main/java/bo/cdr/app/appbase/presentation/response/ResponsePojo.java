package bo.cdr.app.appbase.presentation.response;

import bo.cdr.app.common.Pojo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Pojo
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePojo{
   private String message;
   private Boolean value;
}