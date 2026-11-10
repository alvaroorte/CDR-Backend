package bo.cdr.app.appbase.domain.service;

import bo.cdr.app.appbase.data.repository.GenericRepository;
import bo.cdr.app.appbase.domain.entity.Base;
import bo.cdr.app.appbase.presentation.response.ResponsePojo;
import bo.cdr.app.common.SpanishEntityNameProvider;
import bo.cdr.app.exception.EntityNotFoundException;
import bo.cdr.app.exception.ErrorListException;
import bo.cdr.app.exception.HasAssociatedEntityException;
import bo.cdr.app.exception.response.EntityErrorModel;
import bo.cdr.app.util.constants.Message;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CRUDServiceImpl<T, ID> implements CRUDService<T, ID> {

    protected abstract GenericRepository<T, ID> getRepository();

    @Override
    public T create(T t) {
        return getRepository().save(t);
    }

    @Override
    public T update(ID id, T t) throws Exception {
        setEntityId(t, id);
        T existing = getRepository().findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        SpanishEntityNameProvider.getSpanishName(getEntityName()), (Integer) id));
        if (t instanceof Base base && existing instanceof Base existingBase) {
            base.setCreatedAt(existingBase.getCreatedAt());
        }
        return getRepository().save(t);
    }

    protected void setEntityId(T entity, ID id) {
        try {
            Method setter = entity.getClass().getMethod("setId", id.getClass());
            setter.invoke(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID on " + entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public List<T> getAll() {
        return getRepository().findAll();
    }

    @Override
    public T getById(ID id) {
        return getRepository().findById(id).orElseThrow(
                () -> new EntityNotFoundException(SpanishEntityNameProvider.getSpanishName(getEntityName()),
                        (Integer) id));
    }

    @Override
    public List<T> getByIds(List<ID> ids) {
        List<EntityErrorModel> errors = new ArrayList<>();
        List<T> entities = new ArrayList<>();
        for (ID id : ids) {
            try {
                entities.add(this.getById(id));
            } catch (EntityNotFoundException e) {
                errors.add(new EntityErrorModel(SpanishEntityNameProvider.getSpanishName(getEntityName()),
                        e.getMessage(), "EntityNotFoundException"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ErrorListException(errors);
        }
        return entities;
    }

    @Override
    public ResponsePojo existsById(ID id) {
        this.getById(id);
        return new ResponsePojo(getEntityName() + Message.ENTITY_FOUND + id, true);
    }

    @Override
    public void delete(ID id) {
        getRepository().findById(id).orElseThrow(
                () -> new EntityNotFoundException(SpanishEntityNameProvider.getSpanishName(getEntityName()),
                        (Integer) id));
        getRepository().deleteById(id);
    }

    @Override
    public void softDelete(ID id) {
        T entity = getById(id);
        if (!(entity instanceof Base base)) {
            throw new UnsupportedOperationException(getEntityName() + " no admite borrado lógico");
        }
        base.setActive(false);
        getRepository().save(entity);
    }

    @Override
    public void checkIfElementsExist(Map<String, Boolean> dependencyMap) {
        for (Map.Entry<String, Boolean> entry : dependencyMap.entrySet()) {
            if (entry.getValue()) {
                throw new HasAssociatedEntityException(
                        SpanishEntityNameProvider.getSpanishName(getEntityName()), entry.getKey());
            }
        }
    }

    private String getEntityName() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        return ((Class<?>) parameterizedType.getActualTypeArguments()[0]).getSimpleName();
    }
}
