package bo.cdr.app.appcatalog.presentation.response.pojo;

import bo.cdr.app.appbase.presentation.response.BasePojo;
import bo.cdr.app.common.Pojo;
import bo.cdr.app.common.enums.ParameterFolderEnum;
import bo.cdr.app.common.enums.ParameterValueTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** folder y valueType se exponen con su etiqueta en español (como HolidayPojo en la referencia). */
@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParameterPojo extends BasePojo {
    private Integer id;
    private String folder;
    private String code;
    private String name;
    private String value;
    private String valueType;
    private String description;
    private Boolean editable;

    public ParameterPojo(Integer id, ParameterFolderEnum folder, String code, String name, String value,
                         ParameterValueTypeEnum valueType, String description, Boolean editable, Boolean active,
                         String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.folder = folder != null ? folder.getName() : null;
        this.code = code;
        this.name = name;
        this.value = value;
        this.valueType = valueType != null ? valueType.name() : null;
        this.description = description;
        this.editable = editable;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
