package hu.bme.aut.onlab.beans;

import hu.bme.aut.onlab.model.Category;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by N. Vilagos.
 */
@LocalBean
@Stateless
public class CategoryBean extends BaseBean<Category> {

    @PersistenceContext
    EntityManager entityManager;

    public CategoryBean() {
        super(Category.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

}
