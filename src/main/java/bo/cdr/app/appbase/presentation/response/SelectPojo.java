package bo.cdr.app.appbase.presentation.response;

import bo.cdr.app.common.Pojo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Opción para combos/selects (equivalente a GenericSelectProjection de la referencia). */
@Pojo
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectPojo {
    private Integer id;
    private String name;
}
