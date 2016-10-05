package hu.bme.aut.onlab.model;

import javax.persistence.*;
import java.util.Set;

/**
 * Created by N. Vilagos.
 */
@Entity
@Table(name = "permission")
public class Permission {
    private int id;
    private byte readAllowed;
    private byte replyAllowed;
    private byte startAllowed;
    private Subcategory subcategoryBySubcategoryId;
    private Set<PermissionSet> permissionSets;

    @Id
    @Column(name = "id", nullable = false, insertable = true, updatable = true)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "read_allowed", nullable = false, insertable = true, updatable = true)
    public byte getReadAllowed() {
        return readAllowed;
    }

    public void setReadAllowed(byte readAllowed) {
        this.readAllowed = readAllowed;
    }

    @Basic
    @Column(name = "reply_allowed", nullable = false, insertable = true, updatable = true)
    public byte getReplyAllowed() {
        return replyAllowed;
    }

    public void setReplyAllowed(byte replyAllowed) {
        this.replyAllowed = replyAllowed;
    }

    @Basic
    @Column(name = "start_allowed", nullable = false, insertable = true, updatable = true)
    public byte getStartAllowed() {
        return startAllowed;
    }

    public void setStartAllowed(byte startAllowed) {
        this.startAllowed = startAllowed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Permission that = (Permission) o;

        if (id != that.id) return false;
        if (readAllowed != that.readAllowed) return false;
        if (replyAllowed != that.replyAllowed) return false;
        if (startAllowed != that.startAllowed) return false;
        if (subcategoryBySubcategoryId != null ? !subcategoryBySubcategoryId.equals(that.subcategoryBySubcategoryId) : that.subcategoryBySubcategoryId != null)
            return false;
        return !(permissionSets != null ? !permissionSets.equals(that.permissionSets) : that.permissionSets != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (int) readAllowed;
        result = 31 * result + (int) replyAllowed;
        result = 31 * result + (int) startAllowed;
        result = 31 * result + (subcategoryBySubcategoryId != null ? subcategoryBySubcategoryId.hashCode() : 0);
        result = 31 * result + (permissionSets != null ? permissionSets.hashCode() : 0);
        return result;
    }

    @ManyToOne
    @JoinColumn(name = "subcategory_id", referencedColumnName = "id", nullable = false)
    public Subcategory getSubcategoryBySubcategoryId() {
        return subcategoryBySubcategoryId;
    }

    public void setSubcategoryBySubcategoryId(Subcategory subcategoryBySubcategoryId) {
        this.subcategoryBySubcategoryId = subcategoryBySubcategoryId;
    }

    @ManyToMany
    @JoinTable(name = "permission_to_permission_set", catalog = "forum", schema = "", joinColumns = @JoinColumn(name = "permissionid", referencedColumnName = "id", nullable = false), inverseJoinColumns = @JoinColumn(name = "permission_setid", referencedColumnName = "id", nullable = false))
    public Set<PermissionSet> getPermissionSets() {
        return permissionSets;
    }

    public void setPermissionSets(Set<PermissionSet> permissionSets) {
        this.permissionSets = permissionSets;
    }
}
