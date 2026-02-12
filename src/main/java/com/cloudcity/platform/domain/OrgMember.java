package com.cloudcity.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "org_members")
public class OrgMember extends CreatedEntity {
    @EmbeddedId
    private OrgMemberId id;

    @MapsId("orgId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgRole role;

    protected OrgMember() {
    }

    public OrgMember(Org org, User user, OrgRole role) {
        this.org = org;
        this.user = user;
        this.role = role;
        this.id = new OrgMemberId(org.getId(), user.getId());
    }

    public OrgMemberId getId() {
        return id;
    }

    public Org getOrg() {
        return org;
    }

    public User getUser() {
        return user;
    }

    public OrgRole getRole() {
        return role;
    }

    public void setRole(OrgRole role) {
        this.role = role;
    }
}
