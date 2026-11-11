package bo.cdr.app.appcatalog.presentation.response.pojo;

import bo.cdr.app.appbase.presentation.response.BasePojo;
import bo.cdr.app.common.Pojo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Pojo
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryPojo extends BasePojo {
    private Integer id;
    private String name;
    private String description;
    private String imageBase64;
    private Integer ranking;

    public CategoryPojo(Integer id, String name, String description, String imageBase64, Integer ranking, Boolean active,
                        String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageBase64 = imageBase64;
        this.ranking = ranking;
        this.setActive(active);
        this.setCreatedBy(createdBy);
        this.setUpdatedBy(updatedBy);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }
}
