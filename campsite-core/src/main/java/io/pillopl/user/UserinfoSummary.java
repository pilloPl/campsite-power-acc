package io.pillopl.user;

import io.pillopl.group.Group;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Row of the materialized view USERINFO_SUMMARY: a self-service console user with its role flags. Read only.
 */
@Entity
@Table(name = "USERINFO_SUMMARY")
@Immutable
@Getter
public class UserinfoSummary {

    @Id
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "GROUP_ID")
    private Group group;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "NAME")
    private String name;

    @Column(name = "EMAIL")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private UserinfoStatus status;

    @Column(name = "AFFILIATE_OWNER")
    private boolean affiliateOwner;

    @Column(name = "SEASONAL_RESIDENT")
    private boolean seasonalResident;

    @Override
    public String toString() {
        return "UserinfoSummary{" + username + ", owner=" + affiliateOwner + ", resident=" + seasonalResident + "}";
    }
}
