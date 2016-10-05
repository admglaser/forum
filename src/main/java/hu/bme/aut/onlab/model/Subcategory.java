package hu.bme.aut.onlab.model;

import javax.persistence.*;
import java.util.Collection;

/**
 * Created by N. Vilagos.
 */
@Entity
@Table(name = "subcategory")
public class Subcategory {
    private int id;
    private String title;
    private String desc;
    private Collection<Permission> permissionsById;
    private Category categoryByCategoryId;
    private Collection<SubcategorySubscription> subcategorySubscriptionsById;
    private Collection<Topic> topicsById;

    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "title", nullable = false, insertable = true, updatable = true, length = 255)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    @Column(name = "desc", nullable = true, insertable = true, updatable = true, length = 255)
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subcategory that = (Subcategory) o;

        if (id != that.id) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
        if (permissionsById != null ? !permissionsById.equals(that.permissionsById) : that.permissionsById != null)
            return false;
        if (categoryByCategoryId != null ? !categoryByCategoryId.equals(that.categoryByCategoryId) : that.categoryByCategoryId != null)
            return false;
        if (subcategorySubscriptionsById != null ? !subcategorySubscriptionsById.equals(that.subcategorySubscriptionsById) : that.subcategorySubscriptionsById != null)
            return false;
        return !(topicsById != null ? !topicsById.equals(that.topicsById) : that.topicsById != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        result = 31 * result + (permissionsById != null ? permissionsById.hashCode() : 0);
        result = 31 * result + (categoryByCategoryId != null ? categoryByCategoryId.hashCode() : 0);
        result = 31 * result + (subcategorySubscriptionsById != null ? subcategorySubscriptionsById.hashCode() : 0);
        result = 31 * result + (topicsById != null ? topicsById.hashCode() : 0);
        return result;
    }

    @OneToMany(mappedBy = "subcategoryBySubcategoryId")
    public Collection<Permission> getPermissionsById() {
        return permissionsById;
    }

    public void setPermissionsById(Collection<Permission> permissionsById) {
        this.permissionsById = permissionsById;
    }

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    public Category getCategoryByCategoryId() {
        return categoryByCategoryId;
    }

    public void setCategoryByCategoryId(Category categoryByCategoryId) {
        this.categoryByCategoryId = categoryByCategoryId;
    }

    @OneToMany(mappedBy = "subcategoryBySubcategoryId")
    public Collection<SubcategorySubscription> getSubcategorySubscriptionsById() {
        return subcategorySubscriptionsById;
    }

    public void setSubcategorySubscriptionsById(Collection<SubcategorySubscription> subcategorySubscriptionsById) {
        this.subcategorySubscriptionsById = subcategorySubscriptionsById;
    }

    @OneToMany(mappedBy = "subcategoryBySubcategoryId")
    public Collection<Topic> getTopicsById() {
        return topicsById;
    }

    public void setTopicsById(Collection<Topic> topicsById) {
        this.topicsById = topicsById;
    }
}
