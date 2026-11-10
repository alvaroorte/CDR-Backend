package bo.cdr.app.appbase.domain.service;

import bo.cdr.app.appbase.presentation.response.ResponsePojo;

import java.util.List;
import java.util.Map;

public interface CRUDService<T, ID> {

    T create(T t);

    T update(ID id, T t) throws Exception;

    List<T> getAll();

    T getById(ID id);

    List<T> getByIds(List<ID> ids);

    void delete(ID id);

    /** Borrado lógico (active=false). Usar en catálogos con historial; lo histórico no se borra. */
    void softDelete(ID id);

    ResponsePojo existsById(ID id);

    void checkIfElementsExist(Map<String, Boolean> dependencyMap);
}