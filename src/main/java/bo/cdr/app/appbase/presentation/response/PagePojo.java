package bo.cdr.app.appbase.presentation.response;

import bo.cdr.app.common.Pojo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Pojo
@Getter
@Setter
@NoArgsConstructor
public class PagePojo<T>{
   private List<T> content;
   private boolean last;
   private int pageNumber;
   private int pageSize;
   private int totalPages;
   private long totalElements;

   public PagePojo(List<T> content, boolean last, int pageNumber, int pageSize, int totalPages, long totalElements) {
    this.content = content;
    this.last = last;
    this.pageNumber = pageNumber;
    this.pageSize = pageSize;
    this.totalPages = totalPages;
    this.totalElements = totalElements;
   }

}